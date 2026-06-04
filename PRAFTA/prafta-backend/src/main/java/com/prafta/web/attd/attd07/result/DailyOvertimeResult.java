package com.prafta.web.attd.attd07.result;

/**
 * 일자 상세 조회 시 함께 반환되는 초과근무(OT) 1건 결과.
 * TB_USER_OVERTIME_MGMT 행 1:1 매핑.
 *
 * <p>PRAFTA-003-6: 근태 일자 상세 응답에 OT 리스트를 함께 노출한다.
 *    필드명은 DB 컬럼(UPPER_SNAKE)을 lowerCamel로 변환한 형태로 통일한다
 *    (MyBatis map-underscore-to-camel-case 의존 X — 명시적 alias 사용).
 */
public record DailyOvertimeResult(
      String otId
    , String attdId
    , String reqId

    , String workYmd
    , String nodeCd

    /* 계획 시각 (신청/승인 시점) */
    , String planStartDate
    , String planStartTime
    , String planEndDate
    , String planEndTime

    /* 실제 수행 시각 (가산수당 계산 기준) */
    , String actualStartDate
    , String actualStartTime
    , String actualStartMethod
    , String actualEndDate
    , String actualEndTime
    , String actualEndMethod

    /* 근무시간 계산 결과 */
    , Integer workMinutes
    , Integer breakMinutes

    /* OT 상태 */
    , String otStatus

    /* 공통 관리 컬럼 */
    , String insertDate
) {
}
