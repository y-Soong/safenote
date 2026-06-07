package com.prafta.app.approval.admin.dto.request;

import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 001-P2-B4: 앱 관리자 승인 처리(A-3) 요청(camelCase JSON). plan §3-D 계약.
 *
 * <p>식별자(cmpny/site/user/auth)는 본문으로 받지 않고 JWT 클레임에서만 도출한다(IDOR 차단).
 * 본문은 리소스 키(reqId)와 처리 의사(group/decision/approvalStep/comment/adjusted)만 수용한다.
 *
 * <p>decision: APPROVE_ASIS(요청대로 승인) / APPROVE_ADJUST(조정 후 승인) / REJECT(반려).
 *   - REJECT 시 comment 10자 이상 필수(서버 재검증).
 *   - LEAVE 그룹은 approvalStep 필수(다단 결재 단계 지정).
 *   - APPROVE_ADJUST 는 본 라운드 보류(연차는 계약상 조정 불가). adjusted 는 후속 라운드용 페이로드.
 */
@Getter
@Setter
@NoArgsConstructor
public class ApprovalProcessRequest {

    /** 요청 ID(리소스 키). 서버가 토큰 스코프 내인지 재검증한다. */
    private String reqId;

    /** 유형 그룹(CORRECTION/OVERTIME/LEAVE). 서버 산출 그룹과 불일치 시 거부. */
    private String group;

    /** 처리 의사(APPROVE_ASIS/APPROVE_ADJUST/REJECT). */
    private String decision;

    /** 연차 다단 결재 단계 번호(LEAVE 승인/반려 시 필수). */
    private Integer approvalStep;

    /** 처리 코멘트(REJECT 시 10자 이상 필수). */
    private String comment;

    /** APPROVE_ADJUST 조정 페이로드(본 라운드 보류 — 미사용, 후속 라운드 R3). */
    private Map<String, Object> adjusted;
}
