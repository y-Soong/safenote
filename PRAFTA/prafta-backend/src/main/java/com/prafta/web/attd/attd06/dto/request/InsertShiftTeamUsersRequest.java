package com.prafta.web.attd.attd06.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InsertShiftTeamUsersRequest {
    private String siteCd;
    private String shiftCd;
    private String shiftTeamId;
    private String teamIdx;
    private String userCd;
}
