package com.prafta.app.tbm.admin.application.command;

import com.prafta.app.tbm.admin.application.param.AdminSessionSaveParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionUpdateParam;

/**
 * TB_TBM_SESSION INSERT/UPDATE 커맨드(app 관리자 포팅).
 *
 * <p>상태/비밀번호/개설시각은 서비스가 saveMode에 따라 결정해 채운다(서버 권위).
 * CONTENT_FORMAT_CD 는 기존 기본값(RICH_HTML)을 유지하여 web 표시 호환을 보장한다(T5).
 */
public record AdminSessionCommand(
    String sessionCd
    , String siteCd
    , String title
    , String contentBody
    , String statusCd           // DRAFT / OPENED
    , String entryPwd           // OPENED 시에만 값(서버 생성), DRAFT 시 null
    , String exitPwd
    , String managerUserCd
    , String managerGpsLat
    , String managerGpsLon
    , String gpsVerifyTypeCd
    , Integer gpsVerifyRadiusM
    , Integer eduMinutes        // 교육 인정시간(분, 1~60). NULL 허용
    , String gpsManualConfirmYn
    , boolean opened            // true=OPENED_AT=NOW() 설정
    , String gvCmpnyCd
    , String gvUserCd
){
    /** 개설/임시저장(INSERT) 커맨드. */
    public static AdminSessionCommand forSave(
            AdminSessionSaveParam param, String sessionCd, String statusCd,
            String entryPwd, String exitPwd, boolean opened) {

        return new AdminSessionCommand(
            sessionCd
            , normalize(param.siteCd())
            , trim(param.title())
            , param.contentBody()
            , statusCd
            , entryPwd
            , exitPwd
            , param.gvUserCd()
            // [QA Low] 개설(DRAFT)은 관리자 GPS 좌표 미발급. 조작된 요청이 개설 시점에 좌표를 심지 못하도록
            // 강제 null. 좌표는 교육준비(prepare) 전이에서만 수집·저장한다(검증유형·반경은 개설 설정값으로 유지).
            , null
            , null
            , param.gpsVerifyTypeCd()
            , param.gpsVerifyRadiusM()
            , param.eduMinutes()
            , normalizeYn(param.gpsManualConfirmYn())
            , opened
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }

    /** 수정(UPDATE) 커맨드. 비밀번호/상태는 변경하지 않는다. */
    public static AdminSessionCommand forUpdate(AdminSessionUpdateParam param) {

        return new AdminSessionCommand(
            param.sessionCd()
            , null              // 사업장은 수정 대상 아님(보존)
            , trim(param.title())
            , param.contentBody()
            , null              // 상태 변경 없음
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
