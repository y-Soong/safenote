package com.prafta.common.cmm.push.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * FCM 전송 워커 claim 대상 outbox 1행 (PRAFTA-COM-002).
 *
 * <p>{@code tb_noti_outbox} 의 PENDING 행 중 발송에 필요한 컬럼만 운반한다(SELECT * 금지).
 * {@code DATA_PAYLOAD} 는 json 컬럼이지만 String 으로 받아 서비스에서 Jackson 으로
 * {@code Map<String,String>} 파싱한다(null/빈값이면 data 없이 notification 만 전송).
 */
@Getter
@Setter
public class PushOutboxRowVO {

    /** 알림 ID (PK) */
    private String notiId;

    /** 회사 코드 (CMPNY_CD 스코프) */
    private String cmpnyCd;

    /** 수신 대상 사용자 코드 (= tb_user_device.USER_CD 조회 키) */
    private String targetUserCd;

    /** 알림 유형[SYS045] */
    private String notiType;

    /** 알림 제목 (FCM notification.title) */
    private String title;

    /** 알림 본문 (FCM notification.body) */
    private String body;

    /** 추가 데이터 페이로드(json 원문 문자열, nullable → FCM data) */
    private String dataPayload;

    /** 현재 재시도 횟수 (전송 실패 시 +1 누적, maxRetry 도달 시 FAILED) */
    private int retryCnt;
}
