package com.prafta.app.attd.attd01.result;

import java.time.LocalDateTime;

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

    // 작업지시서_소속이동-이력가시성-보정(QA High 보정): 동일 WORK_YMD+WORK_SEQ 에 SITE_CD 가 다른
    //   행이 공존할 때(ATTD_ID 는 PK 이나 SITE_CD 는 아님) 결정론적 선택을 위한 타이브레이크 기준시각
    //   (A.UPDATE_DATE 우선, 없으면 A.INSERT_DATE). 표시 비대상, 병합 로직 전용. 반드시 마지막 필드(위치매핑).
    , LocalDateTime effectiveDtime
) {
}
