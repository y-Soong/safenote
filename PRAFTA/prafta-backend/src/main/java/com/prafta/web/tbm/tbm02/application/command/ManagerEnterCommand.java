package com.prafta.web.tbm.tbm02.application.command;

/**
 * 관리자 직접 입실 INSERT/RESTORE 커맨드(prafta-051-11 + PRAFTA-SUBCON-T5).
 *
 * <p>ENTRY_TYPE_CD='MANAGER_DIRECT', ENTRY_BY_MANAGER_USER_CD=처리 관리자(개설사 소속 gvUserCd),
 * GPS/거리/비밀번호/APP_FOREGROUND_SEC 는 모두 NULL(검색/대리입실은 위치·앱지표 없음).
 *
 * <p><b>T5 핵심</b>: 출결행의 CMPNY_CD 와 ATTENDANCE_CD 채번 대상은 <b>참석자 회사
 * ({@code targetCmpnyCd})</b>다(관리자 회사가 아니다). 타사 참석행을 관리자 회사로 오기록하면
 * UK(CMPNY,SESSION,USER_TYPE,USER)가 관리자 회사로 잠겨 데이터가 오염된다.
 *
 * <p>UNIQUE(CMPNY,SESSION,USER_TYPE,USER) 는 DEL_YN 을 포함하지 않으므로, 내보내기(eject)로
 * DEL_YN='Y' 된 행이 슬롯을 점유한다. 따라서 재입실은 INSERT 대신 해당 행 RESTORE(DEL_YN='N'
 * 복구 + MANAGER_DIRECT 재기록)로 처리해 UNIQUE 충돌과 중복 행 누적을 막는다.
 */
public record ManagerEnterCommand(
	String targetCmpnyCd
	, String sessionCd
	, String userTypeCd
	, String userCd
	, String managerUserCd
){
}
