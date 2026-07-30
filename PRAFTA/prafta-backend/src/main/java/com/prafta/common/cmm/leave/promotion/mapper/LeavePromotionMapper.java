package com.prafta.common.cmm.leave.promotion.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.leave.promotion.vo.PromotionActiveContextVO;
import com.prafta.common.cmm.leave.promotion.vo.PromotionCandidateVO;
import com.prafta.common.cmm.leave.promotion.vo.PromotionLeaveUseInsertVO;
import com.prafta.common.cmm.leave.promotion.vo.PromotionLogInsertVO;

/**
 * 연차 사용촉진 판정·잔여·마스터 전용 Mapper (PRAFTA-COM-008-A-1/A-2).
 *
 * <p>위치: {@code com.prafta.common.cmm.leave.promotion} — 앱/웹 공용 도메인 영역(E 의
 * {@code common.cmm.sch} 관례). 모든 조회는 CMPNY_CD 스코프로 격리한다.
 *
 * <p>outbox INSERT / NOTI_ID 채번은 신규 SQL 난립 방지를 위해
 * {@code LeaveDashboardMapper.insertNotiOutbox} / {@code selectNextNotiId} 를 재사용한다(본 매퍼 미보유).
 * 본문 합성용 평문 USER_NM 은 {@code LeaveApprovalNotiMapper.selectUserNm} 를 재사용한다.
 *
 * <p>SQL 규칙: leading comma, {@code #{...}} 바인딩, SELECT * 금지(명시 컬럼). MySQL 8.
 */
@Mapper
public interface LeavePromotionMapper {

    /**
     * 촉진 도래 판정 후보 목록(전 회사 1패스 또는 회사별 스캔).
     *
     * <p>대상 = AXIS7_USE_PROMOTION='Y' 활성 정책 회사 + 활성 사용자(USE_YN='Y', ACCOUNT_STATUS='01')
     * 중 <b>ACTIVE STATUTORY_ANNUAL grant 보유자(=1년차 이상)</b>. 1년차 미만(월차만 보유)은
     * STATUTORY_ANNUAL 부재로 자연 제외된다(확정-1, MVP 1년차 이상만).
     *
     * <p>각 후보의 역산 기준 = 본연차(STATUTORY_ANNUAL) grant 중 가장 임박한 AVAIL_TO_DATE 1건
     * (resolvePromotionBaseGrant 단일 기준). 잔여 = 본연차+근속가산 ACTIVE (GRANT_DAYS-USED_DAYS) 합.
     *
     * <p>해당 회차(BASE_AVAIL_TO_DATE 동일) <b>FIRST 마스터 존재 여부(firstMasterYn)·최초 통지일
     * (firstNoticedDate)</b> 를 TB_LEAVE_PROMOTION_LOG LEFT JOIN 으로 1패스에 싣는다(구간 판정 입력).
     * 후보 루프 안 단건 조회(N+1)를 만들지 말 것.
     *
     * <p>도래 시점 판정(1차/2차 구간 판정 — 작업지시서 §4, D5·D6·D8)은 결정성을 위해 서비스 레이어에서
     * {@code today} 와 비교한다(SQL 에서 NOW()/날짜 연산 분기 금지).
     *
     * @return 회사·사용자 단위 후보 목록(BASE_AVAIL_TO_DATE/잔여/FIRST 마스터 메타 포함). 없으면 빈 리스트.
     */
    List<PromotionCandidateVO> selectPromotionCandidates();

    /**
     * 단건 사용자의 촉진 도래 판정 후보 재산정(도래 시점 교차체크용).
     *
     * <p>입사일/부여기준 변경으로 BASE_AVAIL_TO_DATE 가 바뀌었을 수 있어, 통지/지정 직전에
     * 동일 산식으로 재조회한다(§3-3 재산정). 미해당(1년차 미만/촉진 미사용/grant 부재)이면 null.
     * 목록 쿼리와 동일하게 회차 FIRST 마스터 메타(firstMasterYn/firstNoticedDate)를 함께 싣는다
     * (구간 판정 산식 동일성 유지).
     *
     * @param cmpnyCd 회사 코드 (CMPNY_CD 스코프)
     * @param userCd  대상 사용자 코드
     * @return 재산정 후보(없으면 null)
     */
    PromotionCandidateVO selectPromotionCandidate(@Param("cmpnyCd") String cmpnyCd,
                                                  @Param("userCd") String userCd);

    /** 촉진 진행 ID 채번. 'LP' + YYYYMMDD + 시퀀스(FNC_CMM_SEQ_NEXTVAL, SEQ_KEY='LEAVE_PROMOTION_ID'). */
    String selectNextPromoId(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 촉진 진행 마스터 1행 INSERT. 멱등(UNIQUE(CMPNY_CD, DEDUP_KEY)) — 중복 시
     * DuplicateKeyException 전파(서비스가 흡수). 1차 통지/2차 지정 공용.
     *
     * @return 신규 1
     */
    int insertPromotionLog(PromotionLogInsertVO vo);

    /**
     * 회차 마스터 행 존재 여부(멱등 사전 점검, DEDUP_KEY 기준). 1=존재(이미 통지/지정됨).
     *
     * @param cmpnyCd  회사 코드
     * @param dedupKey 회차 멱등 키
     */
    int countByDedupKey(@Param("cmpnyCd") String cmpnyCd,
                        @Param("dedupKey") String dedupKey);

    // ============================================================
    // A-3/A-4 공용 — 촉진 연차 등록 헬퍼 (leave_use INSERT + 마스터 갱신)
    // ============================================================

    /**
     * 촉진 연차 사용 1행 INSERT (PRAFTA-COM-008-A-3/A-4).
     *
     * <p>{@code LeaveFlowMapper.insertLeaveUse} 미러 + 촉진 마커(PROMOTION_STAGE/DESIGNATOR_TYPE/
     * ORIG_DESIGNATED_DATE)를 함께 기록한다. REQ_ID 는 항상 NULL(결재 없는 직접 차감), LEAVE_STATUS
     * ='CONFIRMED', USE_UNIT_TYPE='00'(종일), LEAVE_DAYS=1.0 고정. 동일 (USER|START_DATE|SYS_ANNUAL)
     * 직접차감 멱등키 충돌(UK_LEAVE_USE_DIRECT)이면 DuplicateKeyException 전파(서비스가 흡수).
     *
     * @return 신규 1
     */
    int insertPromotionLeaveUse(PromotionLeaveUseInsertVO vo);

    /**
     * 촉진 1차 자발 지정 일수 스냅샷 누적(앱 계획서 제출분). 회차 마스터(FIRST) 행에 가산한다.
     *
     * <p>대상 = (CMPNY_CD, DEDUP_KEY, PROMO_STAGE='FIRST', DEL_YN='N'). 마스터가 없으면 0행(무동작 —
     * 1차 통지 마스터가 선재한다는 전제. 통지 전 비정상 호출은 갱신 0).
     *
     * @return 갱신 행 수
     */
    int addStage1DesignatedDays(@Param("cmpnyCd") String cmpnyCd,
                                @Param("dedupKey") String dedupKey,
                                @Param("addDays") BigDecimal addDays,
                                @Param("operatorNo") String operatorNo);

    /**
     * 2차 직권지정 회차 마스터 멱등 upsert 보조 — 기존 2차 행(DESIG dedupKey)의 지정 상태 갱신.
     *
     * <p>대상 = (CMPNY_CD, DEDUP_KEY, PROMO_STAGE='SECOND', DEL_YN='N'). STATUS='DESIGNATED',
     * STAGE2_DESIGNATED_DATE=오늘. STAGE2_TARGET_DAYS 는 스케줄러가 도래 시 기록한 불변 스냅샷이라
     * 갱신하지 않는다(직권지정 누적 가산 시 cap/표시 오염 → H1 수정). 행이 없으면 0행 → 서비스가
     * 신규 INSERT(insertPromotionLog) 로 적재한다(A-2 보고 #5 규약).
     *
     * @return 갱신 행 수
     */
    int markStage2Designated(@Param("cmpnyCd") String cmpnyCd,
                             @Param("dedupKey") String dedupKey,
                             @Param("designatedDate") String designatedDate,
                             @Param("operatorNo") String operatorNo);

    /**
     * 앱 1차 계획서 컨텍스트 단건 조회(진행 중 1차 촉진). 미해당이면 null.
     *
     * <p>대상 = AXIS7='Y' + ACTIVE STATUTORY_ANNUAL 보유 + 잔여&gt;0 + FIRST 마스터(STATUS IN
     * ('NOTICED','COMPLETED')) 존재. 보유/잔여/기준 만료일/LOGIN_NOTIFIED_YN/기준 grant 를 싣는다.
     * (이미 등록된 촉진 연차일 목록은 별도 조회 — selectMyPromotionLeaveDates)
     */
    PromotionActiveContextVO selectActiveFirstContext(@Param("cmpnyCd") String cmpnyCd,
                                                      @Param("userCd") String userCd);

    /**
     * 사용자의 촉진 등록 연차일 목록(FIRST/SECOND, CONFIRMED, 기준 grant 차감분). 종료일 기준 정렬.
     *
     * <p>앱 active 응답의 "이미 등록된 연차일" + 웹 designate 직전 중복 표시에 사용한다. 기준
     * baseAvailToDate(=본연차 만료일) 안의 등록분만(역산 기준 회차 한정). PROMOTION_STAGE!='NONE'.
     */
    List<String> selectMyPromotionLeaveDates(@Param("cmpnyCd") String cmpnyCd,
                                             @Param("userCd") String userCd);

    /**
     * 로그인 안내 1회 노출 완료 플래그 갱신(LOGIN_NOTIFIED_YN='Y'). 진행 중 FIRST 마스터 대상(확정-3).
     *
     * @return 갱신 행 수
     */
    int markLoginNotified(@Param("cmpnyCd") String cmpnyCd,
                          @Param("userCd") String userCd,
                          @Param("operatorNo") String operatorNo);

    /**
     * 해당 일자에 근무 스케줄(tb_user_work_plan) 행이 있는지 카운트(교대자 등록 허용 판정).
     *
     * <p>교대팀 소속일이면 work_plan 즉석 생성을 하지 않으므로(교대패턴 우선, §3-2-1), 기존 근무일에만
     * 촉진 연차를 등록한다. 1=근무일 존재. work_plan PK=(CMPNY_CD,SITE_CD,USER_CD,WORK_YMD).
     */
    int countWorkPlanDay(@Param("cmpnyCd") String cmpnyCd,
                         @Param("siteCd") String siteCd,
                         @Param("userCd") String userCd,
                         @Param("workYmd") String workYmd);

    /**
     * 해당 일자에 출근 근태(tb_user_attd_mgmt) 행이 있는지 카운트(촉진 등록 상호배제 게이트, B-M1).
     *
     * <p>정책 §9.4 "연차 신청 조건 = 해당 일자에 출근 기록이 없을 것" — 근로자 신청 경로의
     * {@code AppLeaveFlowMapper.countAttendanceByDate} 와 동일 술어를 미러한다(신규 술어 난립 최소화).
     * 1=출근 근태 존재 → 촉진 연차 등록 거부(이관설계 §5-4 상호배제). 식별값은 토큰 도출값(IDOR).
     */
    int countAttendanceOn(@Param("cmpnyCd") String cmpnyCd,
                          @Param("siteCd") String siteCd,
                          @Param("userCd") String userCd,
                          @Param("workYmd") String workYmd);
}
