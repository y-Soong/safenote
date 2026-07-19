package com.prafta.web.subcon.subcon01.dto.response;

import java.util.List;

import com.prafta.web.subcon.subcon01.result.TerminationImpactItem;

import lombok.Builder;
import lombok.Value;

/**
 * 해지 영향 요약 응답(해지 확인 팝업용 — T1 시점 impacts=[]).
 */
@Value
@Builder
public class TerminateSummaryResponse {
    List<TerminationImpactItem> impacts;
}
