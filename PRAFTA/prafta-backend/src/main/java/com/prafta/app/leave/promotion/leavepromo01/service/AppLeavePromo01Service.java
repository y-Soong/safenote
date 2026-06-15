package com.prafta.app.leave.promotion.leavepromo01.service;

import com.prafta.app.leave.promotion.leavepromo01.application.param.PromotionPlanParam;
import com.prafta.app.leave.promotion.leavepromo01.dto.response.PromotionActiveResponse;
import com.prafta.app.leave.promotion.leavepromo01.dto.response.PromotionPlanResultResponse;
import com.prafta.common.dto.TokenInfo;

/**
 * prafta-com-008-A-3: 앱 1차 연차 사용촉진 계획서 서비스(앱 완전 분리).
 *
 * <p>식별값은 JWT(TokenInfo)에서만 도출한다(IDOR). 공용 도메인 로직(촉진 등록/판정)은
 * {@code com.prafta.common.cmm.leave.promotion} 을 재사용하며, web 코드는 직접 호출하지 않는다.
 */
public interface AppLeavePromo01Service {

    /** 내게 진행 중인 1차 촉진 컨텍스트(보유/미지정 잔여/기준 만료일/등록된 연차일/안내노출). 없으면 미진행. */
    PromotionActiveResponse getActiveContext(TokenInfo tokenInfo);

    /** 선택 날짜 다건을 1일 단위 촉진(FIRST/VOLUNTARY) 연차로 등록. 일부/미제출 허용. */
    PromotionPlanResultResponse submitPlan(PromotionPlanParam param);

    /** 로그인 안내 1회 노출 완료 플래그 갱신(LOGIN_NOTIFIED_YN='Y', 확정-3). */
    void markLoginNotified(TokenInfo tokenInfo);
}
