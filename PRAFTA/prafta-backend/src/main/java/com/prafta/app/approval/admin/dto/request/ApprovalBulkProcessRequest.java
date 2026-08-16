package com.prafta.app.approval.admin.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * prafta-leavemulti: 앱 관리자 승인 <b>일괄</b> 처리 요청(연차 기간신청 묶음 승인/반려).
 *
 * <p>연차 기간(From-To) 신청은 날짜별 REQ N건으로 분해되므로, 2주 휴가면 관리자가 14번 승인해야 한다.
 * 묶음을 한 번에 처리하기 위한 요청이며, 계약은 단건 {@code /process} 와 동일한 어휘(group/decision/comment)를 쓴다.
 *
 * <p>items 는 (reqId, approvalStep) 쌍의 목록이다. 대기 목록이 두 값을 이미 내려주므로 화면이 그대로 모아 보내면 되고,
 * 서버가 단계를 재추론하지 않으니 검증 경로가 단건과 완전히 같아진다.
 *
 * <p>식별자(cmpny/site/user/auth)는 본문으로 받지 않고 JWT 클레임에서만 도출한다(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class ApprovalBulkProcessRequest {

    /** 유형 그룹. v1 은 {@code LEAVE} 만 허용(그 외 400). */
    private String group;

    /** 처리 의사(APPROVE_ASIS / REJECT). APPROVE_ADJUST 는 일괄에서 의미가 없어 미지원. */
    private String decision;

    /** 처리 코멘트(전 건 공통). REJECT 시 필수(공백 제외 1자 이상 — 앱 단건과 동일 기준). */
    private String comment;

    @NotEmpty
    private List<Item> items;

    /** 처리 대상 1건. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Item {
        private String reqId;
        /** 연차 다단 결재 단계 번호(LEAVE 필수). */
        private Integer approvalStep;
    }
}
