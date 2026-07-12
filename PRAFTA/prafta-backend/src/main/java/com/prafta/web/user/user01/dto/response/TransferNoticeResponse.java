package com.prafta.web.user.user01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * 소속이동 안내 조회 응답(로그인 안내 팝업용) — PRAFTA-WEB_001-3.
 *
 * <p>미확인(NOTICE_ACK_YN='N') 예약이 없으면 {@code hasNotice=false}, {@code reservation=null}.
 * 웹/앱 공용 응답(컨트롤러만 webApi/appApi 로 분리). moveDate 는 표시용 "YYYY-MM-DD".
 */
@Value
@Builder
public class TransferNoticeResponse {

    boolean hasNotice;
    Reservation reservation;

    @Value
    @Builder
    public static class Reservation {
        String reservationId;
        String moveDate;
        String toSiteNm;
        String toNodeNm;
        String defaultSchNm;
        String moveReason;
        /** 안내 문구 목록(advisory) — 이동 전 진행중 결재 종료 등. */
        List<String> guideMessages;
    }
}
