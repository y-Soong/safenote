package com.prafta.web.attd.attd07.result;

/**
 * 월간 근태 목록 조회 시 함께 반환되는 초과근무(OT) 1건 결과.
 * TB_USER_OVERTIME_MGMT 행 1:1 매핑.
 *
 * <p>PRAFTA-017: Attd_07 목록뷰에서 일자별 초과근무를 함께 노출하기 위해
 *    {@code selectMonthlyOvertimeList} 가 반환하는 월 단위 OT 목록.
 *    {@link DailyOvertimeResult} 와 컬럼 구성은 동일하되, 월 단위 조회이므로
 *    어느 사용자/사업장의 OT 인지 식별할 수 있도록 userCd / siteCd 를 추가로 담는다.
 *    필드명은 DB 컬럼(UPPER_SNAKE)을 lowerCamel 로 변환한 형태이며 명시적 alias 를 사용한다.
 */
public record MonthlyOvertimeResult(
      String otId
    , String attdId
    , String reqId

    , String cmpnyCd
    , String siteCd
    , String userCd
    , String workYmd
    , String nodeCd

    , String otType

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
