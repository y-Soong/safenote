package com.prafta.web.user.user01.service;

import com.prafta.web.user.user01.application.param.TransferEligibilityParam;
import com.prafta.web.user.user01.application.param.TransferReservationParam;
import com.prafta.web.user.user01.dto.response.TransferEligibilityResponse;
import com.prafta.web.user.user01.dto.response.TransferNoticeResponse;
import com.prafta.web.user.user01.dto.response.TransferReservationResponse;

/**
 * 사용자 소속이동 서비스 — PRAFTA-WEB_001.
 *
 * <p>Terminal A: 예약 등록(검증 + INSERT)과 가능 여부 사전 판정.
 * Terminal C: 등록 즉시 PUSH(reserveTransfer 내부 훅) + 로그인 안내 조회/ack.
 * 발효(실제 SITE_CD/NODE_CD/기본근무 변경·요청종료·TBM)는 Terminal B.
 *
 * <p>안내 조회/ack 는 대상자 본인(JWT userCd) 기준 범용 시그니처 — 웹(webApi)/앱(appApi) 컨트롤러가 공용 호출.
 */
public interface User01TransferService {

    /**
     * 소속이동 가능 여부 사전 판정(불가 사유 목록). master/hr 전용.
     */
    TransferEligibilityResponse checkEligibility(TransferEligibilityParam param);

    /**
     * 소속이동 예약 등록(필수값 + 5종 불가케이스 검증 후 RESERVED 예약 생성). master/hr 전용.
     * 등록 트랜잭션 커밋 후 대상자에게 안내 PUSH 를 best-effort 로 발송한다(Terminal C).
     */
    TransferReservationResponse reserveTransfer(TransferReservationParam param);

    /**
     * 로그인 안내 — 대상자 본인의 미확인 소속이동 예약 1건 조회(없으면 hasNotice=false). 토큰 userCd 기준(IDOR 방지).
     *
     * @param cmpnyCd 회사 코드(JWT)
     * @param userCd  대상자 본인(JWT)
     */
    TransferNoticeResponse getMyTransferNotice(String cmpnyCd, String userCd);

    /**
     * 안내 확인(ack) — 본인 소유 예약만 NOTICE_ACK_YN='Y' 처리(advisory, best-effort). 타인 예약 ack 차단(IDOR).
     *
     * @param cmpnyCd       회사 코드(JWT)
     * @param userCd        대상자 본인(JWT)
     * @param reservationId 확인할 예약 ID(바디)
     */
    void ackTransferNotice(String cmpnyCd, String userCd, String reservationId);
}
