package com.prafta.web.tbm.tbm02.application.command;

import com.prafta.web.tbm.tbm02.application.param.SessionPrepareParam;

/**
 * 교육준비(OPENED) 전이 UPDATE 커맨드(prafta-051-03).
 *
 * <p>DRAFT → OPENED 전이 시 입실비번 발급 + 관리자 GPS 중심좌표 + PREP_START_AT 기준시각
 * 을 한 번에 확정한다. WHERE 절 경합 가드(STATUS_CD='DRAFT')는 매퍼에서 강제한다.
 * 종료비번(EXIT_PWD)은 이 시점에 발급하지 않는다(교육종료 전이 소관).
 */
public record SessionPrepareCommand(
	String sessionCd
	, String title
	, String contentBody
	, String entryPwd			// 입실비번(이 시점 발급)
	, String managerGpsLat
	, String managerGpsLon
	, String gpsVerifyTypeCd
	, Integer gpsVerifyRadiusM
	, String gpsManualConfirmYn
	, String gvCmpnyCd
	, String gvUserCd
){
	public static SessionPrepareCommand of(SessionPrepareParam param, String entryPwd) {

		return new SessionPrepareCommand(
			param.sessionCd()
			, trim(param.title())
			, param.contentBody()
			, entryPwd
			, normalize(param.managerGpsLat())
			, normalize(param.managerGpsLon())
			, param.gpsVerifyTypeCd()
			, param.gpsVerifyRadiusM()
			, normalizeYn(param.gpsManualConfirmYn())
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}

	private static String trim(String s) {
		return s == null ? null : s.trim();
	}

	private static String normalize(String s) {
		return (s == null || s.isEmpty()) ? null : s;
	}

	private static String normalizeYn(String s) {
		return "Y".equals(s) ? "Y" : "N";
	}
}
