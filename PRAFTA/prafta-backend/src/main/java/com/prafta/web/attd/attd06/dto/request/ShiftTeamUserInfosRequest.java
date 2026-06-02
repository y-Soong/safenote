package com.prafta.web.attd.attd06.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShiftTeamUserInfosRequest {
    private String siteCd;
    private String nodeCd;
    private String incSubNodeYn;
    private String shiftCd;
}
