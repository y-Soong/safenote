package com.prafta.app.nearmiss.nearmiss01.result;

/**
 * 아차사고 전이/접근 검증용 경량 메타 결과 VO (tb_near_miss 핵심 컬럼만, camelCase 매핑).
 *
 * <p>selectReportMeta 전용. IncidentResult(29필드, 코드명/파일경로 해석 포함)를 그대로 쓰면
 *    MyBatis record 생성자 매핑이 위치기반(argNameBasedConstructorAutoMapping=false)이라
 *    SELECT 5컬럼 ↔ 생성자 29인자 불일치로 IndexOutOfBoundsException 이 발생한다.
 *    존재/보고자/현재상태 판정에 필요한 5개 필드만 SELECT 컬럼 순서와 1:1로 정의한다.
 *
 * <p>필드 순서 = selectReportMeta SELECT 컬럼 순서(위치기반 생성자 매핑 정합).
 */
public record ReportMetaResult(
    String cmpnyCd
    , String siteCd
    , String nearMissId
    , String reportStatusCd
    , String reporterId
){
}
