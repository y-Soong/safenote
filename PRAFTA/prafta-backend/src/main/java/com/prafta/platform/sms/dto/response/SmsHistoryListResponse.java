package com.prafta.platform.sms.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Platform_05: SMS 발송 이력 목록 응답 (발송 이력 탭).
 *
 * <p>PII 최소화 — 휴대폰은 <b>마스킹 문자열만</b> 내려간다(평문/암호문/HMAC 전부 금지, 공통 정책서 §11.1).
 *
 * <p>★★<b>절대 추가 금지 필드</b>
 * <ul>
 *   <li>{@code AUTH_CD} — 6자리 인증번호 <b>평문</b>. 만료 전·미인증 행이면 지금 당장 쓸 수 있는
 *       자격증명이라, 노출되면 타인의 휴대폰 없이 셀프가입 본인인증 · <b>비밀번호 재설정(계정 탈취)</b> ·
 *       앱 휴대폰 변경을 통과할 수 있다.</li>
 *   <li>{@code MBL_NO_HMAC} — 검색 <b>입력</b>으로만 쓰고 응답에는 담지 않는다(상관·역추적 재료).</li>
 *   <li>{@code SEND_IP_HASH} — 컬럼 주석에 "역추적 용도가 아니다" 라고 명시돼 있다.</li>
 *   <li>{@code SEND_REF_KEY} / {@code SEND_MSG_KEY} / {@code EXPIRED_AT} / {@code FAIL_LOCKED_AT}
 *       — 이번 범위 제외(필요하면 별건 협의).</li>
 * </ul>
 *
 * <p>목적/상태의 한글 라벨 매핑은 <b>화면</b>이 한다(서버는 코드값만) — 기존 Platform_05 방식과 동일.
 */
@Getter
@Builder
public class SmsHistoryListResponse {

    /** 발송 이력 목록 (요청일시 내림차순, 현재 페이지 분) */
    private final List<Row> historyList;

    /** 조건 전체 건수 (페이저 계산용) */
    private final int totalCount;

    @Getter
    @Builder
    public static class Row {

        /** 인증코드 행 ID — 행의 유일 식별자(프론트 key) */
        private final Long smsId;

        /** 요청 일시 (yyyy-MM-dd HH:mm:ss) */
        private final String insertDate;

        /** 마스킹 휴대폰 (010-****-1234). 복호 실패 시 {@code "-"} */
        private final String mblNo;

        /** 인증 목적 — SELF_JOIN / PLATFORM_LOCATION / MOBILE_CHANGE */
        private final String purposeCd;

        /** 발송 상태 — PENDING / SENT / FAILED / SKIPPED */
        private final String sendStatus;

        /** 발송 결과 확정 일시 (yyyy-MM-dd HH:mm:ss). 미발송이면 null */
        private final String sendDate;

        /**
         * 인증 통과 여부 — 'Y' 검증완료 / 'C' 소비완료 / 'N' 미검증.
         *
         * <p>★화면은 <b>'Y' 와 'C' 를 모두 "인증 완료"로 표기</b>해야 한다. 'C' 는 검증을 통과한 뒤
         * 코드가 소비된 상태라 'N'(미검증)과 의미가 정반대다. 'Y' 만 완료로 보면 실제로 인증에
         * 성공한 건이 미인증으로 뒤집혀 보인다(개발 DB 실측 Y=44 / C=8 / N=45).
         */
        private final String verifiedYn;

        /** 인증번호 검증 실패 누적 횟수. ★값이 크면 무차별 대입 시도 단서(5회 이상이면 코드 무효) */
        private final int failCnt;

        /** 발송 실패코드(벤더 원문). 없으면 null */
        private final String sendErrCd;

        /** 발송 실패사유(벤더 원문). 없으면 null. ★서버 로그에는 남기지 않는다(수신번호 혼입 가능) */
        private final String sendErrMsg;

        /** 발송 요청자 코드. 무인증 흐름(셀프가입 등)은 null */
        private final String sendUserCd;
    }
}
