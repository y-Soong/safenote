package com.prafta.app.leave.approval.service;

import com.prafta.app.leave.approval.application.param.LeaveApprovalDetailParam;
import com.prafta.app.leave.approval.application.param.LeaveApprovalHistoryParam;
import com.prafta.app.leave.approval.application.param.LeaveApprovalPendingParam;
import com.prafta.app.leave.approval.application.param.LeaveApprovalProcessParam;
import com.prafta.app.leave.approval.dto.response.LeaveApprovalDetailResponse;
import com.prafta.app.leave.approval.dto.response.LeaveApprovalHistoryResponse;
import com.prafta.app.leave.approval.dto.response.LeaveApprovalPendingResponse;
import com.prafta.app.leave.approval.dto.response.LeaveApprovalProcessResponse;

/**
 * 사용자연차결재-01: 사용자 모드 연차 결재 관리 서비스(결재자 본인 스코프).
 *
 * <p>대기·처리는 표준 엔진 {@code LeaveFlowService} 위임(비즈니스 로직 신규 작성 없음), 상세는 읽기 SQL 포팅 +
 * 게이트 산출, 이력은 "내 단계 행동 기준" 신규 쿼리다. 관리자 승인 관리 엔드포인트와 무관(권한 게이트 미사용).
 */
public interface AppLeaveApprovalService {

    /** 3-A 대기: 내가 현재 단계 결재자인 연차(05/06) 요청 목록. */
    LeaveApprovalPendingResponse selectPending(LeaveApprovalPendingParam param);

    /** 3-B 상세: 메타/게이트/본문(구간·잔여·결재선)/사유. isApproverOf 재검증(IDOR). */
    LeaveApprovalDetailResponse selectDetail(LeaveApprovalDetailParam param);

    /** 3-C 이력: 내가 처리(승인/반려)한 연차 요청 내역. */
    LeaveApprovalHistoryResponse selectHistory(LeaveApprovalHistoryParam param);

    /** 3-D 처리: 연차 단계 승인/반려. isApproverOf 게이트 후 엔진 위임(@Transactional). */
    LeaveApprovalProcessResponse process(LeaveApprovalProcessParam param);
}
