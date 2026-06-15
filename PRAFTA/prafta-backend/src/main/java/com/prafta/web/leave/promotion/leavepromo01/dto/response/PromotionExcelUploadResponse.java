package com.prafta.web.leave.promotion.leavepromo01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-com-008-A-6: 엑셀 업로드(사용자-연차날짜) 결과 응답.
 *
 * <p>프론트(LeavePromotionExcelPop) 계약 = { successCount, failItems:[{ sourceRow, reason }], failsToken }.
 * 실패행 2시트(prafta-052) 다운로드는 failsToken 으로 {@code GET /webApi/leavepromo01/excel/fails?token=}
 * 를 호출한다(서버가 AES-GCM 보관분을 복호화해 스트림).
 *
 * @param totalCount   처리 행 수
 * @param successCount 등록 성공(REGISTERED) 합계
 * @param failCount    실패(검증/잔여부족/마감/비근무일) 합계
 * @param failItems    실패 항목(원본 행 + 사유) — 표시용. 평문 PII 포함 가능(응답 1회성, 저장 안 함).
 * @param failsToken   실패행 2시트 다운로드 토큰(실패 0건이면 null)
 */
@Getter
@Builder
public class PromotionExcelUploadResponse {

    private final int totalCount;
    private final int successCount;
    private final int failCount;
    private final List<FailItemView> failItems;
    private final String failsToken;

    @Getter
    @Builder
    public static class FailItemView {
        private final String userCd;
        private final String reason;
        private final List<String> sourceRow;
    }
}
