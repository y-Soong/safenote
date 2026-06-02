package com.prafta.web.baim.baim05.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InsertDailyQrUserRequest {
    private String siteCd;
    private String userNm;
    private String mblNo;
    private String slotNo;
}
