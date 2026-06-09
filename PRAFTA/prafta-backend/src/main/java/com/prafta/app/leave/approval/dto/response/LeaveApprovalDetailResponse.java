package com.prafta.app.leave.approval.dto.response;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;

/**
 * 사용자연차결재-01 (3-B 상세): 연차 결재 상세 응답.
 *
 * <p>gate 는 서버 산출(본인결재차단/마감/충돌 + 내 단계/상태). body 는 연차 본문(Map): leaveCd/leaveNm/paidYn/
 * unitNm/appliedRange/balance/steps. reason 은 신청 사유.
 */
@Getter
@Builder
public class LeaveApprovalDetailResponse {

    private final Meta meta;
    private final Gate gate;
    private final Map<String, Object> body;
    private final String reason;

    @Getter
    @Builder
    public static class Meta {
        private final String reqId;
        private final String group;
        private final String reqType;
        private final String reqTypeNm;
        private final String reqStatus;
        private final String reqStatusNm;
        private final String requesterUserNm;
        private final String requesterUserCd;
        private final String nodeNm;
        private final String reqDate;
        private final String targetYmd;
        /** 현재 결재 차례 단계('01'). */
        private final Integer approvalStep;
    }

    @Getter
    @Builder
    public static class Gate {
        private final boolean canProcess;
        private final boolean selfBlockedYn;
        private final boolean closedYn;
        private final boolean conflictYn;
        private final String conflictMsg;
        /** 결재선에서 내가 결재자인 단계 번호. */
        private final Integer myStep;
        /** 내 단계 상태[SYS044] 00대기/01신청(차례)/02승인/03반려. */
        private final String myStepStatus;
    }
}
