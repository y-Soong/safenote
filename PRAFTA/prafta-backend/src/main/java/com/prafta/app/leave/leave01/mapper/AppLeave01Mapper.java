package com.prafta.app.leave.leave01.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import com.prafta.app.leave.leave01.application.query.MyLeaveSummaryQuery;
import com.prafta.app.leave.leave01.result.AppliedLeaveTypeRow;
import com.prafta.app.leave.leave01.result.LeaveExpiringResult;
import com.prafta.app.leave.leave01.result.LeaveGroupAggResult;
import com.prafta.app.leave.leave01.result.LeaveUserResult;

/**
 * prafta-app-005: 앱 "연차 현황" 조회 Mapper.
 * <p>그룹 집계(STATUTORY/MANUAL/TOTAL)는 grantTypePrefix 주입으로 동일 SQL 을 3회 호출한다
 *   (prefix null=TOTAL prefix 무관 전체합). LeaveDashboardMapper(웹 연차현황) SSOT 차용.
 */
@Mapper
public interface AppLeave01Mapper {

    /** DB 기준 오늘 일자(YYYYMMDD, varchar8). 그룹/예정/소멸 쿼리 공통 기준일. */
    String selectTodayYmd();

    /**
     * 그룹별 집계 1건(granted/usedTotal/planned).
     * <p>query.grantTypePrefix 가 null 이면 TOTAL(prefix 무관 전체합),
     *   'STATUTORY\_%'/'MANUAL\_%' 이면 해당 그룹. 부여 없으면 SUM 이 null.
     */
    LeaveGroupAggResult selectGroupAgg(@Param("param") MyLeaveSummaryQuery query);

    /** 소멸 임박(D-30) 집계 1건(그룹 무관, 활성집합 동일). 대상 0건이면 targetCount=0. */
    LeaveExpiringResult selectExpiringSoon(@Param("param") MyLeaveSummaryQuery query);

    /** 사용자 메타(userNm/hireDate/serviceCreditMonths). 스코프 밖/미존재면 null. */
    LeaveUserResult selectUser(@Param("param") MyLeaveSummaryQuery query);

    /**
     * 연차 개편(표시): 신청형 휴가('01') 타입별 한도/사용 1행씩 조회.
     * <p>대상 = TB_LEAVE_TYPE_MGMT WHERE CMPNY_CD AND LEAVE_TYPE='01' AND USE_YN='Y'.
     *   사용분 Σ 술어는 {@code AppLeaveFlowMapper.selectFiscalUsedDays} 와 동일(LEAVE_STATUS='CONFIRMED'
     *   AND DEL_YN='N' AND START_DATE [fiscalStartYmd, fiscalEndYmdExclusive)). 회계연도 경계는
     *   서비스가 {@code FiscalYearUtils} 로 산출해 주입한다. 법정/관리자부여(GRANT 그룹)와 합산하지 않는
     *   별도 항목. 잔여(한도-사용)는 서비스에서 파생(한도 NULL → 한도 0 = 잔여 0 fail-closed).
     *
     * @param cmpnyCd             회사 코드(토큰 도출)
     * @param userCd              사용자 코드(토큰 도출, IDOR 방지)
     * @param fiscalStartYmd      당해 회계연도 시작일(YYYYMMDD, inclusive)
     * @param fiscalEndYmdExclusive 당해 회계연도 종료 경계(YYYYMMDD, exclusive)
     */
    List<AppliedLeaveTypeRow> selectAppliedLeaveTypes(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("userCd") String userCd
            , @Param("fiscalStartYmd") String fiscalStartYmd
            , @Param("fiscalEndYmdExclusive") String fiscalEndYmdExclusive
    );

    /**
     * prafta-com-011-5: 미상계 가불 사용 합계(일). 표시 전용(MVP, 결정 §5 / QA D1/D2 통일 모델 §6-2).
     * <p>가불 GRANT 마커 = {@code GRANT_REASON LIKE '[가불]%'}(멱등키 밖, plan §0-1).
     *   live(STATUS!='CANCELED' AND DEL_YN='N') 이며 <b>아직 미발생(AVAIL_FROM_DATE &gt; today)</b>인 가불 GRANT 의
     *   <b>USED_DAYS</b> 합(종류 무관 전체). 통일 모델(§6-2)에서 가불 GRANT 는 전량으로 생성되고 가불 사용분만 USED
     *   차감되므로 "진짜 당겨쓴 분" = 미발생 가불 USED 합이다(GRANT_DAYS 합 금지). 발생일 도래분은 정식 부여로
     *   전환된 것이라 가불 표시에서 제외한다. 대상 0건이면 0(IFNULL). 식별값은 토큰 도출값만(IDOR).
     *
     * @param cmpnyCd 회사 코드(토큰 도출)
     * @param userCd  사용자 코드(토큰 도출, IDOR 방지)
     * @param today   기준일(YYYYMMDD) — AVAIL_FROM_DATE &gt; today(미발생) 필터
     * @return 미상계(미발생) 가불 USED 합계(없으면 0)
     */
    java.math.BigDecimal selectBorrowedDaysTotal(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("userCd") String userCd
            , @Param("today") String today
    );

    /**
     * 사용 내역(연 단위): CONFIRMED·DEL_YN='N' 사용 실적을 START_DATE 범위로 조회(최신순, LIMIT 500).
     * <p>연차 종류명은 TB_LEAVE_TYPE_MGMT LEFT JOIN(타입 삭제 대비). 미래 시작일(사용예정)도 포함.
     * 식별값은 토큰 도출값만(IDOR — 본인 자기조회).
     *
     * @param cmpnyCd 회사 코드(토큰 도출)
     * @param userCd  사용자 코드(토큰 도출)
     * @param fromYmd 조회 시작일(YYYYMMDD, inclusive — 연도 0101)
     * @param toYmd   조회 종료일(YYYYMMDD, inclusive — 연도 1231)
     */
    List<com.prafta.app.leave.leave01.result.LeaveUseHistoryRow> selectLeaveUsesByRange(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("userCd") String userCd
            , @Param("fromYmd") String fromYmd
            , @Param("toYmd") String toYmd
    );

    /**
     * LC-07(표기): 사용자의 시간차(SYS025 02/03/04) CONFIRMED 사용 분 합계(전 기간, DEL_YN='N').
     * 웹 {@code LeaveDashboardMapper.selectHourlyUsedMinutes} 미러 — FE "시간차 사용 N시간 M분"
     * 원본 표기 전용(잔여/부여 수치 무관). 대상 0건이면 0(IFNULL). 식별값은 토큰 도출값(IDOR).
     */
    Integer selectHourlyUsedMinutes(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("userCd") String userCd
    );
}
