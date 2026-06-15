package com.prafta.app.leave.promotion.leavepromo01.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-com-008-A-3: 앱 1차 계획서 등록 요청 본문.
 *
 * <p>식별값(cmpny/site/user)은 본문으로 받지 않는다(JWT 강제, IDOR 차단). 선택 날짜 목록만 신뢰한다.
 */
@Getter
@Setter
public class PromotionPlanRequest {

    /** 등록할 촉진 연차일 목록 (YYYYMMDD, 1일 단위). */
    private List<String> dates;
}
