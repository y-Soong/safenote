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

        log.info("일용직 로그인 성공 — userCd={}, clientType={}", userResult.userCd(), param.clientType());

        return DailyLoginResponse.from(userResult, tbUser, refreshToken, token);
    }
}
