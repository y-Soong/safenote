package com.prafta.common.cmm.shift.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 교대팀 소속 일자 판정 공용 Mapper (PRAFTA-COM-008-E-7).
 *
 * <p>"특정 USER_CD 가 특정 WORK_YMD 에 교대팀 소속 구간인가" 를 단일 판정한다.
 * E-3(기본근무 자동 스케줄 생성)의 교대 제외 근거이며, D(교대 잠금)와 시그니처를 공유한다.
 *
 * <p>D-1 정밀화: {@code tb_shift_sch_team_user.LEAVE_TEAM_YMD}(per-user 탈퇴일) 를 합류했다.
 * 판정 = 팀 마스터({@code tb_shift_sch_team_mgmt}) STR_DATE~END_DATE 적용기간(가입=STR_DATE inclusive, END_DATE inclusive)
 * ∩ 멤버십 행 존재 ∩ 탈퇴일 미포함(LEAVE_TEAM_YMD 가 NULL 이거나 workYmd 보다 큼 = 탈퇴일 당일부터 변경 허용).
 */
@Mapper
public interface ShiftMembershipMapper {

    /**
     * 교대팀 소속 일자 판정. 해당 (CMPNY_CD, USER_CD) 가 가입한 교대팀 중,
     * workYmd 가 팀 적용기간(STR_DATE~END_DATE, 경계 포함, NULL 은 개방경계)에 드는 행이 1건 이상이면 &gt; 0.
     *
     * @param cmpnyCd 회사 코드 (스코프)
     * @param userCd  대상 사용자 코드
     * @param workYmd 판정 대상일(YYYYMMDD)
     * @return 소속이면 1 이상, 아니면 0
     */
    int countShiftMembershipOn(@Param("cmpnyCd") String cmpnyCd,
                               @Param("userCd") String userCd,
                               @Param("workYmd") String workYmd);

    /**
     * 교대 잠금 구간 월 단위 존재 판정 (D-1: 월 단위 근무계획 삭제 가드용).
     *
     * <p>해당 (CMPNY_CD, SITE_CD, USER_CD) 가 workYm(YYYYMM) 안의 어느 하루라도 교대 소속 구간(잠금)에 들면 &gt; 0.
     * 일(日) 단위 workYmd 가 없는 월 단위 삭제 경로(Attd05.deleteUserWorkPlans)에서 보수적 전면 차단에 사용한다.
     *
     * @param cmpnyCd 회사 코드 (스코프)
     * @param siteCd  사업장 코드 (스코프)
     * @param userCd  대상 사용자 코드
     * @param workYm  판정 대상월(YYYYMM)
     * @return 월 안에 잠금일이 1건 이상 있으면 1 이상, 없으면 0
     */
    int existsShiftLockInMonth(@Param("cmpnyCd") String cmpnyCd,
                               @Param("siteCd") String siteCd,
                               @Param("userCd") String userCd,
                               @Param("workYm") String workYm);
}
