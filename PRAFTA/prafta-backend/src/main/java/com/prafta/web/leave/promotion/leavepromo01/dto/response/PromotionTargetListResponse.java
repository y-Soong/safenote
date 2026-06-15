package com.prafta.web.leave.promotion.leavepromo01.dto.response;

import java.util.List;

import com.prafta.web.leave.promotion.leavepromo01.result.PromotionTargetRowResult;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-com-008-A-4: 2차 회사직권 대상자 목록 응답.
 */
@Getter
@Builder
public class PromotionTargetListResponse {

    /** 대상자 목록(미사용 연차/2차 대상일수 포함). 없으면 빈 목록. */
    private List<PromotionTargetRowResult> targetList;
}
