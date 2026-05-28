package com.prafta.web.tbm.tbm02.application.command;

import com.prafta.web.tbm.tbm02.application.param.SessionSaveParam;
import com.prafta.web.tbm.tbm02.application.param.SessionUpdateParam;

/**
 * TB_TBM_SESSION INSERT/UPDATE 커맨드.
 *
 * <p>상태/비밀번호/개설시각은 서비스가 saveMode에 따라 결정해 채운다(서버 권위).
 */
public record SessionCommand(
	String sessionCd
	, String siteCd
	, String title
	, String contentBody
	, String statusCd			// DRAFT / OPENED
	, String entryPwd			// OPENED 시에만 값(서버 생성), DRAFT 시 null
	, String exitPwd
	, String managerUserCd
	, String managerGpsLat
	, String managerGpsLon
	, String gpsVerifyTypeCd
	, Integer gpsVerifyRadiusM
	, String gpsManualConfirmYn
	, boolean opened			// true=OPENED_AT=NOW() 설정
	, String gvCmpnyCd
	, String gvUserCd
){
	/** 개설/임시저장(INSERT) 커맨드. */
	public static SessionCommand forSave(
			SessionSaveParam param, String sessionCd, String statusCd,
			String entryPwd, String exitPwd, boolean opened) {

		return new SessionCommand(
			sessionCd
			, normalize(param.siteCd())
			, trim(param.title())
			, param.contentBody()
			, statusCd
			, entryPwd
			, exitPwd
			, param.gvUserCd()
			, normalize(param.managerGpsLat())
			, normalize(param.managerGpsLon())
			, param.gpsVerifyTypeCd()
			, param.gpsVerifyRadiusM()
			, normalizeYn(param.gpsManualConfirmYn())
			, opened
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}

	/** 수정(UPDATE) 커맨드. 비밀번호/상태는 변경하지 않는다. */
	public static SessionCommand forUpdate(SessionUpdateParam param) {

		return new SessionCommand(
			param.sessionCd()
			, null				// 사업장은 수정 대상 아님(보존)
			, trim(param.title())
			, param.contentBody()
			, null				// 상태 변경 없음
			, null
			, null
			, param.gvUserCd()
			, normalize(param.managerGpsLat())
			, normalize(param.managerGpsLon())
			, param.gpsVerifyTypeCd()
			, param.gpsVerifyRadiusM()
			, normalizeYn(param.gpsManualConfirmYn())
			, false
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
