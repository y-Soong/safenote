package com.prafta.app.leave.approval.result;

/**
 * 사용자연차결재-01 (A-2 상세): 연차 요청 메타 1행(회사 스코프) — AppAdminApprovalMapper.selectReqMeta 연차 포팅.
 *
 * <p>관리자 모듈 의존을 끊기 위해(F-DETAIL) 신규 mapper 로 포팅한 레코드다. 게이트(본인결재/마감/충돌)
 * 산출에 필요한 최소 필드만 담는다. 식별자는 서비스가 토큰값으로만 전달한다(IDOR 차단).
 */
public record LeaveReqMetaRow(
      String reqId
    , String reqType
    , String reqStatus
    , String userCd
    , String requesterUserNm
    , String siteCd
    , String nodeNm
    , String workYmd
    , String targetId
    , String reqReason
    , String reqDate
) {
}
