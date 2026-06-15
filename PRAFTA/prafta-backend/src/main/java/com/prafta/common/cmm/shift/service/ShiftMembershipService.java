package com.prafta.common.cmm.shift.service;

/**
 * 교대팀 소속 일자 판정 공용 서비스 (PRAFTA-COM-008-E-7).
 *
 * <p>E-3(기본근무 자동 스케줄 생성)의 교대 제외 근거이며, D(교대 잠금)와 단일 판정 함수를 공유한다.
 * D-1 정밀화로 per-user 탈퇴일(LEAVE_TEAM_YMD)을 반영한다(가입=팀 STR_DATE inclusive, 탈퇴=LEAVE_TEAM_YMD 미포함).
 */
public interface ShiftMembershipService {

    /**
     * 해당 사용자가 해당 일자에 교대팀 소속 구간인지 판정한다.
     *
     * <p>★ 시그니처 불변(E-3 generateForUser / DefaultSchGenScheduler 호출 중). 매퍼 술어만 정밀화한다.
     *
     * @param cmpnyCd 회사 코드
     * @param siteCd  사업장 코드(현 판정에는 미사용 — 시그니처 호환을 위해 보존)
     * @param userCd  대상 사용자 코드
     * @param workYmd 판정 대상일(YYYYMMDD)
     * @return 소속이면 true
     */
    boolean isInShiftTeamOn(String cmpnyCd, String siteCd, String userCd, String workYmd);

    /**
     * 교대 잠금 가드 (D-2): 해당 일자가 교대팀 소속 구간이면 근무계획 변경을 차단한다(ApiException throw).
     *
     * <p>권한과 무관하게 차단한다(master/hr/노드 관리자 호출자라도 동일 — 가드는 authCd 를 보지 않는다).
     * 차단 대상은 근무 스케줄(SCH_CD) 변경뿐이며, 연차 셀은 호출 측에서 가드를 우회시킨다(D-3).
     *
     * @param cmpnyCd 회사 코드
     * @param siteCd  사업장 코드
     * @param userCd  대상 사용자 코드
     * @param workYmd 판정 대상일(YYYYMMDD)
     * @throws com.prafta.common.exception.ApiException 교대 소속 구간이면 ATTD_400_160
     */
    void assertNotShiftLocked(String cmpnyCd, String siteCd, String userCd, String workYmd);

    /**
     * 교대 잠금 가드 월 단위 (D-2): 해당 월(YYYYMM)에 교대 소속 구간 일자가 1건이라도 있으면 차단한다.
     *
     * <p>일(日) 단위 workYmd 가 없는 월 단위 근무계획 삭제 경로(Attd05.deleteUserWorkPlans)에서
     * 보수적 전면 차단에 사용한다. 같은 에러코드(ATTD_400_160) 를 재사용한다.
     *
     * @param cmpnyCd 회사 코드
     * @param siteCd  사업장 코드
     * @param userCd  대상 사용자 코드
     * @param workYm  판정 대상월(YYYYMM)
     * @throws com.prafta.common.exception.ApiException 월 안에 잠금일이 1건 이상이면 ATTD_400_160
     */
    void assertNotShiftLockedInMonth(String cmpnyCd, String siteCd, String userCd, String workYm);
}
