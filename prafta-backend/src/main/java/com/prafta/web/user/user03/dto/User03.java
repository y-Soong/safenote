package com.prafta.web.user.user03.dto;

import com.prafta.common.dto.cmmReqDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User03 extends cmmReqDto{
	private String chk;
	private String cmpnyCd;
	private String userId;
	private String siteCd;
	private String allocYn;
	private String useYn;
	
	@Override
	public String toString() {
		return "User03 [chk=" + chk + ", cmpnyCd=" + cmpnyCd + ", userId=" + userId + ", siteCd=" + siteCd
				+ ", allocYn=" + allocYn + ", useYn=" + useYn + "]";
	}
}
