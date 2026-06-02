package com.prafta.web.user.user01.dto.response;

import com.prafta.web.user.user01.result.MyProfileResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyProfileResponse {
	private String userId;
	private String userNm;
	private String siteNm;
	private String nodeNm;
	private String mblNo;
	private String email;
	private String lastLoginDtime;

	public static MyProfileResponse from(MyProfileResult result) {
		return MyProfileResponse.builder()
				.userId(result.userId())
				.userNm(result.userNm())
				.siteNm(result.siteNm())
				.nodeNm(result.nodeNm())
				.mblNo(result.mblNo())
				.email(result.email())
				.lastLoginDtime(result.lastLoginDtime())
				.build();
	}
}
