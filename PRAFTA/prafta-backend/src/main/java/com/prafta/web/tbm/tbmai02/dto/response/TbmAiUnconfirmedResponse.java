package com.prafta.web.tbm.tbmai02.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * TBM 교육안 생성 사전 차단용 "미확정 AI 분석 항목" 조회 응답.
 *
 * <p>세션에 묶인 교육자료 세부항목 중 AI 분석 지정(AI_ANALYZE_YN='Y')됐지만 아직 확정되지 않은 항목 목록.
 *    FE 는 이 목록이 비어있지 않으면 [AI 교육안 생성] 버튼을 잠그고 경고 블록을 노출한다(최종 차단은 서버 generate).
 *
 * <ul>
 *   <li>{@code count} — 미확정 항목 수(0 이면 생성 가능).</li>
 *   <li>{@code items} — 항목 상세. {@code statusLabel} 은 서버가 타입/상태를 조합해 채워 보낸다(FE 중복 매핑 방지).</li>
 * </ul>
 */
@Getter
@Builder
public class TbmAiUnconfirmedResponse {

    private int count;
    private List<Item> items;

    @Getter
    @Builder
    public static class Item {
        private String mtrlTitle;     // 교육자료 제목
        private String mtrlItemCd;    // 항목 코드
        private String mtrlItemType;  // 항목 타입(COM003: 01/02/03/04)
        private String mtrlDesc;      // 항목 설명(NULL 가능 — FE 는 공백이면 "(설명 없음)")
        private String aiStatus;      // AI 상태 원본(NONE/ANALYZING/DRAFT/FAILED/CONFIRMED/NULL)
        private String statusLabel;   // 서버가 채운 사용자 노출 상태 라벨
    }
}
