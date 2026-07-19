package com.prafta.web.subcon.subcon02.application.param;

/**
 * 점검 응답 write-through 전파 입력(PRAFTA-SUBCON-T6-05).
 *
 * <p>기점(수행 티어) 좌표 + 복제할 값 + 수행자 스냅샷.
 *
 * <p>[정책 변경] 후행 덮어쓰기(last-writer-wins) 전환으로 소유 판정/ownUpdate 는 폐기됐다.
 * 각 티어는 대응 좌표에 무조건 UPSERT 한다(선행 데이터가 있으면 덮어쓴다).
 */
public record InspectAnswerPropagateParam(
    String cmpnyCd
    , String siteCd
    , String chkptCd
    , String inspectItemCd
    , String workDate
    , String inspectAnswerType
    , String answerDesc
    , String srcFileMgmtCd
    , String performUserCd
    , String performUserNm
){
}
