package com.prafta.web.tbm.tbmai01.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbmai01.application.model.TbmAiItemStatusRow;

/**
 * analysis-status 응답(자료 단위 항목별 AI 상태 목록).
 *
 * <p>대화이력(AI_CHAT_JSON) 파싱은 서비스가 수행해 {@link ItemStatus#of} 로 주입한다(objectMapper 의존 격리).
 */
public record TbmAiAnalysisStatusResponse(
    String mtrlCd
    , List<ItemStatus> items
    , String disclaimer
) {
    /** 항목 1건의 AI 상태 스냅샷. */
    public record ItemStatus(
        String mtrlItemCd
        , String mtrlItemType
        , String aiAnalyzeYn
        , String aiStatus
        , String aiDraftText
        , String aiConfirmDesc
        , List<TbmAiChatTurn> turns
        , String aiAnalyzedAt
    ) {
        /** 목록행 + 파싱된 대화턴 → 항목 상태. */
        public static ItemStatus of(TbmAiItemStatusRow row, List<TbmAiChatTurn> turns) {
            return new ItemStatus(
                row.mtrlItemCd()
                , row.mtrlItemType()
                , row.aiAnalyzeYn()
                , row.aiStatus()
                , row.aiDraftText()
                , row.aiConfirmDesc()
                , turns
                , row.aiAnalyzedAt()
            );
        }
    }

    public static TbmAiAnalysisStatusResponse from(String mtrlCd, List<ItemStatus> items, String disclaimer) {
        return new TbmAiAnalysisStatusResponse(mtrlCd, items, disclaimer);
    }
}
