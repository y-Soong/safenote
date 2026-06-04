package com.prafta.common.cmm.leave.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code tb_leave_refusal_log} 사실 기록 1건 INSERT 운반체 (PRAFTA-COM-001).
 *
 * <p>3개 이벤트(NOTICED / CHECKIN_DETECTED / ADMIN_ALERTED)를 공용으로 적재한다.
 * 출근 원본(tb_user_attd_mgmt)은 절대 수정하지 않으며 본 테이블은 관찰/통지 사실만 남긴다.
 *
 * <p>멱등성: {@code dedupKey} + UNIQUE(CMPNY_CD, DEDUP_KEY). 재호출 시
 * {@code INSERT ... ON DUPLICATE KEY UPDATE UPDATE_DATE=NOW()} 로 1건만 유지한다.
 */
@Getter
@Setter
public class RefusalLogInsertVO {

    /** 로그 ID (PK, varchar(20)) — 서비스에서 채번하여 세팅 */
    private String refusalId;

    /** 회사 코드 */
    private String cmpnyCd;

    /** 사업장 코드 */
    private String siteCd;

    /** 대상 근로자 코드 */
    private String userCd;

    /** 노무수령거부 대상일 (YYYYMMDD, =연차촉진 사용지정일) */
    private String targetYmd;

    /** 이벤트 유형 [SYS064] NOTICED / CHECKIN_DETECTED / ADMIN_ALERTED */
    private String eventType;

    /** 연관 알림 ID (tb_noti_outbox.NOTI_ID, NOTICED/ADMIN_ALERTED 시) */
    private String relatedNotiId;

    /** 연관 근태 ID (tb_user_attd_mgmt.ATTD_ID, CHECKIN_DETECTED 시) */
    private String relatedAttdId;

    /** 출근 감지 일시 세팅 여부 (CHECKIN_DETECTED 시 true → NOW() 기록) */
    private boolean detectNow;

    /** 중복 방지 키 ({CMPNY_CD}_{USER_CD}_{TARGET_YMD}_{EVENT_TYPE} 등) */
    private String dedupKey;

    /** 등록자 (관리자 USER_CD or SYSTEM) */
    private String insertNo;
}
