package com.prafta.web.tbm.tbmai01.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbmai01.application.model.TbmAiItemRow;

/**
 * 단일 항목 액션 결과 응답(chat-item · confirm-item · reanalyze · manual-confirm 공용).
 */
public record TbmAiItemResponse(
    String mtrlItemCd
    , String mtrlItemType
    , String aiStatus
    , String aiDraftText
    , String aiConfirmDesc
    , List<TbmAiChatTurn> turns
    , String aiAnalyzedAt
    , String disclaimer
) {
    /** 항목 AI 행 + 파싱된 대화턴 → 응답. */
    public static TbmAiItemResponse from(TbmAiItemRow row, List<TbmAiChatTurn> turns, String disclaimer) {
        return new TbmAiItemResponse(
            row.mtrlItemCd()
            , row.mtrlItemType()
            , row.aiStatus()
            , row.aiDraftText()
            , row.aiConfirmDesc()
            , turns
            , row.aiAnalyzedAt()
            , disclaimer
        );
    }
}
