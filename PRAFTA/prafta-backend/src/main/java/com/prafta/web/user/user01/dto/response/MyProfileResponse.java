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
	// F-8-2: 현재 기본 근무타입 표시용(미설정이면 전부 null — F-8-3 화면이 "현재값" 표시에 사용).
	private String defaultSchCd;
	private String defaultSchNo;
	private String defaultSchStrTime;
	private String defaultSchEndTime;

	public static MyProfileResponse from(MyProfileResult result) {
		return MyProfileResponse.builder()
				.userId(result.userId())
				.userNm(result.userNm())
				.siteNm(result.siteNm())
				.nodeNm(result.nodeNm())
				.mblNo(result.mblNo())
				.email(result.email())
				.lastLoginDtime(result.lastLoginDtime())
				.defaultSchCd(result.defaultSchCd())
				.defaultSchNo(result.defaultSchNo())
				.defaultSchStrTime(result.defaultSchStrTime())
				.defaultSchEndTime(result.defaultSchEndTime())
				.build();
	}
}
