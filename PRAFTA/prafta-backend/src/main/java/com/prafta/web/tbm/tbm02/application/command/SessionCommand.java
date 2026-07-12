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
	, String statusCd			// 개설(saveSession)은 DRAFT 고정(prafta-051 C2)
	, String entryPwd			// 개설 시 항상 null. 입실비번은 교육준비(prepare) 전이 시 발급
	, String exitPwd			// 개설 시 항상 null. 종료비번은 교육종료(complete) 전이 시 발급
	, String managerUserCd
	, String managerGpsLat
	, String managerGpsLon
	, String gpsVerifyTypeCd
	, Integer gpsVerifyRadiusM
	, Integer eduMinutes		// 교육 인정시간(분, 1~60). NULL 허용
	, String gpsManualConfirmYn
	, boolean opened			// 개설은 항상 false(OPENED_AT/PREP_START_AT은 prepare 전이에서 설정)
	, String gvCmpnyCd
	, String gvUserCd
){
	/**
	 * 개설(INSERT) 커맨드. prafta-051 C2: 개설 결과는 항상 DRAFT.
	 *
	 * <p>비밀번호/GPS 좌표 수집은 교육준비(prepare) 전이로 이동했으므로 개설 시점에는
	 * 비번 null·OPENED_AT 미설정(opened=false)으로 일관 저장한다. 단, GPS 검증 설정값
	 * (타입/반경/수동확인)은 DRAFT 단계에서 미리 지정 가능하므로 보존한다.
	 */
	public static SessionCommand forSave(SessionSaveParam param, String sessionCd) {

		return new SessionCommand(
			sessionCd
			, normalize(param.siteCd())
			, trim(param.title())
			, param.contentBody()
			, "DRAFT"
			, null
			, null
			, param.gvUserCd()
			, normalize(param.managerGpsLat())
			, normalize(param.managerGpsLon())
			, param.gpsVerifyTypeCd()
			, param.gpsVerifyRadiusM()
			, param.eduMinutes()
			, normalizeYn(param.gpsManualConfirmYn())
			, false
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
			, param.eduMinutes()
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
