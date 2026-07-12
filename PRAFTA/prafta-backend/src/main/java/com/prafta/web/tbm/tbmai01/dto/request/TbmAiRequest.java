package com.prafta.web.tbm.tbmai01.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * TBM 교육자료 세부항목 AI 분석·확정 공통 요청.
 *
 * <p>GET analysis-status(@ModelAttribute) · POST analyze-items/chat-item/confirm-item/reanalyze/manual-confirm
 *    (@RequestBody) 공용. (@ModelAttribute · @RequestBody 양쪽 바인딩을 위해 getter/setter 를 둔다.)
 * <p>식별자(cmpnyCd/userCd/authCd)는 바디에 두지 않고 JWT 클레임에서만 도출한다(IDOR 차단).
 * <ul>
 *   <li>{@code mtrlCd} — 자료 단위 스코프(analyze-items · analysis-status).</li>
 *   <li>{@code mtrlItemCd} — 항목 단위 스코프(chat-item · confirm-item · reanalyze · manual-confirm).</li>
 *   <li>{@code userMessage} — 대화형 보완 메시지(chat-item).</li>
 *   <li>{@code confirmText} — 확정 서술 직접 지정(confirm-item, 선택).</li>
 *   <li>{@code adminNote} — 재분석 시 관리자 의견(reanalyze, 선택, 요청단발·미영속).</li>
 *   <li>{@code manualText} — 실패 항목 수기 확정 서술(manual-confirm).</li>
 * </ul>
 */
@Getter
@Setter
public class TbmAiRequest {
    private String mtrlCd;
    private String mtrlItemCd;
    private String userMessage;
    private String confirmText;
    private String adminNote;
    private String manualText;
}
