package com.prafta.app.admin.dashboard.application.query;

/**
 * J1-10 (B-5): 안전 3종(순회/위험성/아차) 카운트 사업장 스코프 Query.
 *
 * <p>식별자(cmpnyCd/siteCd)는 token/멤버십 재검증 후 확정값(IDOR 차단). todayYmd 는 순회 분자(금일 점검 완료)
 * 산출에만 사용한다(위험성/아차는 누적 상태 카운트라 일자 무관).
 */
public record SafetyCountQuery(
      String cmpnyCd
    , String siteCd
    , String todayYmd
) {
}
