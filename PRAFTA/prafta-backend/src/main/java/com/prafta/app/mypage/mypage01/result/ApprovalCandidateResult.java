package com.prafta.app.mypage.mypage01.result;

/**
 * 결재자 후보 1행 (prafta-app-010, 앱 전용 신규).
 *
 * <p>D2: web user04 ApprovalCandidateResult 를 참고하되 앱 전용으로 신규 작성.
 * 동일 회사/사업장 활성 사용자(본인·system 제외), 직급(rankNm)·부서(nodeNm) 표시정보 포함.
 */
public record ApprovalCandidateResult(
      String userCd
    , String userId
    , String userNm
    , String rankCd
    , String rankNm
    , Integer rankSortIdx
    , String nodeNm
) {
}
