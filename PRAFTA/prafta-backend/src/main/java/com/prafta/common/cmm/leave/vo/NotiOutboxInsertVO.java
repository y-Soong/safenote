package com.prafta.common.cmm.leave.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code tb_noti_outbox} 알림 outbox INSERT 1건의 운반체(PRAFTA-031).
 *
 * <p>참조: 공통 정책서 §10(알림/공지). 본 작업(연차 회수)에서는 회수 완료 시
 * {@code SEND_STATUS='PENDING'} 1행을 적재한다(발송은 추후 모바일 push).
 *
 * <p>중복 발송 방지(§10.3): {@code dedupKey} + UNIQUE(CMPNY_CD, DEDUP_KEY).
 * 회수 이벤트는 {@code "RECALL_" + grantId + "_" + notiId} 키로 회수 이벤트마다 1건씩 적재된다
 * (N-1, 2026-08-21 — grantId 단독 키였던 종전 구성은 재활성화 후 재회수 시 UNIQUE 충돌을 유발했다).
 */
@Getter
@Setter
public class NotiOutboxInsertVO {

    /** 알림 ID (PK, varchar(20)) — 서비스에서 채번하여 세팅 */
    private String notiId;

    /** 회사 코드 */
    private String cmpnyCd;

    /** 사업장 코드 (없으면 NULL) */
    private String siteCd;

    /** 수신 대상 사용자 코드 */
    private String targetUserCd;

    /** 알림 유형 [SYS045] (예: LEAVE_GRANT_RECALLED) */
    private String notiType;

    /** 발송 채널 (PUSH) */
    private String channel;

    /** 알림 제목 */
    private String title;

    /** 알림 본문 */
    private String body;

    /** 추가 데이터 페이로드 (JSON 문자열) */
    private String dataPayload;

    /** 발송 상태 (PENDING) */
    private String sendStatus;

    /** 중복 발송 방지 키 */
    private String dedupKey;

    /** 등록자 */
    private String insertNo;
}
