package com.prafta.platform.location.service.impl;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.sms.AuthCodeSmsDispatcher;
import com.prafta.common.cmm.sms.policy.SmsRateLimitGuard;
import com.prafta.common.cmm.sms.policy.SmsSendContext;
import com.prafta.common.error.platform.PlatformErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.GpsCoordCrypto;
import com.prafta.common.security.normalize.Normalizers;
import com.prafta.platform.location.application.command.LocationAccessLogCommand;
import com.prafta.platform.location.application.command.PlatformSmsAuthInsertCommand;
import com.prafta.platform.location.application.command.PlatformSmsVerifyCommand;
import com.prafta.platform.location.application.param.GpsListParam;
import com.prafta.platform.location.application.param.LocationSiteListParam;
import com.prafta.platform.location.application.param.PlatformOperatorParam;
import com.prafta.platform.location.application.param.SmsVerifyParam;
import com.prafta.platform.location.application.query.GpsListQuery;
import com.prafta.platform.location.application.query.OperatorMblQuery;
import com.prafta.platform.location.application.query.PlatformSmsCertQuery;
import com.prafta.platform.location.application.query.PlatformSmsVerifiedQuery;
import com.prafta.platform.location.application.result.GpsInfoResult;
import com.prafta.platform.location.application.result.GpsInfoRow;
import com.prafta.platform.location.application.result.LocationSiteResult;
import com.prafta.platform.location.application.result.OperatorMblResult;
import com.prafta.platform.location.application.result.PlatformSmsVerifiedResult;
import com.prafta.platform.location.dto.response.GpsListResponse;
import com.prafta.platform.location.dto.response.LocationSiteListResponse;
import com.prafta.platform.location.dto.response.SmsStatusResponse;
import com.prafta.platform.location.mapper.PlatformLocationMapper;
import com.prafta.platform.location.service.PlatformLocationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformLocationServiceImpl implements PlatformLocationService {

    private final PlatformLocationMapper platformLocationMapper;

    /** GPS좌표-암호화-전환-04: 좌표 fallback 복호화(ENC 우선, NULL 이면 구 평문). */
    private final GpsCoordCrypto gpsCoordCrypto;

    /** SMS-PPURIO-06: 운영자 등록 휴대폰(MBL_NO_ENC) 복호화 — 발송 수신번호 확보 전용. */
    private final AesGcmCrypto aesGcmCrypto;

    /** SMS-PPURIO-06: 인증번호 실발송 디스패처(게이트 OFF 면 SKIPPED 기록 후 조용히 통과). */
    private final AuthCodeSmsDispatcher authCodeSmsDispatcher;

    /** SMS2-B4: 발송 다층 상한 가드(정책행 잠금으로 TOCTOU 봉인 + 인증코드 INSERT 를 함께 수행). */
    private final SmsRateLimitGuard smsRateLimitGuard;

    /** 위치정보 응답 최대 건수(초과 시 절단 + truncated=true). */
    private static final int GPS_LIST_MAX = 1000;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 위치열람 게이트 SMS 인증코드 발급 + 실발송.
     *
     * <p>★SMS-PPURIO-06: 외부 HTTP(발송)를 트랜잭션 안에 두지 않기 위해 {@code @Transactional} 을 제거했다.
     *    본 메서드의 쓰기는 {@code insertPlatformSmsAuth} 단건뿐이라(앞의 두 호출은 조회) 원자성 손실이 없다.
     *    트랜잭션을 유지하면 ① DB 커넥션을 네트워크 대기 동안 점유하고
     *    ② 발송 실패로 롤백될 때 {@code SEND_STATUS='FAILED'} 기록까지 사라진다(요청서 §7-3).
     *    {@code verifySmsAuth} / {@code selectGpsList} 의 {@code @Transactional} 은 발송과 무관하므로 그대로 유지한다.
     */
    @Override
    public void sendSmsAuth(PlatformOperatorParam param) {

        // 1) 발송 대상은 토큰의 운영자 본인 등록 휴대폰(TB_USER) — 클라 입력 휴대폰 불신.
        OperatorMblResult operatorMbl = selectOperatorMblOrThrow(param.gvCmpnyCd(), param.gvUserCd());

        // 1-1) 발송 레이트리밋은 SMS2-B4 에서 SmsRateLimitGuard 로 이관했다(아래 3) 참조).
        //      인라인 selectRecentPlatformSmsSendCnt 호출은 제거됐다(쿼리는 1릴리즈 유예 후 삭제).

        // 2) 6자리 인증코드 생성(baseinfo insertSmsAuthNo 미러 — SecureRandom).
        //    인증번호 평문은 어떤 로그에도 남기지 않는다(prafta-app-032 보강 규약 승계).
        String certNo = Integer.toString(100000 + secureRandom.nextInt(900000));

        // 2-1) ★SMS-PPURIO-06: 이 흐름에는 휴대폰 평문이 없다(TB_USER 에서 ENC/HMAC 만 읽어옴).
        //      실제 발송을 하려면 수신번호 평문이 필요하므로 여기서만 복호화한다.
        //      평문은 지역변수로만 존재하며 로그·응답·예외 메시지에 절대 노출하지 않는다(§7-6, 정책 §11.1 최소 처리).
        //      저장 시점 포맷이 불확실하므로 복호화 결과를 한 번 더 정규화한다.
        //
        // ★SMS2-D6(qa D-5): 복호화를 게이트 ON 일 때만 수행한다.
        //   게이트 OFF 는 "SMS 연동 이전과 완전히 동일한 동작" 이어야 하는데, 발송을 하지도 않으면서
        //   발송용 전처리(복호화) 실패로 요청이 400 으로 죽는 새 경로가 생겨 있었다.
        //   키 로테이션 이력이나 레거시 포맷 행이 하나라도 있으면 위치열람 게이트가 통째로 막힌다
        //   (LBS 법적 확인자료 경로라 영향이 크다).
        //   게이트 OFF 면 phoneNorm 은 null 로 남고 dispatch() 가 SKIPPED 만 기록한다
        //   (Normalizers.last4(null) 은 null 을 반환하므로 NPE 없음 - 실측 확인).
        String phoneNorm = null;
        if (authCodeSmsDispatcher.isSendEnabled()) {
            try {
                phoneNorm = Normalizers.normalizePhone(aesGcmCrypto.decrypt(operatorMbl.mblNoEnc()));
            } catch (RuntimeException e) {
                // 복호화 실패는 "등록 휴대폰 사용 불가" 와 같은 상황 → 기존 코드 재사용(원인은 서버 로그만).
                log.error("플랫폼 위치열람 SMS 발송 - 운영자 휴대폰 복호화 실패, 운영자={}, 예외={}",
                        param.gvUserCd(), e.getClass().getSimpleName());
                throw new ApiException(PlatformErrorCode.PLATFORM_400_012);
            }
            if (phoneNorm == null || phoneNorm.isEmpty()) {
                throw new ApiException(PlatformErrorCode.PLATFORM_400_012);
            }
        }

        // 3) 목적 PLATFORM_LOCATION 으로 INSERT(코드 유효 1분). ENC 값은 TB_USER 것을 그대로 복사(평문 미접촉).
        //    refKey 는 INSERT 전에 생성해 함께 저장한다(발송 결과 UPDATE 의 조인키).
        //
        //    SMS2-B4: 상한 판정 + INSERT 를 가드에 위임한다(정책행 잠금으로 TOCTOU 봉인).
        //    ★기존 1분 창의 사용자 노출 코드(PLATFORM_400_013)를 컨텍스트에 실어 그대로 유지한다 — 무회귀.
        //      가드의 기본 코드(SMS_400_001)가 나가면 프론트 문구/분기가 달라진다.
        //    ★★[3차 / qa Q-2] windowAlwaysEnforced 플래그는 제거됐다 — 번호 창은 이제 전 진입점에서
        //      게이트와 무관하게 항상 강제된다. 이 1분 창이 SMS 연동 이전부터 존재하던 보안 통제라는
        //      사실(플랫폼 위치열람 보안 리뷰 V-2)은 그대로이며, 진입점 A·B 도 같은 규칙으로 통일됐다.
        //    ★로그인 흐름이므로 사용자 축(userCd)을 채운다.
        String refKey = authCodeSmsDispatcher.newRefKey();
        smsRateLimitGuard.guardAndInsert(
                SmsSendContext.ofWithWindowErrorCode(
                        "PLATFORM_LOCATION"
                        , operatorMbl.mblNoHmac()
                        , param.ipHash()
                        , param.gvUserCd()
                        , PlatformErrorCode.PLATFORM_400_013),
                () -> platformLocationMapper.insertPlatformSmsAuth(new PlatformSmsAuthInsertCommand(
                        operatorMbl.mblNoEnc()
                        , operatorMbl.mblNoHmac()
                        , certNo
                        , param.gvUserCd()
                        , refKey
                        , param.ipHash()
                )));

        // 4) 실발송 + 결과 기록. 게이트 OFF 면 SKIPPED 기록 후 조용히 통과(기존과 동일한 성공 응답).
        //    ★validMinutes=1 은 PlatformLocationMapper.xml 의 EXPIRED_AT(INTERVAL 1 MINUTE)와 한 쌍이다.
        authCodeSmsDispatcher.dispatch(refKey, phoneNorm, certNo, 1);

        log.info("플랫폼 위치열람 SMS 인증코드 발급 - 운영자={}", param.gvUserCd());
    }

    // noRollbackFor=ApiException: 인증번호 불일치(PLATFORM_400_010) 응답과 함께 FAIL_CNT 증가가
    // 롤백되면 브루트포스 카운터(V-2)가 무력화되므로, ApiException 은 커밋 후 전파한다.
    // (본 메서드에서 ApiException 이전의 쓰기는 FAIL_CNT 증가뿐 — 부분 커밋 부작용 없음)
    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = ApiException.class)
    public void verifySmsAuth(SmsVerifyParam param) {

        // 1) 인증번호 형식 검증(6자리 숫자).
        String certNo = param.certNo() == null ? "" : param.certNo().trim();
        if (certNo.length() != 6 || !certNo.chars().allMatch(Character::isDigit)) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_010);
        }

        // 2) 운영자 본인 HMAC 해석(서버측 — 검증 대상 위조 불가).
        OperatorMblResult operatorMbl = selectOperatorMblOrThrow(param.gvCmpnyCd(), param.gvUserCd());

        // 3) 목적 PLATFORM_LOCATION + 미검증 + 미만료(1분) + 실패 5회 미만 매칭.
        String smsId = platformLocationMapper.selectPlatformCertSmsId(
                new PlatformSmsCertQuery(operatorMbl.mblNoHmac(), certNo));
        if (smsId == null || smsId.isEmpty()) {
            // 불일치 시 최신 미검증 코드의 FAIL_CNT +1 — 5회 도달 시 코드 무효(재발송 필요, V-2).
            // 응답은 만료/불일치/초과 미구분(PLATFORM_400_010 재사용 — 열거 방지).
            platformLocationMapper.increasePlatformSmsFailCnt(operatorMbl.mblNoHmac());
            throw new ApiException(PlatformErrorCode.PLATFORM_400_010);
        }

        // 4) 통과 처리(VERIFIED_YN='Y') — 이후 10분 창 동안 gps-lists 조회 허용(소비하지 않음).
        platformLocationMapper.updatePlatformSmsVerified(new PlatformSmsVerifyCommand(
                smsId
                , operatorMbl.mblNoHmac()
                , certNo
                , param.gvUserCd()
        ));

        log.info("플랫폼 위치열람 SMS 인증 통과 - 운영자={}, smsId={}", param.gvUserCd(), smsId);
    }

    @Override
    public SmsStatusResponse selectSmsStatus(PlatformOperatorParam param) {

        PlatformSmsVerifiedResult verified = platformLocationMapper.selectPlatformSmsVerified(
                new PlatformSmsVerifiedQuery(param.gvCmpnyCd(), param.gvUserCd()));

        if (verified == null) {
            return SmsStatusResponse.builder().verified(false).remainSec(0).build();
        }

        long remainSec = verified.remainSec() == null ? 0 : Math.max(0, verified.remainSec());
        return SmsStatusResponse.builder().verified(true).remainSec(remainSec).build();
    }

    @Override
    public LocationSiteListResponse selectSiteList(LocationSiteListParam param) {

        // 사업장 좌표는 시설 좌표(개인위치정보 아님) — SMS 게이트 미적용, 열람 로그 대상 아님.
        List<LocationSiteResult> siteList = platformLocationMapper.selectLocationSiteList(param.cmpnyCd());

        return LocationSiteListResponse.builder()
                .siteList(siteList)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GpsListResponse selectGpsList(GpsListParam param) {

        // 1) 매 호출 서버측 SMS 인증 판정(프론트 게이트는 보조) — 미인증/만료 시 403.
        PlatformSmsVerifiedResult verified = platformLocationMapper.selectPlatformSmsVerified(
                new PlatformSmsVerifiedQuery(param.gvCmpnyCd(), param.gvUserCd()));
        if (verified == null) {
            throw new ApiException(PlatformErrorCode.PLATFORM_403_003);
        }

        // 2) 위치정보 UNION 조회(근태 GPS + TBM 입실) — LIMIT 1001 로 초과 여부 판정.
        List<GpsInfoRow> gpsRows = platformLocationMapper.selectGpsList(GpsListQuery.from(param));

        boolean truncated = gpsRows.size() > GPS_LIST_MAX;
        if (truncated) {
            gpsRows = gpsRows.subList(0, GPS_LIST_MAX);
        }

        // GPS좌표-암호화-전환-04: 양측(ATTD/TBM) 공통 행 단위 fallback 복호화(ENC 우선, NULL 이면 구 평문)
        // 후 기존 GpsInfoResult 로 재조립 — 응답 계약 불변(Platform_04.vue 무수정). 좌표값 로그 출력 금지.
        List<GpsInfoResult> gpsList = new ArrayList<>();
        for (GpsInfoRow row : gpsRows) {
            gpsList.add(new GpsInfoResult(
                    row.srcType()
                    , row.userCd()
                    , row.measureTime()
                    , row.gpsInfoType()
                    , gpsCoordCrypto.resolveToBigDecimal(row.latEnc(), row.lat())
                    , gpsCoordCrypto.resolveToBigDecimal(row.lonEnc(), row.lon())
                    , row.accuracy()
                    , row.mockedYn()
                    , row.ipAddr()
            ));
        }

        // 3) 열람 로그 INSERT(같은 트랜잭션) — 실패(예외) 시 조회 응답도 실패(확인자료 누락 경로 봉쇄, 공통 §11.3).
        platformLocationMapper.insertLocationAccessLog(new LocationAccessLogCommand(
                param.gvUserCd()
                , param.cmpnyCd()
                , param.siteCd()
                , param.date()
                , verified.smsId()
                , verified.verifiedAt()
                , gpsList.size()
                , param.clientIp()
                , param.remoteAddr()
        ));

        log.info("위치정보 열람 - 운영자={}, 대상회사={}, 사업장={}, 일자={}, 건수={}, 절단={}",
                param.gvUserCd(), param.cmpnyCd(), param.siteCd(), param.date(), gpsList.size(), truncated);

        return GpsListResponse.builder()
                .gpsList(gpsList)
                .resultCnt(gpsList.size())
                .truncated(truncated)
                .build();
    }

    /** 운영자 본인 등록 휴대폰(ENC/HMAC) 조회 — 미등록 시 PLATFORM_400_012. */
    private OperatorMblResult selectOperatorMblOrThrow(String cmpnyCd, String userCd) {

        OperatorMblResult operatorMbl = platformLocationMapper.selectOperatorMbl(
                new OperatorMblQuery(cmpnyCd, userCd));

        if (operatorMbl == null
                || operatorMbl.mblNoHmac() == null || operatorMbl.mblNoHmac().isEmpty()
                || operatorMbl.mblNoEnc() == null || operatorMbl.mblNoEnc().isEmpty()) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_012);
        }
        return operatorMbl;
    }
}
