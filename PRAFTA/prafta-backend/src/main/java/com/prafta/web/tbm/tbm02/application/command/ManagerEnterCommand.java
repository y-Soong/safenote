package com.prafta.web.tbm.tbm02.application.command;

import com.prafta.web.tbm.tbm02.application.param.ManagerEnterParam;

/**
 * 관리자 직접 입실 INSERT/RESTORE 커맨드(prafta-051-11).
 *
 * <p>ENTRY_TYPE_CD='MANAGER_DIRECT', ENTRY_BY_MANAGER_USER_CD=처리 관리자(gvUserCd),
 * GPS/거리/비밀번호/APP_FOREGROUND_SEC 는 모두 NULL(검색/대리입실은 위치·앱지표 없음).
 *
 * <p>UNIQUE(CMPNY,SESSION,USER_TYPE,USER) 는 DEL_YN 을 포함하지 않으므로, 내보내기(eject)로
 * DEL_YN='Y' 된 행이 슬롯을 점유한다. 따라서 재입실은 INSERT 대신 해당 행 RESTORE(DEL_YN='N'
 * 복구 + MANAGER_DIRECT 재기록)로 처리해 UNIQUE 충돌과 중복 행 누적을 막는다.
 */
public record ManagerEnterCommand(
	String gvCmpnyCd
	, String sessionCd
	, String userTypeCd
	, String userCd
	, String managerUserCd
){
	public static ManagerEnterCommand of(ManagerEnterParam param) {
		return new ManagerEnterCommand(
			param.gvCmpnyCd()
			, param.sessionCd()
			, param.userTypeCd()
			, param.userCd()
			, param.gvUserCd()
		);
	}
}
