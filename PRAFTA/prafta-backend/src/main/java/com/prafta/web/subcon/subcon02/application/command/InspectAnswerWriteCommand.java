package com.prafta.web.subcon.subcon02.application.command;

/**
 * 점검 응답 write-through 복제 커맨드(PRAFTA-SUBCON-T6-05).
 *
 * <p>대상 좌표는 BFS 가 서버 데이터(LINK_SRC 매핑)로만 산출한 값이다(클라 입력 회사코드 불신).
 * {@code performCmpnyCd} 는 그 티어에서 보이는 인접 1차 회사(relabel, plan D4),
 * {@code performUserCd}/{@code performUserNm} 은 원 수행자의 코드/성명 스냅샷(plan D5)이다.
 *
 * <p>[정책 변경] 후행 덮어쓰기(last-writer-wins) 전환으로 소유 판정(PERFORM_KEY)은 폐기됐다.
 * 각 티어는 무조건 UPSERT(덮어쓰기)이며, 덮어쓸 때마다 위 표시용 3값을 최신 수행자로 갱신한다.
 */
public record InspectAnswerWriteCommand(
    String cmpnyCd
    , String siteCd
    , String chkptCd
    , String inspectItemCd
    , String workDate
    , String inspectAnswerType
    , String answerDesc
    , String fileMgmtCd
    , String performCmpnyCd
    , String performUserCd
    , String performUserNm
){
}
