package com.prafta.web.attd.attd08.dto.response;

import java.util.List;

import com.prafta.web.attd.attd08.result.AttdListsResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AttdListsResponse {
    List<AttdListsResult> attdListsResultList;
}
