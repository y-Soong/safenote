package com.prafta.platform.location.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.platform.location.application.command.LocationAccessLogCommand;
import com.prafta.platform.location.application.command.PlatformSmsAuthInsertCommand;
import com.prafta.platform.location.application.command.PlatformSmsVerifyCommand;
import com.prafta.platform.location.application.query.GpsListQuery;
import com.prafta.platform.location.application.query.OperatorMblQuery;
import com.prafta.platform.location.application.query.PlatformSmsCertQuery;
import com.prafta.platform.location.application.query.PlatformSmsVerifiedQuery;
import com.prafta.platform.location.application.result.GpsInfoResult;
import com.prafta.platform.location.application.result.LocationSiteResult;
import com.prafta.platform.location.application.result.OperatorMblResult;
import com.prafta.platform.location.application.result.PlatformSmsVerifiedResult;

/**
 * 플랫폼 위치정보 열람(Platform_04) 전용 매퍼.
 *
 * <p>쓰기는 SMS 인증 레코드(목적 PLATFORM_LOCATION)와 열람 로그(append-only) INSERT 만 —
 * 고객사 데이터(TB_USER_ATTD_GPS 등)에 대한 쓰기 statement 를 두지 않는다.
 */
@Mapper
public interface PlatformLocationMapper {

    // ===== SMS 인증 게이트(목적 PLATFORM_LOCATION — 셀프가입 흐름과 격리) =====

    /** 운영자 본인 등록 휴대폰(ENC/HMAC) 조회. 미등록이면 null. */
    OperatorMblResult selectOperatorMbl(OperatorMblQuery query);

    /** 최근 1분 내 동일 HMAC·PLATFORM_LOCATION 목적 발송 건수 — 발송 레이트리밋(V-2). */
    int selectRecentPlatformSmsSendCnt(@Param("mblNoHmac") String mblNoHmac);

    /** 인증코드 INSERT(PURPOSE_CD='PLATFORM_LOCATION', 유효 1분 — baseinfo insertSmsAuthNo 미러). */
    int insertPlatformSmsAuth(PlatformSmsAuthInsertCommand command);

    /** 미검증·미만료 인증코드 매칭(SMS_ID). 불일치/만료/실패 5회 이상 시 null. */
    String selectPlatformCertSmsId(PlatformSmsCertQuery query);

    /** 인증번호 불일치 시 최신 미검증 PLATFORM_LOCATION 레코드 FAIL_CNT +1(브루트포스 카운터 V-2). */
    int increasePlatformSmsFailCnt(@Param("mblNoHmac") String mblNoHmac);

    /** 인증 통과 처리(VERIFIED_YN='Y' — baseinfo updateSmsAuthReq 미러). */
    int updatePlatformSmsVerified(PlatformSmsVerifyCommand command);

    /**
     * 유효(10분 창) 인증 상태 판정 — baseinfo selectSmsVerifiedSmsId 미러.
     * 소비(consume)하지 않는다: 10분 창 내 다회 조회 허용(요청서 §3-2). 없으면 null.
     */
    PlatformSmsVerifiedResult selectPlatformSmsVerified(PlatformSmsVerifiedQuery query);

    // ===== 위치정보 조회(read-only) =====

    /** 대상 회사 사업장 목록(좌표/지오펜스 반경 포함). */
    List<LocationSiteResult> selectLocationSiteList(@Param("cmpnyCd") String cmpnyCd);

    /** 위치정보 UNION 조회(근태 GPS + TBM 입실, LIMIT 1001 — 초과 절단 판정용). */
    List<GpsInfoResult> selectGpsList(GpsListQuery query);

    // ===== 열람 로그(append-only — UPDATE/DELETE statement 금지) =====

    /** 열람 로그 1건 INSERT(조회와 동일 트랜잭션 — 실패 시 조회 전체 롤백). */
    int insertLocationAccessLog(LocationAccessLogCommand command);
}
