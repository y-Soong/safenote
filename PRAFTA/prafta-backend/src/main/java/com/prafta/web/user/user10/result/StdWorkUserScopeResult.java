package com.prafta.web.user.user10.result;

/**
 * 소정-10: 대상 근로자의 서버 권위 스코프 (권한 게이트 · 표시용 최소 정보).
 *
 * <p>★record 컬럼 순서 = 매퍼 SELECT 순서와 1:1 (MyBatis 위치 매핑).
 */
public record StdWorkUserScopeResult(
        String userCd
        , String userNm
        , String siteCd
        , String nodeCd
        , String employmentType
        , String useYn
        , String accountStatus
) {
}
