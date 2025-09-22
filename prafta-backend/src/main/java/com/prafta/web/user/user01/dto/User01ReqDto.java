package com.prafta.web.user.user01.dto;

import com.prafta.common.dto.cmmReqDto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User01ReqDto extends cmmReqDto{
	private String sr_cmpnyCd;
	private String sr_userId;
	private String sr_userNm;
	private String sr_useYn;
	private String sr_siteCd;
	
//	@Builder
//	public User01ReqDto(String cmpnyCd, String userId, String userNm, String useYn, String siteCd) {
//		this.cmpnyCd = cmpnyCd;
//		this.userId = userId;
//		this.userNm = userNm;
//		this.useYn = useYn;
//		this.siteCd = siteCd;
//	}
}
