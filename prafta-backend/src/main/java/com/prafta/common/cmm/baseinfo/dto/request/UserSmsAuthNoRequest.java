package com.prafta.common.cmm.baseinfo.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserSmsAuthNoRequest {
	private String cmpnyCd;
    private String mblNo;
    private String dupChkYn;		/* 휴대폰번호 중복 체크 필요 여부 Y/N */
}
