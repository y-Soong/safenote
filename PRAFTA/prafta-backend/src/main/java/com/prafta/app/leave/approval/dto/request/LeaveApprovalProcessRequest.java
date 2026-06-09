package com.prafta.app.leave.approval.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자연차결재-01 (3-D 처리): 연차 결재 단계 승인/반려 요청(camelCase JSON).
 *
 * <p>식별자(cmpny/user)는 본문으로 받지 않고 JWT 클레임에서만 도출한다(IDOR 차단).
 * 본문은 리소스 키(reqId/approvalStep)와 처리 의사(decision/comment)만 수용한다.
 *
 * <p>decision: APPROVE(요청대로 승인) / REJECT(반려). 연차는 조정 불가(재기획서 §5.8.4).
 *   REJECT 시 comment 필수(이력 보존 §9.5).
 */
@Getter
@Setter
@NoArgsConstructor
public class LeaveApprovalProcessRequest {

    /** 요청 ID(리소스 키). 서버가 결재선 실존(isApproverOf)을 재검증한다. */
    private String reqId;

    /** 연차 다단 결재 단계 번호(내 단계). */
    private Integer approvalStep;

    /** 처리 의사(APPROVE/REJECT). */
    private String decision;

    /** 처리 코멘트(REJECT 시 필수). */
    private String comment;
}
