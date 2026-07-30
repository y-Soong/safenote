package com.prafta.web.attd.attd16.result;

import java.math.BigDecimal;

/**
 * ATTD16-T1 - 연차 사용 현황 캘린더 행(일자 전개 완료본).
 *
 * <p>기간형(START_DATE~END_DATE) 종일 연차는 매퍼의 date_seq CTE 가 일자 단위로 전개하므로
 * 같은 LEAVE_ID 가 일수만큼 반복 등장한다(dateYmd 만 다름). 프론트(Attd_16.vue)는 dateYmd 로
 * 그룹핑해 셀 인원(distinct userCd)과 우측 상세 카드(건 단위)를 만든다.
 *
 * <p>PII 최소화(plan §3): 이름/부서/연차종류/사용단위/시간대/일수만 내려준다. 사용 사유
 * (LEAVE_REASON), 증빙 파일(EVIDENCE_FILE_ID), 연락처/이메일/생년월일은 응답에 포함하지 않는다.
 * userCd 는 셀 인원 distinct 판정용 행 키로만 사용한다.
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서를 생성자 인자 순서로 사용한다
 * ({@code feedback_mybatis_record_column_order}) — Attd16Mapper.xml
 * selectLeaveUsageCalendarList 의 SELECT 절 순서를 본 레코드 필드 선언 순서와 일치시킬 것.
 */
public record LeaveUsageCalendarRowResult(
        String dateYmd
        , String userCd
        , String userNm
        , String nodeCd
        , String nodeNm
        , String leaveCd
        , String leaveNm
        , String useUnitType
        , String startTime
        , String endTime
        , BigDecimal leaveDays
        , Integer leaveMinutes
) {
}
