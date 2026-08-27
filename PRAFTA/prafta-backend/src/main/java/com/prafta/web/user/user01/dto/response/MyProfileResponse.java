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
	// PRAFTA-001(기본근무타입-승인제, 2026-08-27): 대기중 신청 요약(MyInfoPop 배너용, 없으면 전부 null).
	//   신규 조회 API 없이 이 필드만으로 배너를 그린다(§조사 근거).
	private String pendingDefaultSchReqId;
	private String pendingDefaultSchCd;
	private String pendingDefaultSchNo;
	private String pendingDefaultSchReqDate;

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
				.pendingDefaultSchReqId(result.pendingDefaultSchReqId())
				.pendingDefaultSchCd(result.pendingDefaultSchCd())
				.pendingDefaultSchNo(result.pendingDefaultSchNo())
				.pendingDefaultSchReqDate(result.pendingDefaultSchReqDate())
				.build();
	}
}
