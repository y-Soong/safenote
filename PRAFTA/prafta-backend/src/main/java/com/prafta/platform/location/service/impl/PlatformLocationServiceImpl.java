package com.prafta.platform.location.service.impl;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.platform.PlatformErrorCode;
import com.prafta.common.exception.ApiException;
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

    /** 위치정보 응답 최대 건수(초과 시 절단 + truncated=true). */
    private static final int GPS_LIST_MAX = 1000;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendSmsAuth(PlatformOperatorParam param) {

        // 1) 발송 대상은 토큰의 운영자 본인 등록 휴대폰(TB_USER) — 클라 입력 휴대폰 불신.
        OperatorMblResult operatorMbl = selectOperatorMblOrThrow(param.gvCmpnyCd(), param.gvUserCd());

        // 1-1) 발송 레이트리밋(브루트포스 방어 V-2): 동일 HMAC·PLATFORM_LOCATION 목적으로
        //      최근 1분 내 발송이 있으면 신규 발송 거부(셀프가입 코어 baseinfo 는 미적용 — 무회귀).
        if (platformLocationMapper.selectRecentPlatformSmsSendCnt(operatorMbl.mblNoHmac()) > 0) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_013);
        }

        // 2) 6자리 인증코드 생성(baseinfo insertSmsAuthNo 미러 — SecureRandom).
        //    인증번호 평문은 어떤 로그에도 남기지 않는다(prafta-app-032 보강 규약 승계).
        String certNo = Integer.toString(100000 + secureRandom.nextInt(900000));

        // 3) 목적 PLATFORM_LOCATION 으로 INSERT(코드 유효 1분). ENC 값은 TB_USER 것을 그대로 복사(평문 미접촉).
        platformLocationMapper.insertPlatformSmsAuth(new PlatformSmsAuthInsertCommand(
                operatorMbl.mblNoEnc()
                , operatorMbl.mblNoHmac()
                , certNo
                , param.gvUserCd()
        ));

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
        List<GpsInfoResult> gpsList = platformLocationMapper.selectGpsList(GpsListQuery.from(param));

        boolean truncated = gpsList.size() > GPS_LIST_MAX;
        if (truncated) {
            gpsList = gpsList.subList(0, GPS_LIST_MAX);
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
