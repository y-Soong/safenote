package com.prafta.web.attd.attd13.service;

import java.util.List;

import com.prafta.web.attd.attd13.application.param.ChangeRequestConfirmParam;
import com.prafta.web.attd.attd13.application.param.ChangeRequestCreateParam;
import com.prafta.web.attd.attd13.application.param.ChangeRequestListParam;
import com.prafta.web.attd.attd13.application.param.ChangeRequestRejectParam;
import com.prafta.web.attd.attd13.result.LeaveChangeRequestRowResult;
import com.prafta.web.attd.attd13.result.MovableLeaveResult;

/**
 * 연차 변경/삭제 동의·거부 서비스 (PRAFTA-COM-008-C).
 *
 * <p>관리자 발의(MOVE/DELETE) → 근로자 응답(AGREE/REJECT) → 관리자 확인 → 반영 의 상태머신과,
 * 근로자 발의(MOVE 전용, C-5a)를 단일 서비스로 처리한다. 웹(attd13)·앱(leavechange) 컨트롤러가 공유한다.
 *
 * <p>권한/IDOR/마감 가드는 {@code AttdCloseService}(prafta-028/042 재사용)로 강제하고,
 * 부여 재계산은 {@code LeaveFlowMapper.recomputeGrantUsedDays}, 알림 적재는 {@code LeaveDashboardMapper}
 * outbox 메서드를 재사용한다(신규 SQL 난립 방지).
 */
public interface Attd13Service {

    // ===== 관리자(웹) =====

    /** 변경 요청 목록 조회(관리자 스코프 + 검색). */
    List<LeaveChangeRequestRowResult> getChangeRequests(ChangeRequestListParam param);

    /** 변경 요청 단건 상세 조회(관리자, 확인/반려 팝업용). 대상자 관리 권한 재검증(safe 제외). 없으면 404. */
    LeaveChangeRequestRowResult getChangeRequestDetail(String cmpnyCd, String authCd, String userCd, String changeReqId);

    /** 관리자 발의(MOVE/DELETE) → REQUESTED 생성 + 근로자 PUSH 적재. */
    void createChangeRequest(ChangeRequestCreateParam param);

    /** 관리자 최종 확인(AGREED 만) → 실제 반영(MOVE/DELETE) + 상태 CONFIRMED + 근로자 PUSH 적재. */
    void confirmChangeRequest(ChangeRequestConfirmParam param);

    /**
     * 관리자 반려(작업2) — AGREED(주로 WORKER 발의건) → REQ_STATUS='REJECTED'. 원 연차 불변.
     * 사유 필수. 근로자에게 반려 PUSH 적재. 멱등(이미 REJECTED/CONFIRMED 면 409).
     */
    void rejectChangeRequest(ChangeRequestRejectParam param);

    // ===== 근로자(앱) =====

    /** 근로자 본인 대기(PENDING) 응답 대상 요청 목록. */
    List<LeaveChangeRequestRowResult> getPendingConsents(String cmpnyCd, String userCd);

    /** 근로자 응답(AGREE/REJECT). 거부 시 사유 필수·원 연차 불변. 관리자 PUSH 적재. */
    void respondChangeRequest(String cmpnyCd, String userCd, String changeReqId,
                              String workerResponse, String responseReason);

    /** 근로자 본인 이동 가능 연차일 목록(C-5a). */
    List<MovableLeaveResult> getMovableLeaves(String cmpnyCd, String userCd);

    /** 근로자 이동 발의(C-5a, MOVE 전용·취소 불가) → REQUESTED 생성. 관리자 승인은 confirm 흐름과 통합. */
    void createWorkerMoveRequest(String cmpnyCd, String userCd, String targetLeaveId,
                                 String moveTargetDate, String reqReason);

    /**
     * 근로자 취소(삭제) 발의(2026-08-18 개방 — 008-C §3-2 "근로자 취소 불가" 조항 개정).
     * 대상 = 본인 소유 + CONFIRMED + 미도래. 촉진 지정 건은 차단. 생성 즉시 AGREED(WORKER 발의 관례) —
     * 관리자 승인(applyDelete 반영)/반려는 confirm/reject 흐름과 통합.
     */
    void createWorkerDeleteRequest(String cmpnyCd, String userCd, String targetLeaveId, String reqReason);
}
