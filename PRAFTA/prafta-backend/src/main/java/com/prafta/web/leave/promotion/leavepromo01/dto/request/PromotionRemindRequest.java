package com.prafta.web.leave.promotion.leavepromo01.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 1차 계획 제출 독촉(재안내) PUSH 재발송 요청(확정 D4, 작업지시서 §5-2).
 *
 * <p>단건도 길이 1 배열로 받는다. <b>부서/회차는 클라이언트 값을 신뢰하지 않고</b> 서버가
 * {@code selectFirstRoundMeta} 로 재조회해 확정한다(IDOR 방어).
 *
 * <p>{@code siteCd} 는 화면이 조회 중인 사업장이다. 조회(1차 현황)가 원장 인가만 통과하면
 * 타 사업장 목록을 반환하므로, 독촉도 같은 사업장을 대상으로 삼아야 조회/발송 범위가 일치한다
 * (미전달 시 세션 사업장으로 폴백). 클라이언트 값이지만 그대로 신뢰하지 않고 서비스가
 * {@code assertSiteAccess} 로 인가를 검증한 뒤에만 사용한다 — 조회 EP 와 동일 패턴.
 */
@Getter
@Setter
public class PromotionRemindRequest {

    /** 독촉 대상 근로자 코드 목록(상한 200건). */
    private List<String> userCds;

    /** 대상 사업장 코드(화면 조회 조건). 공백이면 세션 사업장으로 폴백. */
    private String siteCd;
}
