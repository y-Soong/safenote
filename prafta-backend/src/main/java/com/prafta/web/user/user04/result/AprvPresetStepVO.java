package com.prafta.web.user.user04.result;

/**
 * 결재라인 프리셋 디테일(스텝) 1행 + 결재자 표시정보 (prafta-020, tb_aprv_line_preset_d JOIN tb_user).
 */
public record AprvPresetStepVO(
      String presetId
    , Integer stepNo
    , String approverUserCd
    , String userNm
    , String userId
) {
}
