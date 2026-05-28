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
) {
}
