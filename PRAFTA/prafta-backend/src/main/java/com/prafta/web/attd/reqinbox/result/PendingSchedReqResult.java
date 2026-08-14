package com.prafta.web.attd.reqinbox.result;

/**
 * 요청 승인 관리 — 스케줄 수정(REQ_TYPE='10') 대기 요청 1건 + 현재→요청 스케줄 비교값.
 *
 * <p>기존 {@link PendingReqResult} 와 컬럼 세트가 달라 별도 record 로 분리한다
 * (기존 3탭 무회귀 — plan 결정 B). 비교 필드 집합은 앱 관리자 승인 상세
 * {@code app.approval.admin.result.SchedBodyRow} 와 동일하다.
 *
 * <p>⚠ MyBatis record 매핑은 <b>SELECT 컬럼 순서</b>로 바인딩된다. 필드 중간 삽입 금지,
 * 추가는 항상 끝에 한다(메모리 feedback_mybatis_record_column_order).
 *
 * <p>{@code START_DATE/START_TIME/END_DATE/END_TIME} 은 REQ_TYPE='10' 에서 항상 NULL 이라
 * 조회하지 않는다. {@code nodeCd} 는 REQ 원본값(NULL 가능)이며 승인/반려 키로 그대로 전달해야 한다.
 * {@code cur*} 는 해당 일자 근무계획이 없거나 연차코드면 전부 NULL 이다.
 */
public record PendingSchedReqResult(
      String reqId
    , String reqType
    , String userCd
    , String userNm
    , String siteCd
    , String nodeCd
    , String nodeNm
    , String workYmd
    , Integer workSeq
    , String reqReason
    , String reqDate
    // 요청 스케줄(req*) — REQ.SCH_CD 를 근무일 기준 유효버전으로 해석
    , String reqSchCd
    , String reqSchNo
    , String reqFstStrTime
    , String reqFstEndTime
    , String reqSecStrTime
    , String reqSecEndTime
    , String reqPreFixedOtStrTime
    , String reqPreFixedOtEndTime
    , String reqFixedOtStrTime
    , String reqFixedOtEndTime
    // 현재 스케줄(cur*) — work_plan.WORK_PLAN_CD 를 근무일 기준 유효버전으로 해석
    , String curSchCd
    , String curSchNo
    , String curFstStrTime
    , String curFstEndTime
    , String curSecStrTime
    , String curSecEndTime
    , String curPreFixedOtStrTime
    , String curPreFixedOtEndTime
    , String curFixedOtStrTime
    , String curFixedOtEndTime
) {
}
