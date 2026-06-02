package com.prafta.app.mypage.mypage01.result;

/**
 * 결재선 프리셋 디테일(스텝) 1행 + 결재자 표시정보
 * (prafta-app-010, tb_aprv_line_preset_d JOIN tb_user).
 */
public record PresetStepResult(
      String presetId
    , Integer stepNo
    , String approverUserCd
    , String userNm
    , String userId
    , String rankNm
    , String nodeNm
) {
}
