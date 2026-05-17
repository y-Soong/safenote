package com.prafta.web.attd.attd08.dto.response;

import java.util.List;

import com.prafta.web.attd.attd08.result.AttdGpsTrailResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AttdGpsTrailResponse {
    List<AttdGpsTrailResult> attdGpsTrailResultList;
}
