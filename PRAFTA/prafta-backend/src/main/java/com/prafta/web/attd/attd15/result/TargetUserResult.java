package com.prafta.web.attd.attd15.result;

/**
 * ATTD15-T1 - 주52시간 관리 대상 사용자(모수) 행.
 *
 * <p>필터(사업장/소속부서(+하위)/사용자명/일용직 제외)에 매칭되는 전 사용자를 내려준다.
 * 스케줄/실적이 모두 0인 사용자도 노출해야 하므로(사용자 결정 §2.4), 본 조회 결과가
 * 서비스 계층에서 다른 세 결과(스케줄/실적/초과근무)를 outer-merge 할 때의 모수(base)가 된다.
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서를 그대로 생성자 인자 순서로 사용한다
 * ({@code feedback_mybatis_record_column_order}) — Attd15Mapper.xml selectTargetUsers 의
 * SELECT 절 컬럼 순서를 본 레코드 필드 선언 순서와 반드시 일치시킬 것.
 */
public record TargetUserResult(
        String siteNm
        , String userCd
        , String userId
        , String userNm
        , String deptNm
        /* [소속이동이력가시성-02] "현재 타 사업장 소속" 배지용(신규). 조회 사업장(siteCd)과
           근로자의 현재 소속이 다를 때(이동자)만 값이 다르다. SELECT 끝과 동일 순서(위치 기반 매핑). */
        , String currentSiteNm
        , String currentSiteCd
) {
}
