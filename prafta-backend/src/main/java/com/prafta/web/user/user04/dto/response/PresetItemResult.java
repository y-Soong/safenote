package com.prafta.web.user.user04.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/** 결재라인 프리셋 1건(마스터 + 스텝) 응답 항목 (prafta-020). */
@Value
@Builder
public class PresetItemResult {
    String presetId;
    String presetNm;
    String defaultYn;
    List<PresetStepItem> steps;
}
