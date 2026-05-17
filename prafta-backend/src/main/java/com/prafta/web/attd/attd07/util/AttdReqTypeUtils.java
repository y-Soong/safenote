package com.prafta.web.attd.attd07.util;

/**
 * SYS032 REQ_TYPE 값에 대한 판정 메서드.
 *
 * <p>REQ_TYPE은 prafta-003-ot-001.sql 3-2 섹션에서 enum 형태의 값
 * { ATTD_MODIFY, ATTD_CREATE, OT_REGISTER, LEAVE_REQUEST }로 마이그레이션되었다.
 * 일부 값만 처리하는 endpoint는 나머지를 fail-closed로 거부해야 한다.
 * 그렇지 않으면, 예를 들어 비근태 REQ row가 근태 수정으로 무성하게 승인되는
 * 타입 혼동(type confusion) 공격에 노출될 수 있다.
 *
 * <p>원래 {@code Attd07ServiceImpl}에서 추출됨 (PRAFTA-003-1).
 */
public final class AttdReqTypeUtils {

    public static final String REQ_TYPE_ATTD_MODIFY   = "ATTD_MODIFY";
    public static final String REQ_TYPE_ATTD_CREATE   = "ATTD_CREATE";
    public static final String REQ_TYPE_OT_REGISTER   = "OT_REGISTER";

    private AttdReqTypeUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * SEC-018 - 근태 수정 endpoint에서 사용하는 REQ_TYPE allow-list.
     * 두 가지 근태 변형 값에 대해서만 true를 반환한다. OT, LEAVE 값
     * (그리고 알 수 없는 값)은 false를 반환한다.
     */
    public static boolean isAttendanceReqType(String reqType) {
        if (reqType == null || reqType.isEmpty()) return false;
        return REQ_TYPE_ATTD_MODIFY.equals(reqType)
            || REQ_TYPE_ATTD_CREATE.equals(reqType);
    }

    /**
     * SEC-018 - 초과근무 처리 endpoint에서 사용하는 REQ_TYPE allow-list.
     * OT_REGISTER 값에 대해서만 true를 반환한다. 근태(ATTD)/연차(LEAVE) 값
     * (그리고 알 수 없는 값)은 false를 반환하여 타입 혼동을 차단한다.
     */
    public static boolean isOvertimeReqType(String reqType) {
        if (reqType == null || reqType.isEmpty()) return false;
        return REQ_TYPE_OT_REGISTER.equals(reqType);
    }
}
