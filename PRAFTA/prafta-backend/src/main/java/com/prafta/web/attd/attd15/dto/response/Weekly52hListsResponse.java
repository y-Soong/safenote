package com.prafta.web.attd.attd15.dto.response;

import java.util.List;

import com.prafta.web.attd.attd15.result.Weekly52hListResult;

import lombok.Builder;
import lombok.Value;

/**
 * ATTD15-T1 - 주52시간 관리 조회 응답.
 * 프론트(Attd_15.vue)는 weekly52hListsResultList 를 행으로 바인딩한다.
 */
@Value
@Builder
public class Weekly52hListsResponse {
    List<Weekly52hListResult> weekly52hListsResultList;
}
