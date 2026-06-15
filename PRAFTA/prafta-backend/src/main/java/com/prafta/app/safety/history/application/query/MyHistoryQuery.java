package com.prafta.app.safety.history.application.query;

/**
 * 내 안전활동 이력 매퍼 쿼리 (점검/위험성 공용 — prafta-app-025 J1-10 B-6).
 *
 * <p>식별자는 모두 JWT 클레임에서 도출된 값(서비스가 Param 에서 전달). 본인 필터는
 *    매퍼에서 INSERT_NO(점검)/INIT_ASSESSOR_ID(위험성) = userCd 로 강제한다(IDOR).
 *    siteCd 는 토큰 사업장(누수 방지 동봉). 페이징은 서비스에서 병합 후 슬라이스하므로
 *    매퍼는 도메인별 전건(본인+사업장 스코프)을 시간 역순으로 반환한다.
 */
public record MyHistoryQuery(
      String cmpnyCd
    , String siteCd
    , String userCd
) {
}
