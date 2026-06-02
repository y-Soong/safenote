package com.prafta.app.mypage.mypage01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-010-05: 프리셋 스텝(결재자) 1건 응답 항목.
 */
@Getter
@Builder
public class PresetStepItem {

    private final int stepNo;
    private final String approverUserCd;
    private final String userNm;
    private final String userId;
    private final String rankNm;
    private final String nodeNm;
}
