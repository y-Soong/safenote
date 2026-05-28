package com.prafta.common.cmm.leave.service;

import java.math.BigDecimal;
import java.util.List;

import com.prafta.common.cmm.leave.vo.HireDateAdjustResultVO;
import com.prafta.common.cmm.leave.vo.HireDateGrantResultVO;
import com.prafta.common.cmm.leave.vo.PolicyGrantPreviewVO;

/**
 * 공용 연차 부여 엔진 서비스 (prafta-022 작업 A 추출).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5
 *
 * <p>{@link LeaveDashboardService}에서 부여 핵심 로직(입사일 기준 부여)을 분리하여,
 * Attd_09 버튼 경로와 향후 정기 부여 스케줄러가 동일 엔진을 공유하도록 한다.
 * 본 추출은 동치(equivalence) 리팩터링이며 부여 정책/멱등 규칙은 변경하지 않는다.
 * 대시보드 서비스는 부여 책임을 본 엔진으로 위임하고 조회 책임만 보유한다.
 */
public interface LeaveGrantEngineService {

    /**
     * 입사일 기준 연차 부여(테스트/검증용). @Transactional, 권한 MASTER/HR.
     *
     * <p>선택 직원 각각의 HIRE_DATE 기준 근속 개월 수로 법정 연차를 산정해 부여한다.
     * <ul>
     *   <li>근속 12개월 미만: 법정 월차(경과 개월수, 최대 11) → SYS_MONTHLY</li>
     *   <li>근속 12개월 이상: 본연차 15일(SYS_ANNUAL) + 활성 정책 근속가산(SYS_TENURE_BONUS)</li>
     * </ul>
     * 입사일 미입력 직원이 1명이라도 있으면 전건 부여하지 않고 예외(§입력 가드).
     * 동일 직원·연도·종류로 이미 부여된 건은 멱등 처리(중복 부여 차단, 건너뜀).
     *
     * @param cmpnyCd         회사 코드 (JWT)
     * @param userCds         대상 직원 코드 목록
     * @param authCd          수행자 권한 코드 (JWT)
     * @param operatorUserCd  수행자 사용자 코드 (INSERT_NO 기록용)
     */
    HireDateGrantResultVO hireDateGrant(String cmpnyCd, List<String> userCds, String authCd, String operatorUserCd);

    /**
     * 정책 기준 부여 프리뷰(read-only dry-run). 권한 MASTER/HR. <b>DB 쓰기 없음</b>.
     *
     * <p>{@link #hireDateGrant}와 동일한 부여 계획 산정 로직(처리방식 해석 + entitlement 산정 +
     * 멱등키 판별 + RESET_ALL 취소대상 집계)을 공유하되, 계획을 실행하지 않고 집계만 반환한다.
     * 적용 버튼 클릭 전 "선택 N명 · 신규부여 X명 · 재발급 Y명 · 변경없음 Z명"을 보여줄 용도다.
     *
     * <p>apply(hireDateGrant)와 입력 검증/권한/스코프 기준은 동일하나, 입사일 미입력자는
     * 프리뷰에서 전건 거부 대신 행 {@code note}로 안내한다(apply는 기존대로 전건 거부 유지).
     *
     * @param cmpnyCd 회사 코드 (JWT)
     * @param userCds 대상 직원 코드 목록
     * @param authCd  수행자 권한 코드 (JWT) — MASTER/HR 진입부 강제 (§8.5.7)
     * @return 직원별 처리방식·취소예정·추가예정 일수 집계
     */
    PolicyGrantPreviewVO previewPolicyGrant(String cmpnyCd, List<String> userCds, String authCd);

    /**
     * 입사일 변경 영향분석용 read-only 추정 (prafta-023 F). <b>DB 쓰기 없음</b>.
     *
     * <p>가상의 새 입사일({@code hireDate})로 바꿨을 때 BACKFILL로 소급 부여될 본연차/근속가산 일수 합을 추정한다
     * (유효기간 내 + 멱등 미부여분만). HireDateEditPop 영향분석의 "누락된 부여(변경 후 기준)"를 실제 엔진 산식과 정합시킨다.
     * AXIS1=FISCAL_YEAR 이거나 입사일 미입력/미래면 0(과거 백필 비대상).
     *
     * @param cmpnyCd  회사 코드
     * @param userCd   대상 직원
     * @param hireDate 가상 새 입사일(YYYYMMDD)
     * @return 유효기간 내 소급 부여 가능 일수 합(0 이상)
     */
    int estimateBackfillDays(String cmpnyCd, String userCd, String hireDate);

    /**
     * FISCAL 다음 회계연도 발생예정 안내 텍스트 (prafta-032 D1, read-only). <b>DB 쓰기 없음</b>.
     *
     * <p>입사일 변경 영향분석에서 옵션별 재할당 시뮬 미리보기는 prafta-032(D1/009)로 폐기했으나,
     * "FISCAL 다음 회계연도 발생예정"은 유지한다(D1). 활성 정책 AXIS1=FISCAL_YEAR면
     * "본연차 다음 회계연도(YYYY-MM-DD) 발생 예정", 그 외(HIRE_DATE/정책 없음)는 빈 문자열.
     *
     * @param cmpnyCd 회사 코드
     * @return FISCAL 발생예정 텍스트(아니면 "")
     */
    String fiscalNextGrantText(String cmpnyCd);

    /**
     * 자동 정기부여 1회 실행 (prafta-023 E, 스케줄러 진입점). @Transactional은 청크 단위로 내부 위임.
     *
     * <p>활성 연차정책 회사별로 입사일 보유 활성 직원에게 멱등 부여(중복은 엔진 멱등키로 차단).
     * 시스템 컨텍스트(권한 MASTER 통과, 수행자 SYSTEM)로 {@link #hireDateGrant}를 청크(≤500) 단위 호출한다.
     * <b>스케줄러(LeaveGrantScheduler)는 기본 비활성(게이트) — 본 메서드는 게이트가 켜졌을 때만 호출된다.</b>
     *
     * @return 부여된(신규 INSERT 발생) 직원 수 합계
     */
    int runScheduledAutoGrant();

    /**
     * 입사일 변경에 따른 수동 연차 조정(prafta-032 D3/D4/D5). <b>@Transactional(REQUIRED)</b> — 호출부
     * ({@code User01ServiceImpl.updateUserHireDate})의 단일 트랜잭션에 합류한다(D8). 검증 실패/예외 시 전체 롤백.
     *
     * <p>차액 = (목표 법정 부여량 {@code targetStatutoryGrantDays}) − (현재 ACTIVE 법정 GRANT_DAYS 합).
     * <ul>
     *   <li>{@code target == null}: 조정 없음(차액 0). 현재값만 스냅샷으로 반환.</li>
     *   <li>차액 &gt; 0(D4): 새 입사일 기준 미부여 발생일(오늘 이전) 빠른순 소급 부여 + 오늘 폴백.
     *       GRANT_TYPE은 발생일 시점 산정근속으로 자동판단(MONTHLY/ANNUAL/TENURE_BONUS), GRANT_BY_TYPE='01',
     *       멱등키 접미사 {@code _HD{histId}}.</li>
     *   <li>차액 &lt; 0(D5): 회수. 먼저 회수 가능량(ACTIVE 법정 잔여 합)을 초과하면 차단(예외).
     *       대상 ACTIVE 법정 부여를 (소멸임박→최근부여→GRANT_ID큰순)으로 순회하며 차감.
     *       전체회수+USED_DAYS=0 → STATUS='CANCELED'(CANCEL_* 기록), 그 외 → GRANT_DAYS 직접 차감.
     *       {@code withdrawReason} 필수(빈값이면 예외).</li>
     * </ul>
     * 본 기능은 법정(STATUTORY_*)만 다룬다. 약정(MANUAL_*)은 대상 외(Attd_09 수동부여 경로).
     *
     * @param cmpnyCd        회사 코드 (JWT)
     * @param userCd         대상 직원 코드
     * @param newHireDate    변경된(이미 UPDATE된) 새 입사일(YYYYMMDD)
     * @param target         관리자 입력 목표 법정 부여량(nullable, null이면 조정 없음)
     * @param withdrawReason 회수 사유(차액&lt;0 시 필수, 그 외 무시)
     * @param histId         입사일 변경 이력 HIST_ID(멱등키 _HD{histId}용)
     * @param operatorUserCd 수행자 USER_CD(INSERT_NO/CANCEL_BY 기록용)
     * @return 조정 결과(전/후 총량, 차액, 추가/회수 일수, 영향 스냅샷)
     */
    HireDateAdjustResultVO adjustStatutoryGrantsByHireDateChange(String cmpnyCd, String userCd, String newHireDate,
                                                                BigDecimal target, String withdrawReason,
                                                                String histId, String operatorUserCd);
}
