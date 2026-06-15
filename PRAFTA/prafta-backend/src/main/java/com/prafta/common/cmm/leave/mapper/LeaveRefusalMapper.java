package com.prafta.common.cmm.leave.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.leave.vo.RefusalLogInsertVO;
import com.prafta.common.cmm.leave.vo.RefusalTargetVO;

/**
 * 노무수령거부 차단 판정/이력/알림 공용 Mapper (PRAFTA-COM-001 → -008-B 차단 전환).
 *
 * <p>출퇴근·근태 등록 진입부 차단 가드({@code LeaveRefusalDetectService.guardAndRecord})가 공용으로 사용한다.
 * com-008-B-4 에서 web 기능1(통지) 및 구 detect 자산({@code selectRefusalTarget}/{@code countValidTarget})은 제거되었다.
 * app 이 web 을 호출하지 않는 원칙(앱/웹 분리)에 따라 공용 영역인
 * {@code com.prafta.common.cmm.leave} 에 둔다.
 *
 * <p>모든 조회/INSERT 는 CMPNY_CD 스코프로 격리한다. outbox INSERT / NOTI_ID 채번은
 * 신규 SQL 난립 방지를 위해 {@link LeaveDashboardMapper#insertNotiOutbox}/
 * {@link LeaveDashboardMapper#selectNextNotiId} 를 재사용한다(본 매퍼는 미보유).
 */
@Mapper
public interface LeaveRefusalMapper {

    /**
     * REFUSAL_ID 채번. 'LR' + YYYYMMDD + 시퀀스(FNC_CMM_SEQ_NEXTVAL, SEQ_KEY='LEAVE_REFUSAL_ID').
     */
    String selectNextRefusalId(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 노무수령거부 차단 대상일 판정 (com-008-B §3, detect→block 전환).
     *
     * <p>판정 소스를 NOTICED 로그 → {@code tb_user_leave_use} 촉진단계로 전환했다. 다음 3게이트를
     * 모두 통과하는 종일 CONFIRMED 연차 1건을 반환(없으면 null = 차단 대상 아님):
     * <ol>
     *   <li>[촉진] (USER_CD, START_DATE=targetYmd, LEAVE_STATUS='CONFIRMED', DEL_YN='N',
     *       USE_UNIT_TYPE='00') + PROMOTION_STAGE ∈ {FIRST,SECOND} (자발 NONE 제외).</li>
     *   <li>[게이트2 법정] 연결 grant {@code GRANT_TYPE LIKE 'STATUTORY_%' AND <> 'STATUTORY_MONTHLY'}
     *       (본연차 + 근속가산만, 월차 배제).</li>
     *   <li>[게이트1 휴일] 시도 당일이 휴일이 아님(tb_holiday 일자 ∪ tb_holiday_rule MM/DD NOT EXISTS).</li>
     * </ol>
     * 반환 VO 의 leaveId 는 BLOCKED 이력의 RELATED_LEAVE_ID 로 사용된다.
     *
     * @param cmpnyCd   회사 코드 (CMPNY_CD 스코프)
     * @param siteCd    사업장 코드
     * @param userCd    대상 근로자 코드
     * @param targetYmd 시도 근무일(YYYYMMDD) = 판정 대상일 = leave_use.START_DATE
     */
    RefusalTargetVO selectLaborRefusalTarget(@Param("cmpnyCd") String cmpnyCd,
                                             @Param("siteCd") String siteCd,
                                             @Param("userCd") String userCd,
                                             @Param("targetYmd") String targetYmd);

    /**
     * 사실 로그 1건 append (NOTICED / CHECKIN_DETECTED / ADMIN_ALERTED 공용).
     * dedup UNIQUE(CMPNY_CD, DEDUP_KEY) 충돌 시 UPDATE_DATE 만 갱신(멱등, row alias 문법).
     *
     * @return 신규 적재 시 1, 중복(이미 기록됨) 시 2(ON DUPLICATE KEY UPDATE) — 호출부는 값에 의존하지 않는다.
     */
    int insertRefusalLog(RefusalLogInsertVO vo);

    /**
     * 노무수령거부 알림 대상 관리자 USER_CD 목록 (기능3).
     *
     * <p>대상 = (1) 역할 master/hr (해당 회사/사업장, tb_user_site_auth ∩ AUTH_CD)
     *        ∪ (2) 대상 근로자 소속 노드(NODE_CD)의 main/sub 관리자(tb_site_node).
     * USER_CD 기준 DISTINCT, 사용중 계정(USE_YN='Y', ACCOUNT_STATUS='01')만.
     *
     * @param cmpnyCd     회사 코드 (CMPNY_CD 스코프)
     * @param siteCd      사업장 코드
     * @param userCd      대상 근로자 코드 (소속 노드 어드민 산출용)
     * @param authCdList  역할 기반 관리자 AUTH_CD 목록 (master/hr)
     */
    List<String> selectSiteRefusalAdmins(@Param("cmpnyCd") String cmpnyCd,
                                         @Param("siteCd") String siteCd,
                                         @Param("userCd") String userCd,
                                         @Param("authCdList") List<String> authCdList);
}
