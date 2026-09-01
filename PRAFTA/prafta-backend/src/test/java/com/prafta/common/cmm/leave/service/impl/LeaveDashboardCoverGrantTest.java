package com.prafta.common.cmm.leave.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.command.CoverGrantCommand;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.service.LeaveGrantEngineService;
import com.prafta.common.cmm.leave.service.LeavePolicyService;
import com.prafta.common.cmm.leave.vo.CoverGrantResultVO;
import com.prafta.common.cmm.leave.vo.LeaveGrantInsertVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;
import com.prafta.common.cmm.siteauth.service.SiteAccessService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 경력인정 이원화 Phase 2 §2-3 — 입사일 기준 차액 보전(법정 수기부여, {@code _COVER}) 단위테스트.
 *
 * <p>★R-6 최우선 방어 검증: {@code _COVER} 멱등키가 엔진 표준키 형식({@code {userCd}_{periodLabel}_
 * STATUTORY_ANNUAL...})과 절대 겹치지 않는지(변형키 가드 {@code countActiveBySuffixVariant} 오탐 방지)를
 * 핵심으로 검증한다. 상한 서버 강제(ATTD_400_210)·소정-05 게이트(ATTD_400_211)·입력 검증(ATTD_400_212)도 함께.
 */
class LeaveDashboardCoverGrantTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 21);
    private static final String CMPNY = "C001";
    private static final String AUTH_MASTER = "master";
    private static final String OPERATOR = "OP001";
    private static final String TARGET_USER = "U001";
    private static final String BASE_YMD = "20260821";

    private LeaveDashboardMapper dash;
    private LeavePolicyService policy;
    private LeaveGrantEngineService engine;
    private LeaveDashboardServiceImpl svc;
    private MockedStatic<LocalDate> localDateMock;

    @BeforeEach
    void setUp() {
        dash = mock(LeaveDashboardMapper.class);
        policy = mock(LeavePolicyService.class);
        engine = mock(LeaveGrantEngineService.class);
        svc = new LeaveDashboardServiceImpl(dash, mock(FileService.class), policy, engine, new ObjectMapper(),
                mock(LeaveConversionPolicyService.class),
                mock(SiteAccessService.class));

        localDateMock = mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS);
        localDateMock.when(LocalDate::now).thenReturn(TODAY);

        lenient().when(dash.countActiveUser(eq(CMPNY), eq(TARGET_USER))).thenReturn(1);
        lenient().when(dash.selectNextGrantId(eq(CMPNY))).thenReturn("G900001");
        lenient().when(dash.insertManualGrant(any(LeaveGrantInsertVO.class))).thenReturn(1);
        lenient().when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(0);
        lenient().when(policy.isStatutoryAutoGrantEnabled(eq(CMPNY))).thenReturn(true);
        lenient().when(policy.findActivePolicy(eq(CMPNY))).thenReturn(fiscalPolicy());
        // SEC-P2-1: 상한 재계산~INSERT 직렬화 advisory lock — 기본은 획득 성공(1).
        lenient().when(dash.getAdvisoryLock(anyString(), anyInt())).thenReturn(1);
        lenient().when(dash.releaseAdvisoryLock(anyString())).thenReturn(1);

        // 정답 누적 10일, 실제 부여 누적(P-12 live 법정 부여 총량, 사용·만료 무관) 6일 → 남은 부족분 4일(기본 시나리오)
        lenient().when(engine.computeHireBasisAccrual(eq(CMPNY), eq(TARGET_USER), any(LocalDate.class)))
                .thenReturn(BigDecimal.valueOf(10));
        lenient().when(dash.selectStatutoryGrantedLiveTotal(eq(CMPNY), eq(TARGET_USER)))
                .thenReturn(BigDecimal.valueOf(6));
    }

    @AfterEach
    void tearDown() {
        localDateMock.close();
    }

    private LeavePolicyVO fiscalPolicy() {
        LeavePolicyVO p = new LeavePolicyVO();
        p.setPolicySeq(1L);
        p.setAxis1GrantBase("FISCAL_YEAR");
        p.setAxis6ValidityMonths(12);
        return p;
    }

    private CoverGrantCommand command(BigDecimal grantDays) {
        return new CoverGrantCommand(TARGET_USER, grantDays, "입사일 기준 차액 보전", BASE_YMD);
    }

    private LeaveGrantInsertVO captureInserted() {
        ArgumentCaptor<LeaveGrantInsertVO> cap = ArgumentCaptor.forClass(LeaveGrantInsertVO.class);
        org.mockito.Mockito.verify(dash).insertManualGrant(cap.capture());
        return cap.getValue();
    }

    @Test
    @DisplayName("남은 부족분(4일) 이내 요청(3일) — 정상 부여, GRANT_TYPE=STATUTORY_ANNUAL/GRANT_BY_TYPE=02")
    void withinShortfall_succeeds() {
        CoverGrantResultVO result = svc.coverGrant(CMPNY, command(BigDecimal.valueOf(3)), AUTH_MASTER, OPERATOR);

        assertEquals(0, BigDecimal.valueOf(3).compareTo(result.getGrantedDays()));
        assertEquals(0, BigDecimal.valueOf(1).compareTo(result.getRemainingShortfallAfter())); // 4-3=1
        // P2-D7: 같은 DTO 안 grantedDays 와 소수 자릿수 통일(setScale 1) — API 계약 일관성.
        assertEquals(1, result.getRemainingShortfallAfter().scale());

        LeaveGrantInsertVO vo = captureInserted();
        assertEquals("STATUTORY_ANNUAL", vo.getGrantType());
        assertEquals("SYS_ANNUAL", vo.getLeaveCd());
        assertEquals("02", vo.getGrantByType());
        assertTrue(vo.getIdempotencyKey().endsWith("_COVER"), "멱등키는 _COVER 접미사로 끝나야 한다");
    }

    @Test
    @DisplayName("★R-6 최우선 방어: _COVER 멱등키는 엔진 표준키 패턴({userCd}_{periodLabel}_STATUTORY_ANNUAL_...)과 "
            + "절대 겹치지 않는다(countActiveBySuffixVariant 오탐 방지)")
    void coverKey_neverMatchesEngineStandardKeyPattern() {
        svc.coverGrant(CMPNY, command(BigDecimal.valueOf(1)), AUTH_MASTER, OPERATOR);
        String coverKey = captureInserted().getIdempotencyKey();

        // 실제 SQL LIKE 방어(countActiveBySuffixVariant)는 baseKey(={userCd}_{periodLabel}_STATUTORY_ANNUAL)
        //   에 언더스코어를 붙인 접두사 매칭이다. _COVER 키가 이 패턴에 걸리려면 키 안에 리터럴 문자열
        //   "STATUTORY_ANNUAL_" 이 등장해야 하는데, buildCoverGrantIdempotencyKey는 해시(hash8)+윈도우로만
        //   구성되어 이 문자열을 절대 포함하지 않는다(구조적으로 불가능 — 확률적 회피가 아님).
        assertFalse(coverKey.contains("STATUTORY_ANNUAL"),
                "_COVER 키는 GRANT_TYPE 문자열을 포함하지 않아야 엔진 변형키 가드와 격리된다: " + coverKey);

        // 대조: 엔진 표준키는 반드시 이 리터럴을 포함한다(양성 대조군 — 방어 로직 자체가 유효함을 재확인).
        String engineStandardKeyExample = TARGET_USER + "_2026_STATUTORY_ANNUAL";
        assertTrue(engineStandardKeyExample.contains("STATUTORY_ANNUAL"));
    }

    @Test
    @DisplayName("남은 부족분(4일) 초과 요청(5일) — ATTD_400_210 거부, INSERT 미실행")
    void exceedsShortfall_rejected() {
        ApiException ex = assertThrows(ApiException.class,
                () -> svc.coverGrant(CMPNY, command(BigDecimal.valueOf(5)), AUTH_MASTER, OPERATOR));
        assertEquals(AttdErrorCode.ATTD_400_210, ex.getErrorCode());
        org.mockito.Mockito.verify(dash, org.mockito.Mockito.never()).insertManualGrant(any());
    }

    @Test
    @DisplayName("소정-05(법정 자동부여) OFF 회사 — ATTD_400_211 차단(계산 API는 별개, 본 실행 경로만 차단)")
    void autoGrantOff_blocked() {
        when(policy.isStatutoryAutoGrantEnabled(eq(CMPNY))).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> svc.coverGrant(CMPNY, command(BigDecimal.valueOf(1)), AUTH_MASTER, OPERATOR));
        assertEquals(AttdErrorCode.ATTD_400_211, ex.getErrorCode());
        org.mockito.Mockito.verify(dash, org.mockito.Mockito.never()).insertManualGrant(any());
    }

    @Test
    @DisplayName("0.5일 단위 위반(1.3일) — ATTD_400_212 거부")
    void invalidUnit_rejected() {
        ApiException ex = assertThrows(ApiException.class,
                () -> svc.coverGrant(CMPNY, command(BigDecimal.valueOf(1.3)), AUTH_MASTER, OPERATOR));
        assertEquals(AttdErrorCode.ATTD_400_212, ex.getErrorCode());
    }

    @Test
    @DisplayName("0 이하 요청 — ATTD_400_212 거부")
    void zeroOrNegative_rejected() {
        ApiException ex = assertThrows(ApiException.class,
                () -> svc.coverGrant(CMPNY, command(BigDecimal.ZERO), AUTH_MASTER, OPERATOR));
        assertEquals(AttdErrorCode.ATTD_400_212, ex.getErrorCode());
    }

    @Test
    @DisplayName("★P2-D2 재작업: 미래 기준일(baseYmd)로도 상한이 열리지 않는다 — 상한 재계산은 서버 오늘 고정, "
            + "클라이언트 baseYmd 는 부여 상한 산정에 사용 금지(무제한 반복 부여 구멍 봉쇄)")
    void futureBaseYmd_doesNotOpenCap() {
        // 미래 기준일(2035-07-12) 기준으로는 부족분이 크게 벌어지도록 스텁 —
        //   종전 결함 코드라면 이 스텁이 상한을 열어 5일 부여가 성공했을 것.
        String futureYmd = "20350712";
        LocalDate futureDate = LocalDate.of(2035, 7, 12);
        lenient().when(engine.computeHireBasisAccrual(eq(CMPNY), eq(TARGET_USER), eq(futureDate)))
                .thenReturn(BigDecimal.valueOf(150));

        CoverGrantCommand futureCommand =
                new CoverGrantCommand(TARGET_USER, BigDecimal.valueOf(5), "미래 기준일 시도", futureYmd);
        ApiException ex = assertThrows(ApiException.class,
                () -> svc.coverGrant(CMPNY, futureCommand, AUTH_MASTER, OPERATOR));

        // 오늘(TODAY=20260821) 기준 남은 부족분 4일 < 요청 5일 → 거부되어야 한다.
        assertEquals(AttdErrorCode.ATTD_400_210, ex.getErrorCode());
        org.mockito.Mockito.verify(dash, org.mockito.Mockito.never()).insertManualGrant(any());
        // 상한 산정 입력이 서버 오늘로 고정됐는지 — 미래 기준일 파라미터가 산정에 쓰이지 않았음을 검증.
        //   (P-12 이후 실제 부여 누적 축은 기준일 파라미터 자체가 없어 미래 기준일로 열 방법이 구조적으로 없다.)
        org.mockito.Mockito.verify(engine).computeHireBasisAccrual(eq(CMPNY), eq(TARGET_USER), eq(TODAY));
        org.mockito.Mockito.verify(engine, org.mockito.Mockito.never())
                .computeHireBasisAccrual(eq(CMPNY), eq(TARGET_USER), eq(futureDate));
        org.mockito.Mockito.verify(dash).selectStatutoryGrantedLiveTotal(eq(CMPNY), eq(TARGET_USER));
    }

    @Test
    @DisplayName("P2-D3 재작업: AXIS1=HIRE_DATE 회사 — ATTD_400_213 차단(조회 EP fiscalYearYn='N' 과 게이트 수준 동기화)")
    void hireDateCompany_blocked() {
        LeavePolicyVO hirePolicy = new LeavePolicyVO();
        hirePolicy.setPolicySeq(2L);
        hirePolicy.setAxis1GrantBase("HIRE_DATE");
        when(policy.findActivePolicy(eq(CMPNY))).thenReturn(hirePolicy);

        ApiException ex = assertThrows(ApiException.class,
                () -> svc.coverGrant(CMPNY, command(BigDecimal.valueOf(1)), AUTH_MASTER, OPERATOR));
        assertEquals(AttdErrorCode.ATTD_400_213, ex.getErrorCode());
        org.mockito.Mockito.verify(dash, org.mockito.Mockito.never()).insertManualGrant(any());
    }

    @Test
    @DisplayName("P2-D3 재작업: 활성 정책 없음(폴백 HIRE_DATE) — ATTD_400_213 차단(fail-closed)")
    void noActivePolicy_blocked() {
        when(policy.findActivePolicy(eq(CMPNY))).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> svc.coverGrant(CMPNY, command(BigDecimal.valueOf(1)), AUTH_MASTER, OPERATOR));
        assertEquals(AttdErrorCode.ATTD_400_213, ex.getErrorCode());
    }

    @Test
    @DisplayName("동일 제출 단시간 중복(30초 윈도우) — ATTD_409_030 차단")
    void duplicateSubmission_withinDedupWindow_blocked() {
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(1);

        ApiException ex = assertThrows(ApiException.class,
                () -> svc.coverGrant(CMPNY, command(BigDecimal.valueOf(1)), AUTH_MASTER, OPERATOR));
        assertEquals(AttdErrorCode.ATTD_409_030, ex.getErrorCode());
    }

    @Test
    @DisplayName("P2-D6 재작업: 회수(CANCELED) 행이 멱등키 점유 → 유니크 충돌 시 ATTD_409_031"
            + "(회수 직후 동일 내용 재부여 안내 — 종전 409_030 '방금 동일한 부여가 처리됨' 오문구 분리)")
    void duplicateKeyByCanceledRow_blockedWithClearMessage() {
        // 사전검사(countLive=0, CANCELED 제외라 통과) → INSERT 유니크 충돌 → 재조회에도 live 0건 = 회수 행 점유.
        when(dash.insertManualGrant(any(LeaveGrantInsertVO.class)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("UK_LEAVE_GRANT_IDEMPOTENCY"));

        ApiException ex = assertThrows(ApiException.class,
                () -> svc.coverGrant(CMPNY, command(BigDecimal.valueOf(1)), AUTH_MASTER, OPERATOR));
        assertEquals(AttdErrorCode.ATTD_409_031, ex.getErrorCode());
    }

    @Test
    @DisplayName("P2-D6 재작업: 동시 제출 경합(다른 트랜잭션이 방금 INSERT) → 유니크 충돌 시 기존 ATTD_409_030 유지")
    void duplicateKeyByConcurrentInsert_keepsExistingCode() {
        // 사전검사 시점 live 0건 → INSERT 유니크 충돌 → 재조회 시 live 1건(경합 상대가 INSERT 완료).
        when(dash.countLiveByIdempotencyKey(anyString(), anyString())).thenReturn(0, 1);
        when(dash.insertManualGrant(any(LeaveGrantInsertVO.class)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("UK_LEAVE_GRANT_IDEMPOTENCY"));

        ApiException ex = assertThrows(ApiException.class,
                () -> svc.coverGrant(CMPNY, command(BigDecimal.valueOf(1)), AUTH_MASTER, OPERATOR));
        assertEquals(AttdErrorCode.ATTD_409_030, ex.getErrorCode());
    }

    @Test
    @DisplayName("대상 직원이 회사 스코프 밖 — ATTD_404_020 (IDOR 방어)")
    void targetOutOfScope_rejected() {
        when(dash.countActiveUser(eq(CMPNY), eq(TARGET_USER))).thenReturn(0);

        ApiException ex = assertThrows(ApiException.class,
                () -> svc.coverGrant(CMPNY, command(BigDecimal.valueOf(1)), AUTH_MASTER, OPERATOR));
        assertEquals(AttdErrorCode.ATTD_404_020, ex.getErrorCode());
    }

    @Test
    @DisplayName("★P-12(P2R2-N1 해소): 상한 축 = live 법정 부여 총량(사용·만료 무관) — QTUSERB 시나리오 "
            + "(정답 26 · 월차 11 부여 후 소멸) 부족분 15.0 정확히 부여 성공, 15.5 는 ATTD_400_210")
    void p12_capUsesLiveGrantedTotal_expiryAndUsageIrrelevant() {
        // QTUSERB 재현: 정답 누적 26(오늘 기준), live 법정 부여 총량 11(월차 11일 — AVAIL_TO 가 과거로
        //   소멸됐든, 0/5.5/11일을 썼든 SUM(GRANT_DAYS)=11 로 동일. 사용·만료는 SQL 축에서 원천 배제).
        when(engine.computeHireBasisAccrual(eq(CMPNY), eq(TARGET_USER), eq(TODAY)))
                .thenReturn(new BigDecimal("26.0"));
        when(dash.selectStatutoryGrantedLiveTotal(eq(CMPNY), eq(TARGET_USER)))
                .thenReturn(new BigDecimal("11.0"));

        // 노무상 정답 부족분 15.0 — 정확히 부여 성공(remainingShortfallAfter=0.0).
        CoverGrantResultVO result =
                svc.coverGrant(CMPNY, command(new BigDecimal("15.0")), AUTH_MASTER, OPERATOR);
        assertEquals(0, new BigDecimal("15.0").compareTo(result.getGrantedDays()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getRemainingShortfallAfter()));

        // 15.5(소멸 월차 부활 방향으로 0.5 라도 초과) → 거부.
        ApiException ex = assertThrows(ApiException.class,
                () -> svc.coverGrant(CMPNY, command(new BigDecimal("15.5")), AUTH_MASTER, OPERATOR));
        assertEquals(AttdErrorCode.ATTD_400_210, ex.getErrorCode());

        // ★축 전환 고정: 종전 축(selectStatutoryGrantAccrual, 소멸 제외+사용 포함)은 어떤 인자로도
        //   호출되지 않아야 한다(그 축이면 사용량에 따라 부족분이 26.0/20.5/15.0 으로 흔들린다).
        org.mockito.Mockito.verify(dash, org.mockito.Mockito.never())
                .selectStatutoryGrantAccrual(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("★SEC-P2-1: advisory lock 미획득(타임아웃/오류) — ATTD_409_071, 상한 재계산·INSERT 미도달")
    void secP21_lockNotAcquired_rejectedBeforeCapRecompute() {
        when(dash.getAdvisoryLock(anyString(), anyInt())).thenReturn(0);

        ApiException ex = assertThrows(ApiException.class,
                () -> svc.coverGrant(CMPNY, command(BigDecimal.valueOf(1)), AUTH_MASTER, OPERATOR));
        assertEquals(AttdErrorCode.ATTD_409_071, ex.getErrorCode());
        org.mockito.Mockito.verify(engine, org.mockito.Mockito.never())
                .computeHireBasisAccrual(anyString(), anyString(), any(LocalDate.class));
        org.mockito.Mockito.verify(dash, org.mockito.Mockito.never()).insertManualGrant(any());
        // 획득 실패한 락은 해제하지 않는다(다른 세션 보유분 — 오해제 금지).
        org.mockito.Mockito.verify(dash, org.mockito.Mockito.never()).releaseAdvisoryLock(anyString());
    }

    @Test
    @DisplayName("★SEC-P2-1: 상한 재계산→INSERT 구간이 락 안에서 실행되고, 트랜잭션 동기화 비활성(단위테스트) "
            + "환경에선 finally 폴백으로 해제된다 — acquire→재계산→INSERT→release 순서 고정")
    void secP21_capRecomputeAndInsertInsideLock_thenReleased() {
        svc.coverGrant(CMPNY, command(BigDecimal.valueOf(3)), AUTH_MASTER, OPERATOR);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(dash, engine);
        inOrder.verify(dash).getAdvisoryLock(anyString(), anyInt());
        inOrder.verify(engine).computeHireBasisAccrual(eq(CMPNY), eq(TARGET_USER), eq(TODAY));
        inOrder.verify(dash).selectStatutoryGrantedLiveTotal(eq(CMPNY), eq(TARGET_USER));
        inOrder.verify(dash).insertManualGrant(any(LeaveGrantInsertVO.class));
        inOrder.verify(dash).releaseAdvisoryLock(anyString());
    }

    @Test
    @DisplayName("★SEC-P2-1: 상한 초과 거부(400_210) 경로에서도 락은 반드시 해제된다(finally 폴백)")
    void secP21_lockReleasedOnRejectionPath() {
        ApiException ex = assertThrows(ApiException.class,
                () -> svc.coverGrant(CMPNY, command(BigDecimal.valueOf(5)), AUTH_MASTER, OPERATOR));
        assertEquals(AttdErrorCode.ATTD_400_210, ex.getErrorCode());
        org.mockito.Mockito.verify(dash).releaseAdvisoryLock(anyString());
    }

    @Test
    @DisplayName("★P2R3-N1: coverGrant 는 @Transactional(isolation = READ_COMMITTED) 계약 고정 — "
            + "기본 REPEATABLE READ 로 되돌리면 락 앞 첫 SELECT(countActiveUser)가 read view 를 고정해 "
            + "락 안 상한 재계산이 stale 이 되고 GET_LOCK 직렬화가 실효를 잃는다(SmsRateLimitGuard 선례 미러)")
    void p2r3n1_coverGrantIsolationContract_readCommitted() throws NoSuchMethodException {
        org.springframework.transaction.annotation.Transactional tx = LeaveDashboardServiceImpl.class
                .getDeclaredMethod("coverGrant", String.class, CoverGrantCommand.class, String.class, String.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);

        assertTrue(tx != null, "coverGrant 에 @Transactional 이 없다 — 락 해제 afterCompletion 훅 전제 붕괴");
        assertEquals(org.springframework.transaction.annotation.Isolation.READ_COMMITTED, tx.isolation(),
                "coverGrant 격리수준이 READ_COMMITTED 가 아니다 — P2R3-N1(상한 TOCTOU) 즉시 재개방. "
                        + "advisory lock 과 한 쌍이므로 한쪽만 되돌리지 말 것(서비스 주석·AdvisoryLockTxUtils javadoc 참조)");
    }
}
