package com.prafta.web.attd.attd09.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자 수동 부여 연차 회수(soft cancel) 요청 body (PRAFTA-031).
 * POST /attd09/leave-grant/{grantId}/recall.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.7 / §8.5.8
 * <p>grantId는 PathVariable로 받는다(body 아님). cmpnyCd/수행자는 JWT 스코프만 신뢰(가드레일 3).
 */
@Getter
@Setter
@NoArgsConstructor
public class LeaveRecallRequest {

    /** 회수 사유 (필수, 최대 500자) */
    private String reason;
}
