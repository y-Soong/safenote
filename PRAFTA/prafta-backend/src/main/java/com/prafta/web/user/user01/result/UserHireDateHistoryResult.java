package com.prafta.web.user.user01.result;

/**
 * 입사일 변경 이력 조회 결과 (TB_USER_HIRE_DATE_HISTORY 1행).
 *
 * <p>변경자명(changerNm)은 INSERT_NO(=변경자 USER_CD)를 TB_USER에 조인해 얻은 PII 평문이며,
 * 관리자(master/hr) 전용 화면에서만 노출한다. appliedDate/changedAt은 매퍼에서
 * 'yyyy-MM-dd HH:mm' 으로 포맷해 내려준다(미적용 시 appliedDate는 null).
 */
public record UserHireDateHistoryResult(
    String histId
    , String prevHireDate     /** 변경 전 입사일 (YYYYMMDD) */
    , String newHireDate      /** 변경 후 입사일 (YYYYMMDD) */
    , String changeReason     /** 변경 사유 */
    , String handlingType     /** 처리 방식[SYS039] */
    , String appliedYn        /** 정책 기준 부여 적용 완료 여부 (Y/N) */
    , String appliedDate      /** 적용 일시 (yyyy-MM-dd HH:mm, nullable) */
    , String changerNm        /** 변경자명 (INSERT_NO 조인, PII 평문) */
    , String changedAt        /** 변경 일시 (INSERT_DATE, yyyy-MM-dd HH:mm) */
) {
}
