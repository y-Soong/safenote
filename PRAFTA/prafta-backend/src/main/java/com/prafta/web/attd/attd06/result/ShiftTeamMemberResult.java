package com.prafta.web.attd.attd06.result;

/**
 * prafta-com-013-05-2: 교대팀 현 소속(탈퇴 미마킹) 멤버 조회 결과.
 *   기간 연장 시 연장 구간 근무계획 재생성 대상(TEAM_IDX 별 USER_CD).
 */
public record ShiftTeamMemberResult(
    String teamIdx
    , String userCd
) {
}
