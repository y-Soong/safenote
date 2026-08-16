package com.prafta.app.approval.admin.service;

import com.prafta.app.approval.admin.application.param.ApprovalDetailParam;
import com.prafta.app.approval.admin.application.param.ApprovalHistoryParam;
import com.prafta.app.approval.admin.application.param.ApprovalPendingParam;
import com.prafta.app.approval.admin.application.param.ApprovalProcessParam;
import com.prafta.app.approval.admin.dto.response.ApprovalDetailResponse;
import com.prafta.app.approval.admin.dto.response.ApprovalHistoryResponse;
import com.prafta.app.approval.admin.dto.response.ApprovalPendingResponse;
import com.prafta.app.approval.admin.dto.response.ApprovalProcessResponse;

/**
 * 001-P2: 앱 관리자 승인 관리 서비스(읽기/게이트 — A-1/A-2/A-5).
 *
 * <p>권한([권한매트릭스 §3]): APPROVAL = master ∥ hr ∥ nodeAdmin (safe 단독 ⛔).
 * <p>스코프([§4]): master=전사 / hr=사업장 / 노드관리자=자기노드+자손. 연차는 결재선(결재자 본인) 기반.
 * <p>선점잠금(A-4)·스케줄(A-5 유형)은 본 라운드 범위 외(후속). 처리(A-3)는 본 라운드 추가.
 */
public interface AppAdminApprovalService {

    /** A-1 대기 리스트 + 그룹별 counts. */
    ApprovalPendingResponse selectPending(ApprovalPendingParam param);

    /** A-2 상세 + gate(②④/충돌) 산출. reqId 토큰 스코프 재검증(IDOR). */
    ApprovalDetailResponse selectDetail(ApprovalDetailParam param);

    /** A-5 이력(처리완료 02/03/04, PROCESS_DATE DESC). */
    ApprovalHistoryResponse selectHistory(ApprovalHistoryParam param);

    /**
     * A-3 처리(승인/조정후승인/반려). group·decision 에 따라 web attd07/leaveflow 자산으로 디스패치한다.
     *
     * <p>처리 전 ④마감·멱등(409)·IDOR 스코프를 서버에서 재검증한다.
     * (본인결재차단은 2026-08-16 사용자 확정으로 제거 — 관리자 자기승인 허용.)
     * APPROVE_ADJUST 는 본 라운드 보류(연차는 계약상 조정 불가). reqId 토큰 스코프 위반 시 403.
     */
    ApprovalProcessResponse process(ApprovalProcessParam param);
}
