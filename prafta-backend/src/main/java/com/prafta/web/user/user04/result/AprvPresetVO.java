package com.prafta.web.user.user04.result;

/**
 * 결재라인 프리셋 마스터 1행 (prafta-020, tb_aprv_line_preset).
 */
public record AprvPresetVO(
      String presetId
    , String presetNm
    , String defaultYn
) {
}
