package com.prafta.app.mypage.mypage01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-010-05: 프리셋 저장(신규/수정) 응답 — 채번/대상 presetId.
 */
@Getter
@Builder
public class PresetSaveResponse {

    private final String presetId;
}
