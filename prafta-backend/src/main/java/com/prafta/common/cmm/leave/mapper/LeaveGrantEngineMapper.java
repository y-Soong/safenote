package com.prafta.common.cmm.leave.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.leave.vo.LeaveGrantRecallRowVO;

/**
 * 연차 부여 엔진 전용 Mapper (prafta-022 작업 C).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.6(입사일 변경 처리 매트릭스),
 * §8.5.8(멱등·기부여보호 절대규칙).
 *
 * <p>입사일 변경 이력({@code TB_USER_HIRE_DATE_HISTORY})의 처리방식(HANDLING_TYPE) 해석과
 * RESET_ALL 시 기존 법정 부여 취소 대상 조회·이력 적용 마킹을 담당한다.
 * 모든 조회/UPDATE는 CMPNY_CD 스코프로 격리한다.
 */
@Mapper
public interface LeaveGrantEngineMapper {

    /**
     * 해당 직원의 미적용(APPLIED_YN='N') 입사일 변경 이력 중 INSERT_DATE 최신 1건의
     * 처리방식(HANDLING_TYPE)과 이력 ID(HIST_ID)를 조회한다.
     *
     * <p><b>prafta-032 009 — 처리방식 자동계산 폐기:</b> 입사일 변경 처리방식(SYS039) 분기를 폐기하면서
     * 부여 엔진({@code buildUserPlan})의 호출을 제거했다. 본 SQL은 신규 비즈니스 경로에서 더는 호출되지
     * 않으나(엔진 기준 데드), 기존 부여 엔진 단위테스트(prafta-029/030/scenario)의 mock 스텁 호환 및 이력
     * 감사 추적 목적으로 잔존시킨다. 신규 입사일 변경 이력은 HANDLING_TYPE='MANUAL' 고정이다.
     *
     * @param cmpnyCd 회사 코드 (CMPNY_CD 스코프)
     * @param userCd  대상 직원 코드
     * @return HIST_ID/HANDLING_TYPE 맵 (없으면 null)
     */
    java.util.Map<String, Object> selectLatestUnappliedHandling(@Param("cmpnyCd") String cmpnyCd,
                                                                @Param("userCd") String userCd);

    /**
     * 해당 직원의 활성 법정 부여(GRANT_TYPE LIKE 'STATUTORY_%'
     * AND STATUS IN ('ACTIVE','EXHAUSTED') AND DEL_YN='N') GRANT_ID 목록.
     *
     * <p><b>prafta-032 009 — RESET_ALL 폐기:</b> 처리방식 RESET_ALL 자동 취소+재발급 경로를 폐기하면서
     * 부여 엔진의 호출을 제거했다(엔진 기준 데드). 기존 부여 엔진 단위테스트의 mock 스텁 호환 및 향후
     * 진단/조회 목적으로 잔존시킨다. 신규 비즈니스 경로에서는 호출하지 않는다.
     *
     * @param cmpnyCd 회사 코드 (CMPNY_CD 스코프)
     * @param userCd  대상 직원 코드
     * @return 활성 법정 부여 GRANT_ID 목록 (없으면 빈 리스트)
     */
    List<String> selectActiveStatutoryGrantIds(@Param("cmpnyCd") String cmpnyCd,
                                               @Param("userCd") String userCd);

    /**
     * 부여 성공 후, 해당 직원의 미적용(APPLIED_YN='N') 입사일 변경 이력 전부를
     * APPLIED_YN='Y', APPLIED_DATE=NOW(), APPLIED_BY=수행자로 일괄 마킹한다.
     *
     * <p>누적 결과 HIRE_DATE가 모든 수정을 반영하므로 이전 미적용 행도 함께 소진 처리한다.
     *
     * @param cmpnyCd   회사 코드 (CMPNY_CD 스코프)
     * @param userCd    대상 직원 코드
     * @param appliedBy 적용 수행자 USER_CD
     * @return 마킹된 행 수
     */
    int markHireDateHistoryApplied(@Param("cmpnyCd") String cmpnyCd,
                                   @Param("userCd") String userCd,
                                   @Param("appliedBy") String appliedBy);

    /**
     * 자동 정기부여(prafta-023 E) 대상 회사: 활성 연차정책({@code TB_LEAVE_POLICY.USE_YN='Y'})을 가진 회사 코드.
     * 활성 정책이 없는 회사는 자동 부여 대상이 아니다.
     */
    List<String> selectAutoGrantCompanyCds();

    /**
     * 자동 정기부여(prafta-023 E) 대상 직원: 활성(USE_YN='Y', 미탈퇴, ACCOUNT_STATUS='01') + 입사일 보유 직원 USER_CD.
     * 입사일 미보유자는 제외(부여 대상 아님). USER_CD 오름차순.
     */
    List<String> selectActiveUserCdsForAutoGrant(@Param("cmpnyCd") String cmpnyCd);

    // ============================================================
    // 입사일 변경 수동 연차 조정 (prafta-032 D3/D4/D5) — 신규 SQL
    //   ⚠️ A안 핵심: 기존 잔액/차감 집계 SQL(LeaveDashboardMapper)을 건드리지 않고 회수 전용 SQL을 신규 추가한다.
    // ============================================================

    /**
     * 회수 가능량(prafta-032 D3): 해당 직원의 ACTIVE 법정(STATUTORY_*) 부여의
     * <b>잔여 합계 = SUM(GRANT_DAYS − USED_DAYS)</b>.
     *
     * <p>USED_DAYS 는 휴가 신청 시점(REQ_STATUS='01' 신청 포함)에 CONFIRMED 사용기록으로 예약되어
     * 즉시 반영되므로(leaveflow), 잔여 합계는 "이미 사용 + 사용 예정(승인 대기/승인완료 미래사용)"을
     * 자연 제외한다(D3 근거). 별도 신청 진행분 차감이 필요 없다.
     * 음수 기여(데이터 불일치)는 0으로 클램프하지 않고 SUM 그대로 두되, 행 잔여는 비음수가 정상.
     *
     * @param cmpnyCd 회사 코드 (CMPNY_CD 스코프)
     * @param userCd  대상 직원 코드
     * @return ACTIVE 법정 잔여 합계(없으면 0)
     */
    BigDecimal selectRecallableStatutoryTotal(@Param("cmpnyCd") String cmpnyCd,
                                              @Param("userCd") String userCd);

    /**
     * 현재 법정 부여량(prafta-032 D2): 해당 직원의 ACTIVE 법정(STATUTORY_*) 부여 GRANT_DAYS 합계.
     *
     * <p>차액 = (관리자 입력 목표) − (이 값). HireDateEditPop ②의 "법정 부여" 표시와 동일 기준
     * (ACTIVE STATUTORY GRANT_DAYS 합). 기존 잔액 집계 SQL은 건드리지 않고 신규로 추가한다(A안).
     *
     * @param cmpnyCd 회사 코드 (CMPNY_CD 스코프)
     * @param userCd  대상 직원 코드
     * @return ACTIVE 법정 GRANT_DAYS 합(없으면 0)
     */
    BigDecimal selectActiveStatutoryGrantedTotal(@Param("cmpnyCd") String cmpnyCd,
                                                 @Param("userCd") String userCd);

    /**
     * 회수 대상(prafta-032 D5): 해당 직원의 ACTIVE 법정(STATUTORY_*) 부여행을 회수 우선순위로 정렬해 조회한다.
     *
     * <p>우선순위: ① AVAIL_TO_DATE 가까운 순(ASC) → ② GRANT_DATE 최근 순(DESC) → ③ GRANT_ID 큰 순(DESC).
     * STATUS='ACTIVE' AND DEL_YN='N' 만. 서비스가 순회하며 회수량을 차감한다.
     *
     * @param cmpnyCd 회사 코드 (CMPNY_CD 스코프)
     * @param userCd  대상 직원 코드
     * @return 우선순위 정렬된 ACTIVE 법정 부여행(없으면 빈 리스트)
     */
    List<LeaveGrantRecallRowVO> selectActiveStatutoryGrantsForRecall(@Param("cmpnyCd") String cmpnyCd,
                                                                     @Param("userCd") String userCd);

    /**
     * 회수(전체, prafta-032 D5): 잔여 전체를 회수하고 USED_DAYS=0 인 행을 STATUS='CANCELED' 로 전환한다.
     * prafta-031 회수 패턴과 동일하게 CANCEL_REASON/CANCEL_DATE/CANCEL_BY 를 기록한다.
     * USED_DAYS 는 갱신하지 않는다(사용분 보존). GRANT_REASON 에 회수 태깅을 덧붙인다.
     * WHERE 를 grantId + cmpnyCd + STATUS='ACTIVE' + DEL_YN='N' 으로 못박아 경합을 재확인한다.
     *
     * @return 전환된 행 수(정상 1, 경합 시 0)
     */
    int cancelStatutoryGrantForHireChange(@Param("cmpnyCd") String cmpnyCd,
                                          @Param("grantId") String grantId,
                                          @Param("reason") String reason,
                                          @Param("grantReason") String grantReason,
                                          @Param("operatorUserCd") String operatorUserCd);

    /**
     * 회수(부분, prafta-032 D5): 부분 회수이거나 USED_DAYS>0 인 행의 GRANT_DAYS 를 직접 차감한다.
     * USED_DAYS 와 tb_user_leave_use FK 는 불변. GRANT_REASON 에 회수 태깅을 덧붙인다.
     * WHERE 를 grantId + cmpnyCd + STATUS='ACTIVE' + DEL_YN='N' + (GRANT_DAYS - USED_DAYS) >= 차감량 으로
     * 못박아 잔여 미만 차감/경합을 차단한다.
     *
     * @param reduceDays 차감할 일수(양수)
     * @return 차감된 행 수(정상 1, 경합/잔여부족 시 0)
     */
    int reduceStatutoryGrantDaysForHireChange(@Param("cmpnyCd") String cmpnyCd,
                                              @Param("grantId") String grantId,
                                              @Param("reduceDays") BigDecimal reduceDays,
                                              @Param("grantReason") String grantReason,
                                              @Param("operatorUserCd") String operatorUserCd);
}
