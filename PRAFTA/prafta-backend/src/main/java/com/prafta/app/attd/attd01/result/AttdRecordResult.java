package com.prafta.app.attd.attd01.result;

/**
 * prafta-app-002: 실 출퇴근 근태 레코드 결과 (TB_USER_ATTD_MGMT).
 *
 * <p>매핑 대상: AppAttd01Mapper.selectAttdByDate / selectAttdByRange.
 * <p>레코드당 단일 SITE_CD(출근지=퇴근지). 퇴근지 분리 컬럼은 스키마에 없다.
 *   workSeq 1=1구간, 2=2구간. DEL_YN='N' 만 조회.
 * <p>siteNm 은 TB_SITE JOIN 으로 채운 레코드 사업장명.
 */
public record AttdRecordResult(
    String attdId
    , String workYmd
    , int workSeq
    , String siteCd
    , String siteNm
    , String checkInDate
    , String checkInTime
    , String checkInMethod
    , String checkOutDate
    , String checkOutTime
    , String checkOutMethod
) {
}
