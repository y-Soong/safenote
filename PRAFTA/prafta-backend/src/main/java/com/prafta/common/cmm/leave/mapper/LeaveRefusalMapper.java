package com.prafta.common.cmm.leave.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.leave.vo.RefusalLogInsertVO;
import com.prafta.common.cmm.leave.vo.RefusalTargetVO;

/**
 * 노무수령거부 통지/감지/알림 공용 Mapper (PRAFTA-COM-001).
 *
 * <p>web 기능1(통지 발송)과 app 기능2/3(출근 감지·관리자 PUSH)이 공용으로 사용한다.
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
     * 통지 대상 정합성 검증 (기능1 IDOR 가드).
     * 대상(userCd)이 호출자 회사(cmpnyCd) 소속의 실재·활성 사용자이며 siteCd 와 정합인지 확인.
     * 활성 조건은 {@link #selectSiteRefusalAdmins} 와 동일하게 USE_YN='Y', ACCOUNT_STATUS='01'.
     * SITE_CD 는 TB_USER 의 직접 컬럼으로 대조한다.
     *
     * @return 정합·실재·활성이면 1, 아니면 0
     */
    int countValidTarget(@Param("cmpnyCd") String cmpnyCd,
                         @Param("siteCd") String siteCd,
                         @Param("userCd") String userCd);

    /**
     * 노무수령거부 대상일 판정 (기능2). tb_leave_refusal_log 의 NOTICED 행이 존재하고
     * 그 대상일이 휴일(tb_holiday 일자휴일 / tb_holiday_rule 매년 MMDD)이 아닐 때만 1건 반환.
     * 대상이 아니면(미통지 또는 휴일) null.
     *
     * @param cmpnyCd   회사 코드 (CMPNY_CD 스코프)
     * @param siteCd    사업장 코드
     * @param userCd    대상 근로자 코드
     * @param targetYmd 출근 근무일(YYYYMMDD) = 판정 대상일
     */
    RefusalTargetVO selectRefusalTarget(@Param("cmpnyCd") String cmpnyCd,
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
