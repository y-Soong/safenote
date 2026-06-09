package com.prafta.app.tbm.admin.application.command;

/**
 * prafta-051 R-C(#D-RE2 = 물리삭제) 입실취소 DELETE 커맨드.
 *
 * <p>교육준비(OPENED) 단계의 입실취소는 TB_TBM_ATTENDANCE 행을 물리 DELETE 한다(사용자 leaveBefore
 * deleteAttendance 동형). 소프트삭제(DEL_YN='Y')가 아니므로 UK(CMPNY,SESSION,USER_TYPE,USER) 잔여행이
 * 남지 않아 재입실(본인/대리) 시 충돌 없이 신규 INSERT 된다.
 * 멱등 가드: 본 세션+attendanceCd+토큰 CMPNY + 미종료(EXIT_AT IS NULL) + DEL_YN='N' + 세션 OPENED(서브쿼리).
 */
public record AdminCancelEntryCommand(
    String sessionCd
    , String attendanceCd
    , String gvCmpnyCd
){
    public static AdminCancelEntryCommand of(String sessionCd, String attendanceCd, String gvCmpnyCd) {
        return new AdminCancelEntryCommand(sessionCd, attendanceCd, gvCmpnyCd);
    }
}
