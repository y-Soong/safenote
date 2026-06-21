package com.prafta.common.cmm.leave.service;

import java.math.BigDecimal;
import java.util.List;

import com.prafta.common.cmm.leave.vo.BorrowGrantResultVO;
import com.prafta.common.cmm.leave.vo.BorrowProjectionVO;
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

    // ============================================================
    // 연차 가불(마이너스/이월) 코어 (prafta-com-011-1, read-only projection + 가불 GRANT 생성/회수)
    //   출처: prafta-com-011-decisions.md §1·§2·§3·§6 / attd/08-leave.md §8.5.4·§8.5.8.
    //   호출부(submitLeave/leaveflow 합류)는 prafta-com-011-2. 본 인터페이스는 코어 메서드만 제공한다.
    // ============================================================

    /** 가불 종류 패밀리(결정 §1 — 자기 종류 안에서만 끌어옴). 월차 또는 본연차 둘 중 하나. */
    enum BorrowFamily {
        /** 1년 미만 법정 월차(SYS_MONTHLY / STATUTORY_MONTHLY) — 미래 월차분을 당겨씀. */
        MONTHLY,
        /** 본연차(SYS_ANNUAL / STATUTORY_ANNUAL) — 차기 부여 예정 본연차를 당겨씀. */
        ANNUAL
    }

    /**
     * 가불 가능 한도(일수) 산정 (read-only — DB 쓰기 없음). 결정 §2.
     *
     * <ul>
     *   <li>{@link BorrowFamily#MONTHLY}: {@code 11 − min(actualMonths, 11) − 이미 가불한 월차 일수}.
     *       (입사 4개월차 → 미래 7개월분 = 최대 7일.)</li>
     *   <li>{@link BorrowFamily#ANNUAL}: {@link #projectNextAnnualGrant} 예정 일수 − 이미 가불한 본연차 일수.</li>
     * </ul>
     * 1년 경과(월차 일괄소멸)·경력인정 더블딥·차기 만료 경과 등 가불 불가 상황이면 0 을 반환한다(절대 음수 아님).
     *
     * @param cmpnyCd  회사 코드
     * @param userCd   대상 직원 코드
     * @param hireDate 입사일(YYYYMMDD)
     * @param family   가불 종류(월차/본연차)
     * @return 가불 가능 일수(0 이상)
     */
    BigDecimal computeBorrowQuota(String cmpnyCd, String userCd, String hireDate, BorrowFamily family);

    /**
     * 차기 부여 예정 본연차 projection (read-only — DB 쓰기 없음). 결정 §2·§3 / Q2.
     *
     * <p>차기 회차 시점(AXIS1=HIRE_DATE 면 다음 입사 기념일, AXIS1=FISCAL_YEAR 면 다음 회계연도 시작)을
     * AVAIL_FROM 기준으로 STATUTORY_ANNUAL + STATUTORY_TENURE_BONUS 예정 일수와, 그 발생일 + AXIS6
     * 유효개월로 산정한 정상 만료일을 함께 반환한다(기존 {@code resolveEntitlement}/{@code tenureBonusDays} 재사용).
     *
     * @param cmpnyCd  회사 코드
     * @param userCd   대상 직원 코드
     * @param hireDate 입사일(YYYYMMDD)
     * @return 차기 본연차 예정 일수 + 발생일/만료일. 산정 불가면 days=0, ymd=null.
     */
    BorrowProjectionVO projectNextAnnualGrant(String cmpnyCd, String userCd, String hireDate);

    /**
     * 가불 사용 일자(workYmd)가 만료(소멸)일을 지났는지 fail-closed 검증 (결정 §3). DB 쓰기 없음.
     *
     * <p>경과한 경우 {@code ATTD_400_181} 예외를 던진다(프론트 alert 우회 방지).
     * <ul>
     *   <li>{@link BorrowFamily#MONTHLY}: 만료일 = 입사일 + 1년 − 1일(첫해 월차 일괄소멸일).</li>
     *   <li>{@link BorrowFamily#ANNUAL}: 만료일 = {@link #projectNextAnnualGrant} 의 차기 만료일.</li>
     * </ul>
     *
     * @param cmpnyCd  회사 코드
     * @param userCd   대상 직원 코드
     * @param hireDate 입사일(YYYYMMDD)
     * @param workYmd  가불 사용 일자(YYYYMMDD)
     * @param family   가불 종류
     */
    void assertBorrowWorkYmdWithinExpiry(String cmpnyCd, String userCd, String hireDate, String workYmd,
                                         BorrowFamily family);

    /**
     * 가불 GRANT 생성 (결정 §6). <b>@Transactional</b> — 호출부(신청 흐름) 트랜잭션에 합류한다(REQUIRED).
     *
     * <p>정기 부여 배치와 동일한 멱등키로 미래 GRANT 행을 미리 생성한다(추후 배치가 멱등 skip → 자동 상계).
     * 가불 마커는 멱등키 밖({@code GRANT_REASON} 프리픽스 {@code [가불] ...})에 둔다. GRANT_BY_TYPE='01'(자동),
     * AVAIL_FROM=workYmd, AVAIL_TO=만료일(월차=입사+1년−1일, 본연차=차기 만료일), STATUS='ACTIVE', USED_DAYS=0.
     * 멱등키 UNIQUE 충돌(이미 정기 부여됨)이면 해당 슬롯 생성을 skip 한다(이미 부여 = 가불 불필요).
     *
     * <p>월차는 §8.5.4 발생 모델(입사+m개월)의 "다음 미발생 월차 슬롯"부터 순서대로 1일씩 분할 점유한다(Q3).
     * 멱등키 라벨 = 슬롯 YYYYMM(배치 키 동일). 한도(11−발생분) 내에서만 생성한다.
     *
     * @param cmpnyCd        회사 코드
     * @param userCd         대상 직원 코드
     * @param hireDate       입사일(YYYYMMDD)
     * @param family         가불 종류(월차/본연차)
     * @param days           가불 충당 일수(부족분)
     * @param workYmd        가불 사용 일자(YYYYMMDD) — AVAIL_FROM 으로 설정
     * @param operatorUserCd 수행자 USER_CD(INSERT_NO 기록용)
     * @return 생성된 가불 GRANT 슬롯 목록 + 생성/skip 일수
     */
    BorrowGrantResultVO createBorrowGrant(String cmpnyCd, String userCd, String hireDate, BorrowFamily family,
                                          BigDecimal days, String workYmd, String operatorUserCd);

    /**
     * 가불 GRANT 회수 (반려/취소 시, 결정 §6). <b>@Transactional</b> — 호출부 트랜잭션에 합류한다(REQUIRED).
     *
     * <p>해당 reqId 의 leave_use 가 차감했던 가불 GRANT({@code GRANT_REASON LIKE '[가불]%'}) 중
     * <b>USED_DAYS=0</b>(차감 해제 후) 인 ACTIVE 행만 STATUS='CANCELED' 로 전환한다(유령 미래 부여 방지).
     * 비가불 GRANT 와 USED_DAYS&gt;0 가불 GRANT 는 건드리지 않는다(§8.5.8 기부여보호).
     *
     * @param cmpnyCd        회사 코드
     * @param reqId          반려된 연차 요청 ID(tb_user_attd_req.REQ_ID)
     * @param operatorUserCd 수행자 USER_CD(CANCEL_BY 기록용)
     * @return 회수(CANCELED)된 가불 GRANT 건수
     */
    int cancelBorrowGrantByReqId(String cmpnyCd, String reqId, String operatorUserCd);
}
