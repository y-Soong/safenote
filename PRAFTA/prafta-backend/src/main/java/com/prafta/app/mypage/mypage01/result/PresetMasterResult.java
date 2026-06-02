package com.prafta.app.mypage.mypage01.result;

/**
 * 결재선 프리셋 마스터 1행 (prafta-app-010, tb_aprv_line_preset).
 *
 * <p>D2: web user04 의 AprvPresetVO 와 동일 형태이나 앱 전용으로 신규 작성한다.
 */
public record PresetMasterResult(
      String presetId
    , String presetNm
    , String defaultYn
) {
}
