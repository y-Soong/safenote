package com.prafta.app.mypage.mypage01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-010-05: 결재선 프리셋 목록 응답.
 */
@Getter
@Builder
public class PresetListResponse {

    private final List<PresetItemResult> presets;
}
