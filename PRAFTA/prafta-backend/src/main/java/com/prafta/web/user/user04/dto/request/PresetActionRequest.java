package com.prafta.web.user.user04.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 결재라인 프리셋 단건 액션(삭제/기본지정) 요청 body (prafta-020).
 * POST /user04/presets/delete, /user04/presets/set-default.
 */
@Getter
@Setter
@NoArgsConstructor
public class PresetActionRequest {

    @NotBlank
    @Size(max = 20)
    private String presetId;
}
