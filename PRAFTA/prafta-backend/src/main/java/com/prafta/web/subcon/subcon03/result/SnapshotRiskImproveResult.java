package com.prafta.web.subcon.subcon03.result;

/**
 * 위험성평가 스냅샷 개선항목 자식행(수신 조회 + 릴레이 복사 공용 — PRAFTA-SUBCON-T7 §5-8).
 *
 * <p>MyBatis property 매핑(카멜 별칭). {@code fileMgmtCd} 는 수신사 소유 파일코드(릴레이 시 재복제 원본).
 */
public record SnapshotRiskImproveResult(
    Long improveId
    , Long detailId
    , Integer improveSeq
    , String improveDate
    , String improveDesc
    , Integer likelihood
    , Integer severity
    , String riskLv
    , String fileMgmtCd
){
}
