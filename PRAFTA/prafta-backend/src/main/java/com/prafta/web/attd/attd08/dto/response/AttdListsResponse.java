package com.prafta.web.attd.attd08.dto.response;

import java.util.List;

import com.prafta.web.attd.attd08.result.AttdListsResult;
import com.prafta.web.attd.attd08.result.HalfLeaveWindowResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AttdListsResponse {
    List<AttdListsResult> attdListsResultList;

    /*
     * A안(2026-08-17): 확정 "시각 보유" 연차(반차/시간차) 구간 목록 (조회기간 단위).
     * FE 가 실근로/인정시간 표시에서 실근태와의 겹침을 차감한다(연차 시간은 근로시간 미산입).
     * additive — 구 FE 는 무시(무회귀).
     */
    List<HalfLeaveWindowResult> timeLeaveWindowList;
}
