package com.prafta.web.user.user04.result;

/**
 * 결재라인 결재자 후보 1행 (prafta-019-D, User_04 직급 프리셋 보조).
 *
 * <p>동일 회사/사업장 사용자 + 직급(RANK_CD)·직급 순서(rankSortIdx). 프리셋은 rankSortIdx로
 * "내 위 직급 순"을 구성하고, 사용자가 가감한다.
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
