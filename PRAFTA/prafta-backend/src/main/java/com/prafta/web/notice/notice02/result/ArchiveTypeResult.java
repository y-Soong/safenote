package com.prafta.web.notice.notice02.result;

/**
 * 자료타입 드롭다운 1건 결과 VO (tb_baim_val_d, USE_YN='Y').
 *
 * <p>⚠️ MyBatis record 매핑: SELECT 컬럼 순서 = 생성자 인자 순서(위치 기반).
 */
public record ArchiveTypeResult(
    String archiveTypeCd
    , String archiveTypeNm
    , Integer sortIdx
){
}
