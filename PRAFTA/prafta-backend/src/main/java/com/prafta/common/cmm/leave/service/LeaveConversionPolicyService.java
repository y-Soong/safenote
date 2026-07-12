package com.prafta.common.cmm.leave.service;

import java.util.List;

import com.prafta.common.cmm.leave.vo.LeaveConversionPolicyVO;

/**
 * 연차 시간차 "1일 환산시간"(분모) 정책 서비스 (연차 시간차 환산 개편 LC-02).
 *
 * <p>회사 단위 설정(기본 480분) + 적용일 이력(effective-dating, F4). 시간차 차감의 분모는
 * 그날 스케줄이 아닌 본 설정값으로 고정된다(R1 — 지시서 §2).
 *
 * <p>설계 원칙:
 * <ul>
 *   <li>분모 선택 기준일 = 신청 대상일(WORK_YMD). 사후 신청도 근무일 기준으로 일관(F4).</li>
 *   <li>설정 미존재 회사는 480 폴백(시드 불필요 — 8시간 사업장 결과 불변, 설계 문서 §1-⑤).</li>
 *   <li>적용일은 오늘 이후만 허용(과거 소급 금지 — 소급 재계산 없음과 정합, F4).</li>
 *   <li>유효범위 60~1440분(plan §8-②) + R2 무반올림 방어(30분 단위 5자리 유한소수) 검증.</li>
 *   <li>저장/이력 조회는 정책 변경 권한자(AUTH_MASTER OR AUTH_HR_MANAGER)만 —
 *       {@code LeavePolicyService}(baim07) 권한 가드 미러(§8.5.7).</li>
 * </ul>
 *
 * <p>출처: 작업지시서_연차-시간차-환산-개편 T0·F4 / plan §1·§2 LC-02 / 정책서 attd/08-leave.md §8.5.9
 */
public interface LeaveConversionPolicyService {

    /** 기본 1일 환산시간(분) — 설정 미존재 회사 폴백(8시간). */
    int DEFAULT_CONV_MINUTES = 480;
    /** 환산시간 입력 하한(분) — plan §8-②. */
    int MIN_CONV_MINUTES = 60;
    /** 환산시간 입력 상한(분) — plan §8-②. */
    int MAX_CONV_MINUTES = 1440;

    /**
     * 신청 대상일(WORK_YMD) 기준 유효 환산시간(분) 조회.
     *
     * <p>{@code APPLY_FROM_DATE <= workYmd} 최신 1행. 행 미존재/비정상 값이면 480 폴백.
     * 시간차 차감(LC-03)·재정산(LC-05)의 분모 단일 출처.
     */
    int selectConversionMinutes(String cmpnyCd, String workYmd);

    /**
     * 환산시간 변경 이력 조회(적용일 내림차순).
     *
     * <p>권한: AUTH_MASTER OR AUTH_HR_MANAGER (Baim_07 정책 이력과 동일 가드).
     */
    List<LeaveConversionPolicyVO> findHistory(String cmpnyCd, String authCd);

    /**
     * 환산시간 저장 — 신규 적용일 INSERT, 같은 적용일 재저장은 UPDATE.
     *
     * <p>검증: 권한(AUTH_MASTER OR AUTH_HR_MANAGER) / 적용일 YYYYMMDD + 오늘 이후만(ATTD_400_190)
     * / 60~1440분(ATTD_400_191) / R2 유한소수 방어(ATTD_400_192).
     */
    void savePolicy(String cmpnyCd, String applyFromDate, Integer dailyConvMinutes, String authCd, String userCd);
}
