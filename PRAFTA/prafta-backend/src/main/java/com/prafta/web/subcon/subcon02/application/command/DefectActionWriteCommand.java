package com.prafta.web.subcon.subcon02.application.command;

/**
 * 불량조치 write-through 복제 커맨드(PRAFTA-SUBCON-T6-06).
 *
 * <p>{@code fileMgmtCd} 는 그 티어 소유로 복제된 조치 사진 파일코드(미첨부면 빈 문자열).
 * {@code actionCmpnyCd} 는 그 티어에서 보이는 인접 1차 회사(relabel)다.
 *
 * <p>[정책 변경] 후행 덮어쓰기(last-writer-wins) 전환으로 소유 판정(ACTION_KEY)은 폐기됐다.
 * 각 티어는 무조건 UPSERT(덮어쓰기)이며, 덮어쓸 때마다 표시용 조치주체 3값을 최신 조치자로 갱신한다.
 */
public record DefectActionWriteCommand(
    String cmpnyCd
    , String siteCd
    , String chkptCd
    , String inspectItemCd
    , String workDate
    , String actionDesc
    , String fileMgmtCd
    , String actionCmpnyCd
    , String actionUserCd
    , String actionUserNm
){
}
