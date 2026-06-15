package com.prafta.app.leave.promotion.leavepromo01.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-com-008-A-3: 앱 1차 계획서 진행 컨텍스트 응답.
 *
 * <p>진행 중 1차 촉진이 있을 때만 {@code inProgress=true} 로 컨텍스트를 싣는다. 없으면
 * {@code inProgress=false} + 빈 값(프론트가 팝업/계획화면 미노출).
 *
 * <p>식별값(userCd 등)은 응답에 싣지 않는다(본인 자기조회, JWT 강제). PII 미포함.
 */
@Getter
@Builder
public class PromotionActiveResponse {

    /** 진행 중 1차 촉진 존재 여부. false 면 나머지 필드 의미 없음(프론트 미노출). */
    private boolean inProgress;

    /** 보유 연차(본연차+근속가산 ACTIVE GRANT_DAYS 합). */
    private BigDecimal grantedDays;

    /** 미지정(미사용) 잔여 연차(본연차+근속가산 ACTIVE (GRANT_DAYS-USED_DAYS) 합). */
    private BigDecimal remainingDays;

    /** 기준 만료(소멸) 일자 (YYYYMMDD). */
    private String baseAvailToDate;

    /** 이미 등록된 촉진 연차일 목록 (YYYYMMDD). */
    private List<String> designatedDates;

    /** 앱 로그인 안내 1회 노출 완료 여부 (Y/N). 'N' 이면 프론트가 안내 팝업 1회 노출 후 notified 호출. */
    private String loginNotifiedYn;
}
