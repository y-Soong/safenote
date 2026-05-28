package com.prafta.web.user.user04.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/** 본인 결재라인 프리셋 목록 응답 (prafta-020). */
@Value
@Builder
public class PresetListResponse {
    List<PresetItemResult> presets;
}
