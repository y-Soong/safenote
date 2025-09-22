package com.prafta.common.cmm.login.dto;

import com.prafta.common.dto.cmmReqDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginReqDto extends cmmReqDto{
	private String userId;
    private String userNm;
    private String userPw;
}
