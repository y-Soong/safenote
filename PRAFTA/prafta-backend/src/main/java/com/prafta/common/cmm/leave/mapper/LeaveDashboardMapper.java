package com.prafta.common.cmm.leave.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.leave.vo.AppliedLeaveTypeVO;
import com.prafta.common.cmm.leave.vo.LeaveDashboardMetricsVO;
import com.prafta.common.cmm.leave.vo.LeaveDashboardRowVO;
import com.prafta.common.cmm.leave.vo.LeaveDetailUserVO;
import com.prafta.common.cmm.leave.vo.LeaveGrantHistoryRowVO;
import com.prafta.common.cmm.leave.vo.LeaveGrantInsertVO;
import com.prafta.common.cmm.leave.vo.LeaveRecallTargetVO;
import com.prafta.common.cmm.leave.vo.LeaveTypeAvailTermVO;
import com.prafta.common.cmm.leave.vo.LeaveTypeOptionVO;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;

/**
 * 연차 현황 대시보드/상세/수동 부여(attd09) 전용 Mapper.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5
 *
 * <p>모든 조회/INSERT는 CMPNY_CD 스코프로 격리한다. 법정/법정외 구분은
 * {@code tb_user_leave_grant.GRANT_TYPE} prefix(STATUTORY_ / MANUAL_).
 * 활성 부여 정의: STATUS=ACTIVE AND DEL_YN=N.
 */
@Mapper
public interface LeaveDashboardMapper {

    // ============================================================
    // 대시보드 목록 / 메트릭
    // ============================================================

    /**
     * 대시보드 메트릭(전체 직원/평균 사용률 산출 합/소멸임박30/이번달 신규부여) 단건 집계.
     */
    LeaveDashboardMetricsVO selectDashboardMetrics(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 대시보드 직원 목록 페이징 조회.
     *
     * <p>정렬은 고정(잔여 적은순, USER_CD ASC tie-break)이며 동적 정렬 입력을 받지 않는다.
     *
     * @param cmpnyCd      회사 코드 (CMPNY_CD 스코프)
     * @param siteCd       사업장 코드 필터 (NULL/빈값이면 전체, CMPNY_CD 스코프 내)
     * @param nodeCd       소속부서 노드 코드 필터 (NULL/빈값이면 전체)
     * @param incSubNodeYn 하위부서 포함 여부 'Y'/'N' (Y면 nodeCd 서브트리 포함 — attd08 패턴)
     * @param userNm       사용자명 LIKE 검색어 (NULL/빈값이면 전체)
     * @param offset       페이징 offset
     * @param limit        페이징 limit
     */
    List<LeaveDashboardRowVO> selectDashboardList(@Param("cmpnyCd") String cmpnyCd,
                                                  @Param("siteCd") String siteCd,
                                                  @Param("nodeCd") String nodeCd,
                                                  @Param("incSubNodeYn") String incSubNodeYn,
                                                  @Param("userNm") String userNm,
                                                  @Param("offset") int offset,
                                                  @Param("limit") int limit);

    /**
     * 대시보드 직원 목록 전체 행 수 (페이징 메타).
     */
    long countDashboardList(@Param("cmpnyCd") String cmpnyCd,
                            @Param("siteCd") String siteCd,
                            @Param("nodeCd") String nodeCd,
                            @Param("incSubNodeYn") String incSubNodeYn,
                            @Param("userNm") String userNm);

    // ============================================================
    // 상세
    // ============================================================

    /**
     * 직원 상세 헤더정보 + 법정/법정외 집계 단건. 대상 직원이 없거나 스코프 밖이면 null.
     */
    LeaveDetailUserVO selectDetailUser(@Param("cmpnyCd") String cmpnyCd,
                                       @Param("userCd") String userCd);

    /**
     * 직원 부여 이력 목록 (GRANT_DATE 내림차순). DEL_YN='N' 전체 상태 포함.
     */
    List<LeaveGrantHistoryRowVO> selectGrantHistory(@Param("cmpnyCd") String cmpnyCd,
                                                    @Param("userCd") String userCd);

    /**
     * 신청형 휴가(사용자 신청 LEAVE_TYPE='01') 타입별 잔여 현황 조회 (연차 대시보드 상세 — 신청형 휴가 섹션).
     *
     * <p>대상 타입: {@code tb_leave_type_mgmt} WHERE CMPNY_CD AND LEAVE_TYPE='01' AND USE_YN='Y'.
     * 법정/관리자부여(02) 그룹과 합산하지 않고 타입별 별도 항목으로 반환한다.
     *
     * <p>타입별:
     * <ul>
     *   <li>한도(maxAplyDays) = MAX_APLY_DAYS (NULL 허용 — 서비스에서 0 fail-closed).</li>
     *   <li>사용(usedDays) = Σ TB_USER_LEAVE_USE.LEAVE_DAYS WHERE 동일 CMPNY_CD/USER_CD/LEAVE_CD
     *       AND LEAVE_STATUS='CONFIRMED' AND DEL_YN='N'
     *       AND START_DATE &gt;= fiscalStartYmd AND START_DATE &lt; fiscalEndYmdExclusive (당해 회계연도).</li>
     * </ul>
     * 잔여(remainDays)는 서비스 계층에서 한도−사용으로 산출한다(SQL은 한도/사용만 반환).
     *
     * <p>회계연도 경계({@code fiscalStartYmd}/{@code fiscalEndYmdExclusive})는 서비스에서
     * {@code FiscalYearUtils}로 산출해 {@code #{}}로 주입한다. CMPNY_CD/USER_CD 스코프 격리.
     *
     * @param cmpnyCd              회사 코드 (CMPNY_CD 스코프)
     * @param userCd               대상 직원 코드
     * @param fiscalStartYmd       당해 회계연도 시작일 (YYYYMMDD, inclusive)
     * @param fiscalEndYmdExclusive 당해 회계연도 종료 경계 (YYYYMMDD, exclusive)
     */
    List<AppliedLeaveTypeVO> selectAppliedLeaveTypes(@Param("cmpnyCd") String cmpnyCd,
                                                     @Param("userCd") String userCd,
                                                     @Param("fiscalStartYmd") String fiscalStartYmd,
                                                     @Param("fiscalEndYmdExclusive") String fiscalEndYmdExclusive);

    // ============================================================
    // 수동 부여
    // ============================================================

    /**
     * 수동 부여 가능 휴가 종류 옵션.
     * LEAVE_TYPE='02' AND GRANT_TYPE='02' AND USE_YN='Y' AND CMPNY_CD.
     */
    List<LeaveTypeOptionVO> selectManualGrantTypes(@Param("cmpnyCd") String cmpnyCd);

    /**
     * leaveCd가 수동 부여 가능 휴가 종류 화이트리스트에 속하는지 서버 재검증용 카운트.
     */
    int countManualGrantType(@Param("cmpnyCd") String cmpnyCd,
                             @Param("leaveCd") String leaveCd);

    /**
     * 대상 사용자가 본 회사 활성 사용자인지 검증용 카운트.
     */
    int countActiveUser(@Param("cmpnyCd") String cmpnyCd,
                        @Param("userCd") String userCd);

    /**
     * 수동 부여 타입의 "사용 가능 기간" 설정 단건 조회 (prafta-045, §8.1.1).
     *
     * <p>부여건 AVAIL_TO_DATE를 회사 공통 AXIS6가 아니라 해당 타입의
     * {@code ADMIN_AVAIL_TERM_TYPE}(SYS026 01/02/03) + {@code ADMIN_AVAIL_FROM_DT}/{@code ADMIN_AVAIL_TO_DT}
     * (YYYYMMDD 8자, prafta-044-FU2)로 산출하기 위해 조회한다. 수동 부여 타입은 항상 관리자 부여
     * 타입(LEAVE_TYPE='02' AND GRANT_TYPE='02')이므로 관리자 전용 컬럼만 읽는다.
     *
     * <p>CMPNY_CD 스코프로 격리한다(타사 타입 avail-term 조회 차단). 미존재/스코프 밖이면 null.
     */
    LeaveTypeAvailTermVO selectAdminAvailTerm(@Param("cmpnyCd") String cmpnyCd,
                                              @Param("leaveCd") String leaveCd);

    /**
     * GRANT_ID 채번. 'G' + YYYYMMDD + 시퀀스(FNC_CMM_SEQ_NEXTVAL, SEQ_KEY='LEAVE_GRANT_ID').
     */
    String selectNextGrantId(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 수동 부여 1건 INSERT.
     */
    int insertManualGrant(LeaveGrantInsertVO vo);

    // ============================================================
    // 수동 부여 연차 회수 (soft cancel, PRAFTA-031)
    // ============================================================

    /**
     * 회수 대상 부여행 단건 조회(회사 스코프). 없거나 스코프 밖이면 null.
     * 서버 재검증용 최소 컬럼(USER_CD/LEAVE_CD/GRANT_TYPE/GRANT_BY_TYPE/GRANT_DAYS/USED_DAYS/STATUS/DEL_YN).
     */
    LeaveRecallTargetVO selectRecallTarget(@Param("cmpnyCd") String cmpnyCd,
                                           @Param("grantId") String grantId);

    /**
     * 수동 부여 연차 회수(soft cancel) UPDATE. STATUS='CANCELED' 전환 + 회수 메타(사유/일시/수행자) 기록.
     *
     * <p>정책서 §8.5.8: <b>USED_DAYS는 갱신하지 않는다</b>(사용분 보존). EXPIRE_YN/DEL_YN도 'N' 유지(행 보존).
     * WHERE를 grantId + cmpnyCd + STATUS='ACTIVE' + DEL_YN='N'으로 못박아 동시성/조건을 재확인한다.
     *
     * @return 회수된 행 수(정상 1, 경합으로 조건 불일치 시 0)
     */
    int recallGrant(@Param("cmpnyCd") String cmpnyCd,
                    @Param("grantId") String grantId,
                    @Param("reason") String reason,
                    @Param("operatorUserCd") String operatorUserCd);

    /**
     * NOTI_ID 채번. 'N' + YYYYMMDD + 시퀀스(FNC_CMM_SEQ_NEXTVAL, SEQ_KEY='NOTI_OUTBOX_ID').
     */
    String selectNextNotiId(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 알림 outbox 1건 INSERT(SEND_STATUS='PENDING'). 회수 완료 시 추후 모바일 push용 적재.
     */
    int insertNotiOutbox(NotiOutboxInsertVO vo);

    /**
     * CANCELED 단건을 ACTIVE로 부활(reactivate)시킨다 (옵션 A — prafta-029 후속 버그 수정).
     *
     * <p>RESET_ALL이 만든 CANCELED 표준키 1건을 정확히 그 (CMPNY_CD, IDEMPOTENCY_KEY)로만 ACTIVE 복원하며
     * GRANT_DATE / AVAIL_FROM_DATE / AVAIL_TO_DATE / GRANT_DAYS / POLICY_SEQ / GRANT_REASON / GRANT_BY_TYPE을
     * 재부여 값으로 갱신한다.
     * <b>USED_DAYS는 건드리지 않는다</b>(§8.5.8 #2 사후차감 금지 — 사용분 보존). LEAVE_CD/GRANT_TYPE도 미변경.
     *
     * <p><b>보안</b>: CANCELED→ACTIVE 부활의 유일 경로다. WHERE를 정확한 단건 멱등키 + STATUS='CANCELED' +
     * DEL_YN='N'으로 못박아 범위/LIKE 부활을 배제한다. {@link LeaveGrantStatusMapper#updateStatusWithSync}의
     * CANCELED 부활 차단 가드를 우회하지 않고 본 전용 메서드로만 부활한다.
     *
     * @return 부활된 행 수(정상 1, 경합으로 이미 없으면 0)
     */
    int reactivateCanceledGrant(LeaveGrantInsertVO vo);

    /**
     * 해당 멱등키의 CANCELED 단건 GRANT_ID를 반환한다(없으면 null). 부활(reactivate) 분기 진입 판정용.
     */
    String selectCanceledGrantIdByKey(@Param("cmpnyCd") String cmpnyCd,
                                      @Param("idempotencyKey") String idempotencyKey);

    // ============================================================
    // 입사일 기준 연차 부여 (테스트/검증용)
    // ============================================================

    /**
     * 대상 사용자의 입사일(HIRE_DATE, YYYYMMDD). 활성 사용자만, 스코프 밖/미존재면 null.
     */
    String selectUserHireDate(@Param("cmpnyCd") String cmpnyCd,
                              @Param("userCd") String userCd);

    /**
     * 멱등성 키로 기존 부여행 존재 카운트 (재실행 시 중복 부여 차단 — 정책서 §8.5.8).
     *
     * <p><b>주의(prafta-029 옵션 A)</b>: 이 카운트는 STATUS/DEL_YN을 필터하지 않아 CANCELED 행도 함께 센다.
     * 따라서 "live(부활 대상이 아닌 점유)" 판정에는 {@link #countLiveByIdempotencyKey}를 사용한다.
     * 본 메서드는 기존 테스트 스텁 보호를 위해 시그니처/SQL을 변경하지 않는다.
     */
    int countByIdempotencyKey(@Param("cmpnyCd") String cmpnyCd,
                              @Param("idempotencyKey") String idempotencyKey);

    /**
     * 멱등성 키로 <b>live</b> 부여행 존재 카운트 (옵션 A — prafta-029 후속 버그 수정).
     *
     * <p>live = {@code STATUS != 'CANCELED' AND DEL_YN = 'N'}. CANCELED 행은 멱등키를 점유하지 않은 것으로
     * 본다(옵션 A). RESET_ALL이 표준키를 CANCELED로 만든 뒤 KEEP_AND_BACKFILL/APPLY_NEW이 같은 기간을
     * 다시 부여(reactivate)할 수 있도록, 기부여 판정은 이 메서드(live-only)로 한다.
     */
    int countLiveByIdempotencyKey(@Param("cmpnyCd") String cmpnyCd,
                                  @Param("idempotencyKey") String idempotencyKey);

    /**
     * 멱등성 키로 ACTIVE 부여행 존재 카운트 (prafta-023 #1 — 월차 집계↔per-월 상호배타:
     * 해당 연도에 ACTIVE 집계 월차 보유 시 per-월 부여를 건너뛰기 위한 판정).
     */
    int countActiveByIdempotencyKey(@Param("cmpnyCd") String cmpnyCd,
                                    @Param("idempotencyKey") String idempotencyKey);

    /**
     * 같은 (기간·종류)에 대해 회차 접미사({@code _R{HIST_ID}})/{@code _HIRE} 등 '변형 키'로 부여된
     * ACTIVE 행 카운트 (prafta-029, RESET_ALL 회차키 누수 차단).
     *
     * <p>baseKey = {@code "{USER_CD}_{periodLabel}_{grantType}"}(접미사 없는 표준 키). baseKey 의 언더스코어를
     * 이스케이프(literal)하고 그 뒤에 {@code "_접미사"}가 붙은 키만 매칭한다(표준키 자기 자신은 제외).
     * RESET_ALL이 {@code _R{HIST_ID}} 회차키로 재발급한 기간을 직후 표준키 클릭(APPLY_NEW)이
     * 다시 부여하던 분할/이중부여 누수를 차단하는 데 쓴다.
     */
    int countActiveBySuffixVariant(@Param("cmpnyCd") String cmpnyCd,
                                   @Param("baseKey") String baseKey);

    /**
     * 부여에 사용할 시스템 연차 종류(LEAVE_CD)가 회사에 존재/사용중인지 카운트.
     */
    int countLeaveTypeExists(@Param("cmpnyCd") String cmpnyCd,
                             @Param("leaveCd") String leaveCd);

    /**
     * 경력 인정 개월 합계 (tb_user_service_credit, USE_YN='Y' AND LEAVE_CALC_YN='Y'). 입사일 기준 부여 시 근속 가산용.
     * 경력인정 이원화(2026-08-21, 지시서 §1-2): 일수 모드(LEAVE_CALC_YN='N')는 산정근속 미가산(정책 P-7)이라 제외.
     */
    int selectCreditMonths(@Param("cmpnyCd") String cmpnyCd,
                           @Param("userCd") String userCd);

    /**
     * 일수 모드 경력인정 연간 추가 부여 일수 합계 (tb_user_service_credit, USE_YN='Y' AND LEAVE_CALC_YN='N').
     * 경력인정 이원화(2026-08-21, 지시서 §1-4) — MANUAL_CAREER 연간 자동 부여량 산정용.
     *
     * @return 활성 일수 모드 credit 행들의 EXTRA_LEAVE_DAYS 합(없으면 0)
     */
    java.math.BigDecimal selectExtraLeaveDaysSum(@Param("cmpnyCd") String cmpnyCd,
                                                 @Param("userCd") String userCd);

    /**
     * 입사일 변경 차액 보전(prafta-030 BE-1)용 "기존 부여누적" 합계 (정정 2026-05-26 / 결정문서 D1).
     *
     * <p>live(STATUS!='CANCELED' AND DEL_YN='N')인 <b>전 STATUTORY 유형(GRANT_TYPE LIKE 'STATUTORY\_%',
     * 월차 포함)</b>에 대해 <b>"소멸 제외 + 사용 포함"</b>으로 합산한다.
     * 각 행 기여 = {@code USED_DAYS + (AVAIL_TO_DATE >= today ? GRANT_DAYS - USED_DAYS : 0)}.
     * <ul>
     *   <li>사용분(USED_DAYS)은 이미 소멸했더라도 "혜택 제공분"이라 항상 누적에 포함(정답표 §2.1).</li>
     *   <li>미사용 잔여분은 소멸일(AVAIL_TO_DATE)이 today 이상일 때만(=아직 유효) 포함, 소멸분은 제외.</li>
     *   <li>월차(STATUTORY_MONTHLY)를 포함해야 경계B(기존 월차 누적 7 차감 → 차액 +8)가 맞다. 월차를
     *       빼면 경계B가 +15로 과다부여된다.</li>
     * </ul>
     *
     * @param cmpnyCd 회사 코드 (CMPNY_CD 스코프)
     * @param userCd  대상 직원 코드
     * @param today   기준일(YYYYMMDD) — 소멸 판정용. 엔진은 {@code ctx.today}를 전달한다.
     * @return live 전 STATUTORY "소멸 제외 + 사용 포함" 누적(없으면 0)
     */
    java.math.BigDecimal selectStatutoryGrantAccrual(@Param("cmpnyCd") String cmpnyCd,
                                                     @Param("userCd") String userCd,
                                                     @Param("today") String today);

    /**
     * 입사일 변경 옵션별 미리보기(prafta-030 BE-3)용 "기존 본연차+가산 누적" 합계 (월차 제외).
     *
     * <p>{@link #selectStatutoryGrantAccrual}과 <b>완전히 동일한 산식</b>(USED 포함 + 유효잔여)이되 대상 유형을
     * <b>본연차/근속가산만</b>(GRANT_TYPE IN ('STATUTORY_ANNUAL','STATUTORY_TENURE_BONUS'))으로 좁혀 월차
     * (STATUTORY_MONTHLY)를 제외한다. 옵션별 finalHold를 "본연차/가산"과 "월차"로 분리 산정하기 위함이다.
     * 각 행 기여 = {@code USED_DAYS + (AVAIL_TO_DATE >= today ? GRANT_DAYS - USED_DAYS : 0)}.
     *
     * <p>live = {@code STATUS != 'CANCELED' AND DEL_YN = 'N'}.
     *
     * @param cmpnyCd 회사 코드 (CMPNY_CD 스코프)
     * @param userCd  대상 직원 코드
     * @param today   기준일(YYYYMMDD) — 소멸 판정용
     * @return live 본연차+가산 "소멸 제외 + 사용 포함" 누적(없으면 0)
     */
    java.math.BigDecimal selectStatutoryAnnualTenureAccrual(@Param("cmpnyCd") String cmpnyCd,
                                                            @Param("userCd") String userCd,
                                                            @Param("today") String today);

    // ============================================================
    // 연차 가불(마이너스/이월) 코어 (prafta-com-011-1)
    //   가불 마커 = GRANT_REASON LIKE '[가불]%' (멱등키 밖, plan §0-1). GRANT_BY_TYPE='01' 재사용.
    //   live = STATUS != 'CANCELED' AND DEL_YN = 'N'.
    // ============================================================

    /**
     * 이미 가불한(아직 미발생) 일수 합계 (prafta-com-011 QA D1/D2 통일 모델, read-only). 누적 가불 한도 산정(결정 §2/§6-2)용.
     *
     * <p>{@code GRANT_REASON LIKE '[가불]%'} 인 live(STATUS!='CANCELED' AND DEL_YN='N') GRANT 중,
     * {@code grantTypePrefix} 로 시작하는 {@code GRANT_TYPE}(예: 월차 'STATUTORY_MONTHLY', 본연차 'STATUTORY_ANNUAL'),
     * 그리고 <b>아직 발생 전(AVAIL_FROM_DATE &gt; today)</b>인 GRANT 의 <b>USED_DAYS</b> 합을 반환한다(통일 모델 §6-2).
     * 가불 GRANT 는 전량(1.0/차기 전량)으로 생성되고 가불 사용분만 USED 로 차감되므로, "이미 당겨쓴 일수"는 GRANT_DAYS 가
     * 아니라 USED_DAYS 합이다. 발생일이 도래(AVAIL_FROM &lt;= today)한 GRANT 는 정식 부여로 전환된 것이라 한도에서 제외한다
     * (그 개월/회차분은 발생분 카운트로 별도 반영됨 — 이중 차감 방지). 가불 한도 = 전량 − 이 값.
     *
     * @param cmpnyCd         회사 코드 (CMPNY_CD 스코프)
     * @param userCd          대상 직원 코드
     * @param grantTypePrefix 집계 대상 GRANT_TYPE prefix(LIKE '{prefix}%')
     * @param today           기준일(YYYYMMDD) — AVAIL_FROM_DATE &gt; today(미발생) 필터
     * @return live 미발생 가불 USED 일수 합(없으면 0)
     */
    java.math.BigDecimal selectBorrowedDaysTotal(@Param("cmpnyCd") String cmpnyCd,
                                                 @Param("userCd") String userCd,
                                                 @Param("grantTypePrefix") String grantTypePrefix,
                                                 @Param("today") String today);

    /**
     * 멱등키로 기존 live 가불 GRANT 의 잔여 capacity 조회 (prafta-com-011 QA D1/D2 통일 모델, 누적 가불 §6-2).
     *
     * <p>같은 슬롯/회차의 가불 멱등키({@code {userCd}_{periodLabel}_{grantType}})로 이미 생성된 가불 GRANT
     * ({@code GRANT_REASON LIKE '[가불]%'} + STATUS='ACTIVE' + DEL_YN='N')가 있으면 grantId/GRANT_DAYS/USED_DAYS 를
     * 반환한다. 없으면 null. 누적 가불 시 신규 슬롯 INSERT 대신 이 GRANT 의 잔여(GRANT_DAYS−USED_DAYS)에 leave_use 를
     * 추가(USED 증액)하여 충당한다.
     *
     * @param cmpnyCd        회사 코드 (CMPNY_CD 스코프)
     * @param idempotencyKey 가불 멱등키
     * @return 기존 가불 GRANT capacity(없으면 null)
     */
    com.prafta.common.cmm.leave.vo.BorrowGrantCapacityVO selectBorrowGrantByKey(
            @Param("cmpnyCd") String cmpnyCd,
            @Param("idempotencyKey") String idempotencyKey);

    /**
     * 반려/취소 시 회수 대상 가불 GRANT_ID 목록 (prafta-com-011-1).
     *
     * <p>해당 {@code reqId} 의 leave_use(차감 해제 후)가 가리키는 GRANT_ID 중,
     * 가불 마커({@code GRANT_REASON LIKE '[가불]%'}) + ACTIVE + DEL_YN='N' + <b>USED_DAYS=0</b>(차감 복원됨) 인
     * 행만 회수 후보로 반환한다. 비가불 GRANT/사용분 잔존 GRANT 는 제외(§8.5.8 기부여보호).
     *
     * <p>호출부는 leave_use 차감 해제(recomputeGrantUsedDays 등)를 <b>먼저</b> 수행해 USED_DAYS=0 으로 만든 뒤
     * 본 조회를 한다(prafta-com-011-2 흐름).
     *
     * @param cmpnyCd 회사 코드 (CMPNY_CD 스코프)
     * @param reqId   반려된 연차 요청 ID
     * @return 회수 대상 가불 GRANT_ID 목록(없으면 빈 목록)
     */
    List<String> selectBorrowGrantIdsForCancel(@Param("cmpnyCd") String cmpnyCd,
                                               @Param("reqId") String reqId);

    /**
     * 가불 GRANT 1건 회수(soft cancel) UPDATE (prafta-com-011-1). STATUS='CANCELED' + 회수 메타.
     *
     * <p>WHERE 를 grantId + cmpnyCd + STATUS='ACTIVE' + DEL_YN='N' + USED_DAYS=0 + 가불 마커
     * ({@code GRANT_REASON LIKE '[가불]%'})로 못박아, 비가불/사용분 잔존/경합 행을 절대 취소하지 않는다.
     * USED_DAYS 는 갱신하지 않는다(§8.5.8).
     *
     * @return 회수된 행 수(정상 1, 조건 불일치/경합 시 0)
     */
    int cancelBorrowGrant(@Param("cmpnyCd") String cmpnyCd,
                          @Param("grantId") String grantId,
                          @Param("operatorUserCd") String operatorUserCd);

    /**
     * LC-07(표기): 사용자의 시간차(SYS025 02/03/04) CONFIRMED 사용 분 합계(전 기간, DEL_YN='N').
     *
     * <p>FE 가 "시간차 사용 N시간 M분" 원본 표기를 조립할 수 있게 하는 표기 전용 값이다
     * (차감 일수 합계와 별개 — 잔여/부여 수치엔 영향 없음). 대상 0건이면 0(IFNULL).
     *
     * @return 시간차 사용 분 합계(분)
     */
    Integer selectHourlyUsedMinutes(@Param("cmpnyCd") String cmpnyCd,
                                    @Param("userCd") String userCd);
}
