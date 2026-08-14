package com.prafta.web.attd.leaveflow.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-leavemulti: 연차 결재 일괄 승인/반려 결과.
 *
 * <p><b>부분 성공</b>을 표현한다. 관리자가 14일 묶음을 승인했는데 1일이 마감으로 막혔다면,
 * 나머지 13일은 정당하게 승인된 것이므로 그 13일을 되돌릴 이유가 없다
 * (신청자의 기간신청은 전체 실패지만 — 그건 "내 휴가가 잘리면 안 된다"는 다른 성격의 요구다).
 *
 * <p>실패 건은 사유와 함께 내려 화면이 {@code BatchResultPop} 으로 안내한다.
 */
@Getter
@Builder
public class LeaveApprovalBulkResponse {

    /** 처리 성공 건수. */
    private final int successCount;

    /** 처리 실패(스킵) 건수. */
    private final int failedCount;

    /** 실패 건 상세(성공 건은 담지 않는다). */
    private final List<Failed> failedList;

    @Getter
    @Builder
    public static class Failed {
        private final String reqId;
        private final Integer approvalStep;
        /** 실패 사유 코드(ApiException 의 errorCode, 그 외는 null). */
        private final String reasonCode;
        /** 사용자 안내 문구. */
        private final String reason;
    }
}
