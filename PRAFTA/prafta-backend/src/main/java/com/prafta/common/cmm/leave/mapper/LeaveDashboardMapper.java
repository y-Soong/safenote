package com.prafta.common.cmm.leave.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.leave.vo.LeaveDashboardMetricsVO;
import com.prafta.common.cmm.leave.vo.LeaveDashboardRowVO;
import com.prafta.common.cmm.leave.vo.LeaveDetailUserVO;
import com.prafta.common.cmm.leave.vo.LeaveGrantHistoryRowVO;
import com.prafta.common.cmm.leave.vo.LeaveGrantInsertVO;
import com.prafta.common.cmm.leave.vo.LeaveRecallTargetVO;
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
     * 경력 인정 개월 합계 (tb_user_service_credit, USE_YN='Y'). 입사일 기준 부여 시 근속 가산용.
     */
    int selectCreditMonths(@Param("cmpnyCd") String cmpnyCd,
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
}
