package com.prafta.app.leave.leaveflow.dto.response;

import java.util.List;

/**
 * prafta-app-018-A: 본인 결재선 프리셋 목록 응답 (GET /appApi/leaveflow/approval-presets).
 *
 * <p>mypage01 매퍼(selectPresetMasters/selectPresetStepsByUser) 결과를 서비스에서 presetId 기준 그룹핑하여 조립.
 *   키명은 018-C(FE)가 그대로 소비하므로 임의 변경 금지.
 */
public record ApprovalPresetListResponse(
      List<PresetItem> presets
) {
    /** 프리셋 마스터 1건 + 스텝 목록. */
    public record PresetItem(
          String presetId
        , String presetNm
        , boolean defaultYn
        , List<PresetStepItem> steps
    ) {
    }

    /** 프리셋 스텝(결재자) 1건. PII 최소노출(휴대폰/이메일 미포함). */
    public record PresetStepItem(
          int stepNo
        , String approverUserCd
        , String userNm
        , String userId
        , String rankNm
        , String nodeNm
    ) {
    }
}
