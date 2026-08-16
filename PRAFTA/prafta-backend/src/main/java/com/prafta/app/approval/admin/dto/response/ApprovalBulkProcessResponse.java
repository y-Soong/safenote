package com.prafta.app.approval.admin.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-leavemulti: 앱 관리자 승인 일괄 처리 결과.
 *
 * <p><b>부분 성공</b>을 표현한다. 관리자가 14일 묶음을 승인했는데 1일이 마감으로 막혔다면 나머지 13일은
 * 정당하게 승인된 것이므로 되돌리지 않는다. 실패 건은 사유와 함께 내려 화면이 "N건 처리 · M건 제외" 로 안내한다.
 * 전부 성공으로 뭉뚱그리면 관리자가 전건 처리됐다고 오인한다.
 *
 * <p>형상은 웹 {@code LeaveApprovalBulkResponse} 와 동형이다(화면 처리 코드를 웹과 같은 모양으로 유지).
 */
@Getter
@Builder
public class ApprovalBulkProcessResponse {

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
