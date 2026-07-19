package com.prafta.web.subcon.subcon02.application.param;

/**
 * 불량조치 write-through 전파 입력(PRAFTA-SUBCON-T6-06).
 *
 * <p>기점(조치 티어) 좌표 + 조치 내역 + 조치 사진(기점 파일코드) + 조치자 스냅샷.
 *
 * <p>[정책 변경] 후행 덮어쓰기(last-writer-wins) 전환으로 소유 판정/ownUpdate 는 폐기됐다.
 * 각 티어는 대응 좌표에 무조건 UPSERT 하며, {@code srcFileMgmtCd} 가 있으면 티어 소유 파일로 사진을 복제한다.
 */
public record DefectActionPropagateParam(
    String cmpnyCd
    , String siteCd
    , String chkptCd
    , String inspectItemCd
    , String workDate
    , String actionDesc
    , String srcFileMgmtCd
    , String actionUserCd
    , String actionUserNm
){
}
