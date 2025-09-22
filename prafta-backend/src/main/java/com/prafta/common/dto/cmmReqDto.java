package com.prafta.common.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class cmmReqDto {
	private String gv_cmpnyCd;
	private String gv_userId;
	private String gv_siteCd;
	private String gv_authCd;
	private String gv_email;
	private String gv_mblNo;
	@Override
	public String toString() {
		return "cmmReqDto [gv_cmpnyCd=" + gv_cmpnyCd + ", gv_userId=" + gv_userId + ", gv_siteCd=" + gv_siteCd
				+ ", gv_authCd=" + gv_authCd + ", gv_email=" + gv_email + ", gv_mblNo=" + gv_mblNo + "]";
	}
}
