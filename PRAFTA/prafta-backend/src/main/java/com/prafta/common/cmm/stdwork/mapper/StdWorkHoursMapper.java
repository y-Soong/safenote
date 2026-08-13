package com.prafta.common.cmm.stdwork.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.stdwork.command.StdWorkHoursSaveCommand;
import com.prafta.common.cmm.stdwork.vo.StdWorkHoursVO;
import com.prafta.common.cmm.stdwork.vo.StdWorkPolicyVO;
import com.prafta.common.cmm.stdwork.vo.StdWorkReasonRuleVO;
import com.prafta.common.cmm.stdwork.vo.StdWorkUserScopeVO;

/**
 * 소정-02: 근로자별 소정근로시간(TB_USER_STD_WORK_HOURS) 공용 Mapper.
 *
 * <p>웹/앱 어느 모듈에서도 호출 가능한 계약량 축이므로 {@code com.prafta.common.cmm.stdwork}
 * 에 둔다. 모든 쿼리는 CMPNY_CD 술어를 필수로 가진다(멀티테넌시).
 *
 * <p>식별값(cmpnyCd/userCd)은 호출부에서 토큰 도출값으로 전달한다(IDOR 차단).
 */
@Mapper
public interface StdWorkHoursMapper {

    /** DB 서버 NOW() 기준 오늘 일자(YYYYMMDD). JVM 시계 스큐 방지 — WorktimeGateMapper 선례. */
    String selectTodayYmd();

    /**
     * 기준일에 유효한 소정근로 이력 1건.
     *
     * <p>유효 = {@code APPLY_STR_DATE <= 기준일} 이고 {@code APPLY_END_DATE} 가 NULL 이거나
     * 기준일 이상(양 끝 당일 포함). 데이터 정합이 깨져 2건 이상 걸리면 최신 적용 시작일 1건.
     *
     * @return 유효 행. 이력 미입력 계정이면 null (서비스가 회사 기준값으로 폴백)
     */
    StdWorkHoursVO selectEffectiveRow(@Param("cmpnyCd") String cmpnyCd,
                                      @Param("userCd") String userCd,
                                      @Param("baseYmd") String baseYmd);

    /**
     * 소정근로 이력 전체 (적용 시작일 내림차순 — 화면 타임라인용).
     */
    List<StdWorkHoursVO> selectHistory(@Param("cmpnyCd") String cmpnyCd,
                                       @Param("userCd") String userCd);

    /** 적용 시작일이 정확히 일치하는 이력 1건 (PK 조회). 없으면 null. */
    StdWorkHoursVO selectRowByApplyStrDate(@Param("cmpnyCd") String cmpnyCd,
                                           @Param("userCd") String userCd,
                                           @Param("applyStrDate") String applyStrDate);

    /**
     * 신규 구간과 겹치는 기존 이력 건수.
     *
     * <p>겹침 판정에서 <b>자동 마감 대상</b>(APPLY_END_DATE IS NULL 이면서 시작일이 신규
     * 시작일보다 이른 행)은 제외한다 — 그 행은 등록 시 전일로 마감되므로 겹침이 아니다.
     *
     * @param excludeStrDate  정정(correct) 시 자기 자신 행 제외용 적용 시작일 (등록 시 null)
     * @param excludeStrDate2 함께 이동될 복귀 행 제외용 적용 시작일 (해당 없으면 null)
     */
    int countOverlap(@Param("cmpnyCd") String cmpnyCd,
                     @Param("userCd") String userCd,
                     @Param("applyStrDate") String applyStrDate,
                     @Param("applyEndDate") String applyEndDate,
                     @Param("excludeStrDate") String excludeStrDate,
                     @Param("excludeStrDate2") String excludeStrDate2);

    /**
     * 마감 대상이 될 직전 열린 이력 행 1건 (APPLY_END_DATE IS NULL, 시작일 &lt; 신규 시작일).
     *
     * <p>단축 사유(유한 기간) 등록 시 "단축 종료 후 복귀 행"이 승계할 값(주 소정근로분·사유·
     * 사유 상세)의 출처다. 마감 전에 읽어야 하므로 {@link #closeOpenRowBefore} 보다 먼저 호출한다.
     *
     * @return 직전 열린 행. 없으면 null (이력이 없던 계정 = 복귀 행 생성 대상 아님)
     */
    StdWorkHoursVO selectOpenRowBefore(@Param("cmpnyCd") String cmpnyCd,
                                       @Param("userCd") String userCd,
                                       @Param("applyStrDate") String applyStrDate);

    /**
     * 직전 열린 이력 행(APPLY_END_DATE IS NULL, 시작일 &lt; 신규 시작일)을 전일로 마감한다.
     *
     * @param closeYmd 마감할 종료일 (= 신규 적용 시작일의 전일)
     * @return 마감된 행 수 (0 = 마감 대상 없음)
     */
    int closeOpenRowBefore(@Param("cmpnyCd") String cmpnyCd,
                           @Param("userCd") String userCd,
                           @Param("applyStrDate") String applyStrDate,
                           @Param("closeYmd") String closeYmd,
                           @Param("updateNo") String updateNo);

    /** 소정근로 이력 1행 INSERT. */
    int insertRow(@Param("cmd") StdWorkHoursSaveCommand cmd);

    /** 동일 적용 시작일 행의 값 정정 UPDATE (오입력 정정 전용 — 과거 값 덮어쓰기 아님). */
    int updateRow(@Param("cmd") StdWorkHoursSaveCommand cmd);

    /**
     * 복귀 행 이동 전용 DELETE.
     *
     * <p>PK 에 APPLY_STR_DATE 가 포함되어 시작일 변경을 UPDATE 로 할 수 없다.
     * 반드시 같은 트랜잭션에서 {@link #insertMovedRow} 와 쌍으로 사용한다.
     */
    int deleteRow(@Param("cmpnyCd") String cmpnyCd,
                  @Param("userCd") String userCd,
                  @Param("applyStrDate") String applyStrDate);

    /**
     * 복귀 행 이동 전용 INSERT ({@link #deleteRow} 와 쌍).
     *
     * <p>원 행의 값 전량을 승계하되 적용 시작일만 새 위치로 바꾼 VO 를 넘긴다.
     * INSERT_NO/INSERT_DATE 는 원 행 값을 보존하고, 이동 작업자는 UPDATE_NO 에 기록된다.
     *
     * @param row      새 적용 시작일이 세팅된 이동 대상 행
     * @param updateNo 이동을 수행한 작업자
     */
    int insertMovedRow(@Param("row") StdWorkHoursVO row,
                       @Param("updateNo") String updateNo);

    /**
     * 통상근로자 주 소정근로 기준값 1행 — <b>사업장 오버라이드 우선</b>(TB_CMPNY_STD_WORK_POLICY).
     *
     * <p>{@code siteCd} 가 있으면 {@code SITE/siteCd} 행을, 없으면 {@code COMPANY/'-'} 행을
     * 우선 반환한다(정렬로 1건 고정). 어느 스코프에서 나왔는지는 반환 VO 의 SCOPE_TYPE 으로
     * 판별한다.
     *
     * @param siteCd 대상 사업장. null/빈 값이면 회사 기본값만 조회한다.
     * @return 기준값 행. 둘 다 없으면 null (서비스가 코드 상수 2400 으로 폴백)
     */
    StdWorkPolicyVO selectEffectivePolicy(@Param("cmpnyCd") String cmpnyCd,
                                          @Param("siteCd") String siteCd);

    /**
     * 특정 스코프의 기준값 행 1건 (폴백 없음 — 화면이 "직접 지정 여부"를 그리는 데 쓴다).
     *
     * @return 해당 스코프 행. 없으면 null (= 상위 스코프 상속)
     */
    StdWorkPolicyVO selectPolicy(@Param("cmpnyCd") String cmpnyCd,
                                 @Param("scopeType") String scopeType,
                                 @Param("scopeCd") String scopeCd);

    /** 기준값 행 upsert (PK = CMPNY_CD + SCOPE_TYPE + SCOPE_CD). */
    int upsertPolicy(@Param("cmpnyCd") String cmpnyCd,
                     @Param("scopeType") String scopeType,
                     @Param("scopeCd") String scopeCd,
                     @Param("weekStdMinutes") int weekStdMinutes,
                     @Param("actorNo") String actorNo);

    /** 기준값 행 삭제 (= 상위 스코프 상속으로 되돌림). */
    int deletePolicy(@Param("cmpnyCd") String cmpnyCd,
                     @Param("scopeType") String scopeType,
                     @Param("scopeCd") String scopeCd);

    /**
     * 대상 사용자의 고용형태 + 소속 사업장. USE_YN='Y' 인 계정만 조회한다.
     *
     * <p>★EMPLOYMENT_TYPE 은 NULL 허용 컬럼이므로 "계정 없음"과 "고용형태 미지정"을
     * 구분할 수 있게 미지정은 빈 문자열로 치환해 반환한다. 즉 반환 null = 계정 없음/사용중지.
     *
     * <p>사업장(SITE_CD)을 함께 읽는 이유는 기준값 폴백이 <b>사업장 → 회사 → 2400</b> 3단이
     * 되면서 판정 분모가 소속 사업장에 따라 달라지기 때문이다. 고용형태와 별도 쿼리로 나누면
     * {@code resolveSummary} 왕복이 1회 늘어난다.
     *
     * @return 스코프 행. 계정이 없거나 탈퇴·사용중지면 null
     */
    StdWorkUserScopeVO selectUserScope(@Param("cmpnyCd") String cmpnyCd,
                                       @Param("userCd") String userCd);

    /**
     * SYS083 사유코드의 정책 규칙 1건 (사용중 코드만).
     *
     * <p>2단계 차감·부여 분기가 소비하는 데이터 진입점 — 코드 하드코딩 금지(plan §1.4).
     *
     * @return 규칙 행. 미등록/미사용 코드면 null
     */
    StdWorkReasonRuleVO selectReasonRule(@Param("reasonCd") String reasonCd);

    /** SYS083 사용중 사유코드 전체 (화면 셀렉트 박스용, SORT_IDX 순). */
    List<StdWorkReasonRuleVO> selectReasonRules();
}
