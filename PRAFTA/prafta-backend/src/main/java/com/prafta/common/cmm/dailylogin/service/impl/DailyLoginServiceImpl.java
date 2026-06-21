package com.prafta.common.cmm.dailylogin.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.prafta.common.cmm.dailylogin.application.command.DailyActiveTokenCommand;
import com.prafta.common.cmm.dailylogin.application.command.DailyUserPwdFailCommand;
import com.prafta.common.cmm.dailylogin.application.command.DailyUserPwdUnlockCommand;
import com.prafta.common.cmm.dailylogin.application.param.DailyLoginParam;
import com.prafta.common.cmm.dailylogin.application.query.DailyLoginQuery;
import com.prafta.common.cmm.dailylogin.dto.response.DailyLoginResponse;
import com.prafta.common.cmm.dailylogin.mapper.DailyLoginMapper;
import com.prafta.common.cmm.dailylogin.result.DailyUserResult;
import com.prafta.common.cmm.dailylogin.service.DailyLoginService;
import com.prafta.common.cmm.login.application.command.DeviceLoginCommand;
import com.prafta.common.cmm.login.application.command.DeviceOccupancyAnomalyCommand;
import com.prafta.common.cmm.login.mapper.LoginMapper;
import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.error.dailylogin.DailyLoginErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.security.crypto.HmacSigner;
import com.prafta.common.util.PasswordHasher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PRAFTA-app-027-2 — 일용직 직접 로그인 서비스 구현.
 *
 * <p>정책서: {@code .claude/context/policies/common/03-account-auth.md} §3.2(로그인)/§3.5(차단),
 * §3.4(토큰 — 리프레시 48시간), §11.1(PII).
 *
 * <p>정규 LoginServiceImpl 의 잠금 로직을 미러한다. 차단 사유(비활성/만료/오ID/오비번/탈퇴)는
 * 계정 존재 노출 방지를 위해 통합 메시지(DAILYLOGIN_400_001)로만 응답한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyLoginServiceImpl implements DailyLoginService {

    private final DailyLoginMapper dailyLoginMapper;
    private final PasswordHasher passwordHasher;
    private final HmacSigner hmacSigner;
    private final JwtUtil jwtUtil;
    // PRAFTA-app-027-2'(통합형) — TB_USER 행 로드/정규 토큰 발급 재사용(읽기 전용, 정규 쓰기 미오염).
    private final LoginMapper loginMapper;

    // 정규 로그인과 동일한 잠금 정책 프로퍼티 재사용(login.lock.*).
    @Value("${login.lock.duration-minutes}")
    private int lockDurationMinutes;

    @Value("${login.lock.max-fail-count}")
    private int maxFailCount;

    /** ACCOUNT_STATUS '05' = 비활성화(자정 만료 배치 027-1 이 설정). */
    private static final String ACCOUNT_STATUS_INACTIVE = "05";

    private static final DateTimeFormatter LOCK_DTIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /*
     * 잠금 카운트가 차단 예외(RuntimeException)와 함께 롤백되지 않도록 메서드 레벨 @Transactional 을
     * 부여하지 않는다(정규 LoginServiceImpl 미러). 각 UPDATE 는 auto-commit 되어 실패 카운트가 영속된다.
     */
    @Override
    public DailyLoginResponse login(DailyLoginParam param) {

        // 1) USE_YN='Y' 가드 내장 조회 — 비활성/만료 계정은 행 미반환 → 통합 차단 메시지.
        //    cmpnyCd 미전송 + 동일 USER_ID 다회사 활성 공존 시 다건 가능 → 단건이 아니면 통합 차단(500 노출 방지).
        List<DailyUserResult> userResults = dailyLoginMapper.selectDailyUserForLogin(DailyLoginQuery.from(param));
        if (userResults == null || userResults.size() != 1) {
            throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_400_001);
        }
        DailyUserResult userResult = userResults.get(0);

        // 2) 비밀번호 인증 실패 잠금 만료일시 체크(정규 미러).
        if ("Y".equals(userResult.pwdLockYn())) {
            String unlockDtimeStr = userResult.pwdLockExpireDtime();
            if (unlockDtimeStr != null && !unlockDtimeStr.isBlank()) {
                LocalDateTime unlockDtime = LocalDateTime.parse(unlockDtimeStr, LOCK_DTIME);
                if (LocalDateTime.now().isBefore(unlockDtime)) {
                    throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_400_002);
                }
            }
        }

        // 3) 잠금 해제 시각이 지났으면 상태 정리(만료된 잠금에만 적용 — 미잠금 구간 실패 카운트는 누적 보존).
        dailyLoginMapper.updateDailyUserPwdUnlock(DailyUserPwdUnlockCommand.from(userResult));

        // 4) 비밀번호 검증. 실패 시 실패 누적(임계 도달 시 잠금) 후 통합 차단.
        if (!passwordHasher.matches(param.userPw(), userResult.userPw())) {
            dailyLoginMapper.updateDailyUserPwdFail(
                    DailyUserPwdFailCommand.from(userResult, lockDurationMinutes, maxFailCount));
            throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_400_001);
        }

        // 5) 탈퇴/비활성 차단(통합 메시지 — 계정 존재 비노출).
        if (userResult.withdrawalDate() != null && !userResult.withdrawalDate().isBlank()) {
            throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_400_001);
        }
        if (ACCOUNT_STATUS_INACTIVE.equals(userResult.accountStatus())) {
            throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_400_001);
        }

        // 5-1) 비밀번호 인증 성공 — 실패 카운트/잠금 무조건 초기화.
        //      (진입 시 unlock 은 만료 시에만 리셋하므로 성공 경로에서 명시적으로 0 복귀시킨다.)
        dailyLoginMapper.updateDailyUserPwdReset(DailyUserPwdUnlockCommand.from(userResult));

        // 5-2) PRAFTA-app-027-2' 통합형 — 같은 USER_CD 의 TB_USER 행 로드(인가/표시 클레임의 단일 출처).
        //      인증=TB_DAILY_USER, 인가/표시 클레임=TB_USER 로 출처를 분리한다.
        //      행 미존재(기존 분리형 발급계정 = TB_USER 행 없음) 또는 비활성('05'/USE_YN!='Y')이면 통합 차단.
        UserResult tbUser = loginMapper.selectUserByUserCd(userResult.cmpnyCd(), userResult.userCd());
        if (tbUser == null) {
            // 통합형 가입 전 발급된 계정 — TB_USER 행 부재. 계정 존재 비노출 통합 메시지.
            throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_400_001);
        }
        if (!"Y".equals(tbUser.useYn()) || ACCOUNT_STATUS_INACTIVE.equals(tbUser.accountStatus())) {
            // 이중 안전망: TB_DAILY_USER 는 'Y' 인데 TB_USER 가 비활성인 정합 깨짐도 차단.
            throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_400_001);
        }

        // 6) 리프레시 토큰 발급(정책 §3.4: 48시간=2일) — TB_AUTH_TOKEN 재사용.
        String tokenId = UUID.randomUUID().toString().replace("-", "");
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String refreshTokenHash = hmacSigner.hmacSha256Base64Url(refreshToken);

        DailyActiveTokenCommand tokenCommand = DailyActiveTokenCommand.from(
                userResult, tokenId, param.clientType(), refreshTokenHash, "2");
        dailyLoginMapper.revokeDailyActiveToken(tokenCommand);
        dailyLoginMapper.insertDailyAuthToken(tokenCommand);

        // 7) PRAFTA-app-027-2' 통합형 — 정규와 동일 클레임 토큰 발급(TB_USER 행 기반).
        //    gv_authCd='99999'/gv_authLevel/gv_siteNo/gv_nodeCd=null/gv_employmentType='DAILY' 등.
        String token = jwtUtil.generateToken(tbUser);

        // 8) 마지막 로그인 일시 갱신.
        dailyLoginMapper.updateDailyUserLastLoginDtime(userResult.cmpnyCd(), userResult.userCd());

        // 9) (계정당 활성 디바이스 1대) 디바이스 행 생성/이력 적재 + 다른 기기 비활성화.
        //    정규 LoginServiceImpl.recordDeviceLogin 미러 — best-effort(실패해도 로그인 영향 없음).
        recordDeviceLogin(userResult, param);

        log.info("일용직 로그인 성공 — userCd={}, clientType={}", userResult.userCd(), param.clientType());

        return DailyLoginResponse.from(userResult, tbUser, refreshToken, token);
    }

    /**
     * 일용직 로그인 성공 직후 디바이스/로그인 이력 적재 + 단일 활성기기 정리(예외 격리).
     *
     * <p>정규 {@link com.prafta.common.cmm.login.service.impl.LoginServiceImpl} 의 동명 훅을 미러한다.
     * deviceId 가 비어 있으면(웹/구버전 앱) 아무 것도 하지 않는다. 적재 중 어떤 예외가 나도
     * 로그인 흐름에는 영향을 주지 않으며 log.error 로만 남긴다.
     *
     * <p>일용직 로그인은 IP 를 수집하지 않으므로 LOGIN_IP 는 null 로 적재한다(컬럼 nullable).
     * 디바이스 행 생성(upsertUserDevice)은 이후 푸시 토큰 등록(device01 UPDATE)이 성립하기 위한 선행 행이기도 하다.
     */
    private void recordDeviceLogin(DailyUserResult userResult, DailyLoginParam param) {
        try {
            if (param == null || param.deviceId() == null || param.deviceId().isBlank()) {
                return; // 디바이스ID 미전송(웹/구버전 앱) → 적재 대상 아님.
            }
            // prafta-com-015 015-1: 점유 재할당 이상탐지(upsert 직전, 정규 LoginServiceImpl 미러).
            //   직전 점유자가 "다른 계정" 이면 감사행 적재(차단 없음). 동일 계정 재로그인은 미적재.
            detectOccupancyAnomaly(userResult.cmpnyCd(), param.deviceId(), userResult.userCd(),
                    param.clientType(), param.ipAddr());
            DeviceLoginCommand command = new DeviceLoginCommand(
                    userResult.cmpnyCd()
                    , param.deviceId()
                    , userResult.userCd()
                    , param.deviceType()
                    , param.deviceModel()
                    , param.osVersion()
                    , param.appVersion()
                    , param.clientType()
                    , param.ipAddr()  // prafta-com-015 4-1: 일용직 LOGIN_IP 수집(컨트롤러 추출).
                    , userResult.userCd());
            loginMapper.upsertUserDevice(command);
            loginMapper.insertDeviceLoginHist(command);
            // (계정당 활성 디바이스 1대) 같은 사용자의 다른 기기를 비활성화 → 마지막 로그인 기기만 푸시 수신.
            int deactivated = loginMapper.deactivateOtherUserDevices(userResult.userCd(), param.deviceId());
            // PII(기기ID/IP) 평문 로그 금지 — 식별 키만 남긴다.
            log.info("일용직 디바이스 로그인 이력 적재 완료 — userCd={}, clientType={}, 비활성화기기={}건"
                    , userResult.userCd(), param.clientType(), deactivated);
        } catch (Exception e) {
            log.error("일용직 디바이스 로그인 이력 적재 실패(로그인 영향 없음) — userCd={}", userResult.userCd(), e);
        }
    }

    /**
     * prafta-com-015 015-1 — 디바이스 점유 재할당 이상탐지(감사만, 차단 없음 — 정규 LoginServiceImpl 미러).
     *
     * <p>upsert 직전 deviceUuid 의 현재 점유자가 "다른 계정" 이면 이상행 적재 + log.warn.
     * 신규 기기 또는 동일 계정 재로그인(iOS IDFV 변경/재설치 포함)은 미적재.
     * recordDeviceLogin 의 try-catch 안에서 호출되어 실패는 격리된다.
     */
    private void detectOccupancyAnomaly(String cmpnyCd, String deviceUuid, String newUserCd,
            String clientType, String loginIp) {
        // 이상탐지/적재 실패(예: 마이그 미적용 Unknown table)가 디바이스 upsert/이력/단일기기 정리를
        //   막지 않도록 자체 격리(정규 LoginServiceImpl 미러). 탐지는 부가 기능 — 실패해도 무시.
        try {
            String prevOwner = loginMapper.selectDeviceOwner(deviceUuid);
            if (prevOwner == null || prevOwner.equals(newUserCd)) {
                return; // 신규 기기 또는 동일 계정 재로그인 → 정상(이상 아님).
            }
            loginMapper.insertOccupancyAnomaly(new DeviceOccupancyAnomalyCommand(
                    cmpnyCd, deviceUuid, prevOwner, newUserCd, clientType, loginIp));
            log.warn("일용직 디바이스 점유 재할당 감지 — deviceUuid={}, prevUserCd={}, newUserCd={}, clientType={}",
                    maskHead(deviceUuid), maskHead(prevOwner), maskHead(newUserCd), clientType);
        } catch (Exception e) {
            log.warn("일용직 디바이스 점유 이상탐지 실패(무시, 디바이스 적재 계속) — deviceUuid={}: {}",
                    maskHead(deviceUuid), e.getMessage());
        }
    }

    /** 식별자 마스킹: 앞 8자 + ***(짧으면 길이만큼). 평문 로그 금지(S2 패턴 미러). */
    private String maskHead(String value) {
        if (value == null || value.isBlank()) {
            return "(none)";
        }
        int head = Math.min(8, value.length());
        return value.substring(0, head) + "***";
    }
}
