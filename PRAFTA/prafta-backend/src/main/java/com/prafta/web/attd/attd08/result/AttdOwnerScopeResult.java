package com.prafta.web.attd.attd08.result;

/**
 * security H-1: {@code attd-gps-trail} 인가 판정용 근태 행 스코프(사업장/부서/소유자).
 *
 * <p>GPS 궤적 API 는 {@code attdId} 만 받으므로 파라미터만으로는 인가를 판정할 수 없다.
 * 좌표를 읽기 전에 이 행으로 사업장 접근 권한·부서 관리 권한을 먼저 확인한다.
 *
 * <p>⚠️ record 매핑은 SELECT 컬럼 순서 의존 — {@code Attd08Mapper.selectAttdOwnerScope} 와 순서 일치.
 */
public record AttdOwnerScopeResult(
      String siteCd
    , String nodeCd
    , String userCd
) {
}
