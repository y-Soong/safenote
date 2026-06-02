package com.prafta.web.attd.attd09.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 정책 기준 부여 — 활성 연차정책 안내 정보 응답 (prafta-022 보완).
 * GET /attd09/leave-grant/policy-info.
 *
 * <p>관리자 화면(Attd_09)이 "정책 기준 부여" 버튼/프리뷰 모달에 안내 문구를 노출할지
 * 판단하기 위한 읽기 전용 메타 정보다. 부여 로직은 변경하지 않으며, 화면 노출만 지원한다.
 *
 * <p>핵심: 활성 정책의 첫해 방식(AXIS3)이 PRORATE(비례부여)일 때
 * {@code prorateFallback=true}로 채워, 현재 엔진이 차년도 일괄(NEXT_YEAR_BULK)로
 * 폴백한다는 사실을 화면에 안내한다(엔진 동작은 {@code LeaveGrantEngineServiceImpl} 그대로).
 */
@Value
@Builder
public class PolicyGrantPolicyInfoResponse {

    /** 활성 정책의 부여 기준(AXIS1): HIRE_DATE / FISCAL_YEAR. 정책 없으면 null. */
    String grantBase;

    /** 활성 정책의 첫해 방식(AXIS3): MONTHLY_ONLY / PRORATE / NEXT_YEAR_BULK. 정책 없으면 null. */
    String firstYearMethod;

    /** 첫해 방식이 PRORATE라 차년도 일괄로 폴백되는지 여부(활성 정책 존재 AND AXIS3=PRORATE). */
    boolean prorateFallback;

    /** 안내 문구. prorateFallback일 때만 채워지며, 그 외에는 빈 문자열. */
    String noticeText;
}
