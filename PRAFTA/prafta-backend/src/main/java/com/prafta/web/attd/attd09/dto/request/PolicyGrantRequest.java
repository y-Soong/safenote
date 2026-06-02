package com.prafta.web.attd.attd09.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 정책 기준 부여(프리뷰/적용) 공통 요청 body (prafta-022 작업 D).
 * POST /attd09/leave-grant/policy-grant/preview, POST /attd09/leave-grant/policy-grant.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.6·§8.5.7
 * <p>cmpnyCd는 요청 body로 받지 않는다(JWT 스코프만 신뢰 — 가드레일 3).
 * 활성 정책·입사일·경력인정 기준 부여는 서버가 수행하므로 페이로드는 대상 직원 목록만 받는다.
 * prafta-032(009): 처리방식(handlingType) 자동 해석은 폐기됐다(기존 부여자는 변경 없음, 신규자만 부여).
 */
@Getter
@Setter
@NoArgsConstructor
public class PolicyGrantRequest {

    /** 대상 사용자 코드 목록 (체크박스 선택) */
    private List<String> userCds;
}
