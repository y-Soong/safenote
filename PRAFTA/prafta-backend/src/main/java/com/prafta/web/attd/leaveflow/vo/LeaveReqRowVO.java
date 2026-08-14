package com.prafta.web.attd.leaveflow.vo;

import java.math.BigDecimal;

/**
 * tb_user_attd_req 연차 요청 단건 (prafta-019-E 결재 진행 시 소유권/상태 확인용).
 *
 * <p>PRAFTA-025: 연차 수정('06') 처리를 위해 reqType / targetId(=대상 LEAVE_ID) 및
 * 요청이 담은 "새 값"(workYmd / start·end date·time / leaveDays)을 함께 싣는다.
 * 05(연차사용)은 기존과 동일하게 동작하며, 06일 때만 최종 승인 시 이 값들로
 * 기존 사용 기록(TARGET_ID)을 in-place 갱신한다.
 */
public record LeaveReqRowVO(
      String reqId
    , String cmpnyCd
    , String siteCd
    , String userCd
    , String reqStatus
    , String reqType
    , String targetId
    , String workYmd
    , String startDate
    , String startTime
    , String endDate
    , String endTime
    , BigDecimal leaveDays
    /**
     * prafta-leavemulti: 연차 기간(From-To) 신청 묶음 ID. 단일일 신청은 null.
     *
     * <p>승인/반려 결과 통보를 <b>묶음 1건으로 수렴</b>시키는 데 쓴다. 이 값이 없으면 14일 휴가를
     * 승인할 때 신청자에게 결과 알림이 14개 간다(QA 에서 실제로 5건 발생해 발견).
     *
     * <p>★ MyBatis record 위치 기반 매핑 — 반드시 맨 끝 유지(SELECT 맨 끝 컬럼과 순서 일치).
     */
    , String leaveGroupId
) {
}
