package com.prafta.app.approval.admin.dto.response;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

/**
 * 001-P2-B3: 승인 상세(A-2) 응답. plan §3-C 계약.
 *
 * <p>gate 는 서버 산출(②본인결재차단/④마감/충돌). lockedYn 은 항상 false(A-4 미구현).
 * body 는 유형별 본문(Map). attachments 는 현행 스키마에 첨부 테이블이 없어 빈 리스트.
 */
@Getter
@Builder
public class ApprovalDetailResponse {

    private final Meta meta;
    private final Gate gate;
    private final Map<String, Object> body;
    private final String reason;
    private final List<Object> attachments;

    @Getter
    @Builder
    public static class Meta {
        private final String reqId;
        private final String group;
        private final String reqType;
        private final String reqTypeNm;
        private final String reqStatus;
        private final String requesterUserNm;
        private final String requesterUserCd;
        private final String nodeNm;
        private final String reqDate;
        private final String targetYmd;
        private final Integer deadlineDday;
        private final String deadlineLevel;
        private final Integer approvalStep;
    }

    @Getter
    @Builder
    public static class Gate {
        private final boolean canProcess;
        private final boolean selfBlockedYn;
        private final boolean closedYn;
        /** A-4(선점 잠금) 미구현 — 항상 false. */
        private final boolean lockedYn;
        private final String lockedByNm;
        private final boolean conflictYn;
        private final String conflictMsg;
    }
}
