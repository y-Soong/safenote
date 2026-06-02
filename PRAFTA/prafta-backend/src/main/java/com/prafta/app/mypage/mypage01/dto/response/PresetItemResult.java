package com.prafta.app.mypage.mypage01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-010-05: 프리셋 1건(마스터 + 스텝) 응답 항목.
 */
@Getter
@Builder
public class PresetItemResult {

    private final String presetId;
    private final String presetNm;
    private final String defaultYn; // "Y" | "N"
    private final List<PresetStepItem> steps;
}
