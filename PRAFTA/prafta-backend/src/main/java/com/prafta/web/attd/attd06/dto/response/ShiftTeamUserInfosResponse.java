package com.prafta.web.attd.attd06.dto.response;

import java.util.List;
import com.prafta.web.attd.attd06.result.ShiftTeamUserInfosResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShiftTeamUserInfosResponse {
    List<ShiftTeamUserInfosResult> shiftTeamUserInfosResultList;
}
