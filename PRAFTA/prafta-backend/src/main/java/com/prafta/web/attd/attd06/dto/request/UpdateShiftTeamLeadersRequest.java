package com.prafta.web.attd.attd06.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateShiftTeamLeadersRequest {
    private String siteCd;
    private String userCd;
    private String leaderYn;
}
