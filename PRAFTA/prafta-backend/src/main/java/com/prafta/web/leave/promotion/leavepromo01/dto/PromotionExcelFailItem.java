package com.prafta.web.leave.promotion.leavepromo01.dto;

import java.util.List;

/**
 * prafta-com-008-A-6: 엑셀 업로드(사용자-연차날짜) 실패 항목 (prafta-052 패턴).
 *
 * <p>{@code sourceRow} = 실패 행의 원본 입력값(양식 컬럼 순서: USER_CD/이름/부서/연차날짜). 재업로드용
 * 시트1 재구성에 쓴다. 그리드 경로가 없으므로 항상 채워진다(엑셀 전용). 사유는 시트2.
 *
 * <p>{@code sourceRow} 에는 평문 이름/부서가 포함되므로(PII), 비동기/저장 직렬화 시 전체 페이로드를
 * AES-GCM 으로 암호화한다(prafta-052: at-rest 평문 금지). 본 record 자체는 메모리 운반체.
 */
public record PromotionExcelFailItem(
        int index,
        String userCd,
        String reason,
        List<String> sourceRow
) {
}
