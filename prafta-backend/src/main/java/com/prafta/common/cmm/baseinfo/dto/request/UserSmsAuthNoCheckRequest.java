package com.prafta.common.cmm.baseinfo.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserSmsAuthNoCheckRequest {
	private String cmpnyCd;
    private String mblNo;
    private String certNo;
}
