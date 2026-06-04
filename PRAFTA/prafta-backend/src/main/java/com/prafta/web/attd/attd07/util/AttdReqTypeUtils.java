package com.prafta.web.attd.attd07.util;

/**
 * TB_USER_ATTD_REQ 의 REQ_TYPE / REQ_STATUS 코드값에 대한 상수 및 판정 메서드.
 *
 * <p>REQ_TYPE 은 TB_SYST_VAL_D 의 SYST_VAL_CD 'SYS032' 기준 코드값으로 관리된다
 * (PRAFTA-010-2): 01 근태생성, 02 근태수정, 03 초과근무생성, 04 초과근무수정,
 * 05 연차사용, 06 연차수정. 현재 코드는 01/02/03 만 매핑하며, 일부 값만 처리하는
 * endpoint 는 나머지를 fail-closed 로 거부해야 한다. 그렇지 않으면, 예를 들어 비근태
 * REQ row 가 근태 수정으로 무성하게 승인되는 타입 혼동(type confusion) 공격에
 * 노출될 수 있다.
 *
 * <p>REQ_STATUS 는 TB_SYST_VAL_D 의 SYST_VAL_CD 'SYS033' 기준 코드값으로 관리된다
 * (PRAFTA-010-2): 01 신청, 02 승인, 03 반려, 04 취소.
 *
 * <p>원래 {@code Attd07ServiceImpl}에서 추출됨 (PRAFTA-003-1).
 */
public final class AttdReqTypeUtils {

    // REQ_TYPE 코드값 (SYS032)
    public static final String REQ_TYPE_ATTD_CREATE   = "01";
    public static final String REQ_TYPE_ATTD_MODIFY   = "02";
    public static final String REQ_TYPE_OT_REGISTER   = "03";
    public static final String REQ_TYPE_OT_MODIFY     = "04";
    /**
     * 스케줄 수정 요청 (PRAFTA-APP-007 신규).
     *
     * <p>SYS032=10 마이그레이션 적용 전 단계에서도 본 상수의 값(="10") 자체는 사용 가능하다.
     * 마이그레이션 미적용 운영 DB 에서는 INSERT 시 FK/CHECK 가 없어 컬럼 그대로 저장된다.
     * 운영 적용 (prafta-app-007-attd-req-extensions.sql) 후 비로소 의미를 가진다.
     */
    public static final String REQ_TYPE_SCHED_MODIFY  = "10";

    // REQ_STATUS 코드값 (SYS033)
    public static final String REQ_STATUS_REQUESTED   = "01";
    public static final String REQ_STATUS_APPROVED    = "02";
    public static final String REQ_STATUS_REJECTED    = "03";
    public static final String REQ_STATUS_CANCELLED   = "04";

    private AttdReqTypeUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * SEC-018 - 근태 수정 endpoint에서 사용하는 REQ_TYPE allow-list.
     * 근태 생성('01')/근태 수정('02') 값에 대해서만 true를 반환한다. OT, LEAVE 값
     * (그리고 알 수 없는 값)은 false를 반환한다.
     */
    public static boolean isAttendanceReqType(String reqType) {
        if (reqType == null || reqType.isEmpty()) return false;
        return REQ_TYPE_ATTD_MODIFY.equals(reqType)
            || REQ_TYPE_ATTD_CREATE.equals(reqType);
    }

    /**
     * SEC-018 - 초과근무 처리 endpoint에서 사용하는 REQ_TYPE allow-list.
     * 초과근무 생성('03')/수정('04') 값에 대해서만 true를 반환한다. 근태(ATTD)/연차(LEAVE)
     * 값(그리고 알 수 없는 값)은 false를 반환하여 타입 혼동을 차단한다.
     * (PRAFTA-025: 초과근무 수정('04') 추가. 승인 분기는 03=INSERT / 04=UPDATE 로 명시 구분한다.)
     */
    public static boolean isOvertimeReqType(String reqType) {
        if (reqType == null || reqType.isEmpty()) return false;
        return REQ_TYPE_OT_REGISTER.equals(reqType)
            || REQ_TYPE_OT_MODIFY.equals(reqType);
    }

    /**
     * 초과근무 수정('04') 여부. 승인 endpoint에서 기존 OT 행 UPDATE 분기 판정에 사용한다.
     */
    public static boolean isOvertimeModify(String reqType) {
        return REQ_TYPE_OT_MODIFY.equals(reqType);
    }

    /**
     * SEC-018 - 스케줄 수정 처리 endpoint에서 사용하는 REQ_TYPE allow-list (PRAFTA-APP-007).
     * 스케줄 수정 요청('10') 값에 대해서만 true 를 반환한다. 근태(01/02)/OT(03/04)/연차(05/06)
     * 값(그리고 알 수 없는 값)은 false 를 반환하여 타입 혼동(type confusion)을 차단한다.
     * 스케줄 수정 승인은 tb_user_work_plan 의 WORK_PLAN_CD 를 교체하므로, 다른 유형의
     * REQ row 가 이 경로로 흘러들면 잘못된 스케줄 반영이 발생할 수 있다.
     */
    public static boolean isScheduleModifyReqType(String reqType) {
        if (reqType == null || reqType.isEmpty()) return false;
        return REQ_TYPE_SCHED_MODIFY.equals(reqType);
    }
}
