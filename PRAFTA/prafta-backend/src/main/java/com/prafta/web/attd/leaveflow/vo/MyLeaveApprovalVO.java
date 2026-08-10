package com.prafta.web.attd.leaveflow.vo;

import java.math.BigDecimal;

/**
 * 내 결재함 — 내가 현재 결재 단계의 결재자인 연차 요청 1건 (prafta-019-E 후속).
 *
 * <p>요청승인관리 화면 '연차 상신' 탭(재기획서 §5.8.4)의 접수함/상세 표시용.
 */
public record MyLeaveApprovalVO(
      String reqId
    , Integer approvalStep
    , String requesterUserCd
    , String requesterUserNm
    , String nodeNm
    , String workYmd
    , String leaveType
    , String leaveCd
    , String leaveNo
    , String leaveNm
    , String useUnitType
    , String unitNm
    , BigDecimal leaveDays
    , Integer leaveMinutes
    , String startTime
    , String endTime
    , String reqReason
    , String reqDate
    /** 요청자 == 결재자(본인 결재) 여부 (§7.3) */
    , String selfYn
    /**
     * 가불(미래 연차 당겨쓰기) 충당 일수 (가불표시-01).
     * 이 요청의 차감(use, CONFIRMED)이 가불 GRANT(GRANT_REASON '[가불] ' 프리픽스)를 충당한 LEAVE_DAYS 합.
     * 항상 0 이상(null 없음) — 소비측은 0 초과 여부로 가불 배지를 판정한다.
     * ★ MyBatis record 위치 기반 매핑 — 반드시 맨 끝 유지(SELECT 맨 끝 컬럼 borrowDays 와 순서 일치).
     */
    , BigDecimal borrowDays
) {
}
