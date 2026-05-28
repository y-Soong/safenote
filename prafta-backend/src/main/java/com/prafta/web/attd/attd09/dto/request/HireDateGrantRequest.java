package com.prafta.web.attd.attd09.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 입사일 기준 연차 부여 요청 body (테스트/검증용).
 * POST /attd09/leave-grant/hire-date-grant.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5
 * <p>cmpnyCd는 요청 body로 받지 않는다(JWT 스코프만 신뢰 — 가드레일 3).
 */
@Getter
@Setter
@NoArgsConstructor
public class HireDateGrantRequest {

    /** 대상 사용자 코드 목록 (체크박스 선택) */
    private List<String> userCds;
}
