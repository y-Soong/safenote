package com.prafta.web.attd.attd08.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AttdListsRequest {
    private String fromDate;       // YYYY-MM-DD
    private String toDate;         // YYYY-MM-DD
    private String siteCd;         // required
    private String nodeCd;         // optional
    private String incSubNodeYn;   // Y/N (default N)
    private String userNm;         // partial match (nullable)
}
