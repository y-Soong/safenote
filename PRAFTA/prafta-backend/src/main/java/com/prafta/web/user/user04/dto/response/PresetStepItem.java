package com.prafta.web.user.user04.dto.response;

import lombok.Builder;
import lombok.Value;

/** 프리셋 1스텝(결재자) 응답 항목 (prafta-020). */
@Value
@Builder
public class PresetStepItem {
    Integer stepNo;
    String approverUserCd;
    String userNm;
    String userId;
}
