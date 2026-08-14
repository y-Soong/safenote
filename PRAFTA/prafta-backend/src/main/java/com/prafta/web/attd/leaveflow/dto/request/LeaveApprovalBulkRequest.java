package com.prafta.web.attd.leaveflow.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-leavemulti: 연차 결재 <b>일괄</b> 승인/반려 요청.
 *
 * <p>연차 기간(From-To) 신청은 날짜별 REQ N건으로 분해되므로, 2주 휴가면 관리자가 14번 승인해야 한다.
 * 묶음을 한 번에 처리하기 위한 엔드포인트다.
 *
 * <p>계약은 <b>단건 엔드포인트와 동일</b>하게 (reqId, approvalStep) 쌍의 목록이다.
 * 결재함 목록({@code my-approvals})이 이미 두 값을 내려주므로 화면이 그대로 모아 보내면 된다
 * — 서버가 단계를 재추론하지 않으니 검증 경로가 단건과 완전히 같아진다.
 *
 * <p>묶음 전체 처리(정책 ④ 기본)든 일부만 선택한 개별 처리든 같은 요청으로 표현된다.
 */
@Getter
@Setter
public class LeaveApprovalBulkRequest {

    @NotEmpty
    private List<Item> items;

    /** 처리 코멘트(전 건 공통). */
    private String comment;

    @Getter
    @Setter
    public static class Item {
        private String reqId;
        private Integer approvalStep;
    }
}
