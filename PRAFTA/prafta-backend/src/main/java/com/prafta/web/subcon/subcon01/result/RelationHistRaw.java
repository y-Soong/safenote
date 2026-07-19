package com.prafta.web.subcon.subcon01.result;

/**
 * 연동 관계 이력 매퍼 원시 1행.
 *
 * <p>actionUserNm 은 자사 소속 행위자만 SQL CASE 로 해석(USER_NM, 조인 실패 시 USER_CD 폴백)하고,
 * 상대사 행위자는 NULL 로 내려온다(타 테넌트 인명이 DB 밖으로 나오지 않게 SQL 단 차단 — Q4).
 * 서비스에서 NULL → "상대사 처리" 라벨로 치환한다.
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record RelationHistRaw(
    Long histId
    , String actionType
    , String actionUserNm
    , String actionDtime
    , String actionDesc
){
}
