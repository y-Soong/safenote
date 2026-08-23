package com.prafta.app.attd.attd01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.attd.attd01.application.command.CheckInCommand;
import com.prafta.app.attd.attd01.application.command.CheckInGpsCommand;
import com.prafta.app.attd.attd01.application.command.CheckOutCommand;
import com.prafta.app.attd.attd01.application.command.CheckOutGpsCommand;
import com.prafta.app.attd.attd01.application.query.AttdRangeQuery;
import com.prafta.app.attd.attd01.result.AttdRecordResult;
import com.prafta.app.attd.attd01.result.GpsResult;
import com.prafta.app.attd.attd01.result.HolidayResult;
import com.prafta.app.attd.attd01.result.PartialLeaveWindowResult;
import com.prafta.app.attd.attd01.result.LeaveUseResult;
import com.prafta.app.attd.attd01.result.OpenAttdResult;
import com.prafta.app.attd.attd01.result.RangeOvertimeResult;
import com.prafta.app.attd.attd01.result.ScheduleResult;
import com.prafta.app.attd.attd01.result.SiteGeofenceResult;

/**
 * prafta-app-002: 앱 본인 근태조회(attd01) Mapper.
 *
 * <p>모든 조회는 본인 스코프(cmpnyCd/siteCd/userCd) + 일자범위로 한정된다(IDOR 가드).
 *   산출(workStatus/액션/dayType/합계)은 전부 ServiceImpl 에서 처리하고,
 *   여기서는 원천 데이터(스케줄/근태/GPS/휴일/연차/마감)만 로딩한다.
 */
@Mapper
public interface AppAttd01Mapper {

    /** 사업장명 단건 조회 (JWT siteCd 기준). */
    String selectSiteName(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

    /** 근무계획 + 스케줄정의(SCH) + 연차명(LEAVE) 일자범위 조회. */
    List<ScheduleResult> selectScheduleByRange(@Param("q") AttdRangeQuery query);

    /** 실 근태 레코드 일자범위 조회 (DEL_YN='N', SITE_NM 조인). */
    List<AttdRecordResult> selectAttdByRange(@Param("q") AttdRangeQuery query);

    /** 근태 GPS 측정 조회 (ATTD_ID 목록 기준, 출근/퇴근 최신행). */
    List<GpsResult> selectGpsByAttdIds(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("attdIds") List<String> attdIds
    );

    /** 휴일 일자범위 조회 (USE_YN='Y', YYYYMMDD 문자열 변환). */
    List<HolidayResult> selectHolidaysByRange(@Param("q") AttdRangeQuery query);

    /** 본인 연차 사용 실적 조회 (CONFIRMED, 범위 겹침). */
    List<LeaveUseResult> selectLeaveUseByRange(@Param("q") AttdRangeQuery query);

    /**
     * 본인 초과근무 보유 일자(YYYYMMDD) 목록 일자범위 조회 — 스케줄 수정 요청 게이팅용.
     * <p>"초과근무 보유" = 등록 초과근무(tb_user_overtime_mgmt, 취소 제외) OR
     *   초과근무 신청(tb_user_attd_req REQ_TYPE 03/04, 상태 신청/승인). 둘 중 하나라도 있으면 그 일자 포함.
     *   초과근무는 스케줄 기준으로 정산되므로 보유일은 스케줄 변경(수정 요청)을 막는다.
     */
    List<String> selectOvertimeYmds(@Param("q") AttdRangeQuery query);

    /**
     * prafta-app-030 후속: 적용(승인) 초과근무 실적 일자범위 조회 — 본인 근태 조회 응답 표시용.
     * <p>출처 TB_USER_OVERTIME_MGMT. 술어 = AppReq07Mapper.selectAppliedOvertimes(단일일)와 동일하되
     *   WORK_YMD BETWEEN(fromYmd~toYmd) 범위로 일반화: DEL_YN='N' AND OT_STATUS&lt;&gt;'CANCELLED'
     *   AND ACTUAL_END_DATE/TIME IS NOT NULL(구간 확정분만). 식별값은 JWT 도출값(query)만 사용(IDOR).
     *   서비스가 workYmd 별로 그룹/합계한다(N+1 회피 = 범위 1회 호출).
     */
    List<RangeOvertimeResult> selectAppliedOvertimesByRange(@Param("q") AttdRangeQuery query);

    /**
     * prafta-com-008-E-2: 그날 일 단위(USE_UNIT_TYPE='00') 확정 연차 존재 카운트(출근 차단 §8.3 판정).
     * 부분연차(반차/시간차)는 근무일 유지 → 차단 비대상(미카운트). &gt; 0 이면 연차일.
     */
    int countFullDayLeaveOn(@Param("cmpnyCd") String cmpnyCd,
                            @Param("siteCd") String siteCd,
                            @Param("userCd") String userCd,
                            @Param("workYmd") String workYmd);

    /**
     * HB-08(D5) 앱 미러: 그날 확정 <b>시각 보유</b> 연차(반차 '01' + 시간차 '02'~'04')의 면제 구간 목록.
     *
     * <p>앱 초과근무 발의 가드가 "연차로 쉬는 시간대"를 거부(ATTD_400_196)하는 판정 입력.
     * 웹 {@code Attd07Mapper.selectLeaveExemptWindows} 와 동일 술어(app↔web 매퍼 직접 의존 회피 미러).
     * 시각 없는 구 반차는 자연 제외되어 종전 동작을 유지하며, 종일('00')은 대상이 아니다
     * ({@link #countFullDayLeaveOn} 게이트가 ATTD_400_151 로 통째 차단 — 회귀 금지 ②).
     */
    List<PartialLeaveWindowResult> selectPartialLeaveWindowsOn(@Param("cmpnyCd") String cmpnyCd,
                                                               @Param("siteCd") String siteCd,
                                                               @Param("userCd") String userCd,
                                                               @Param("workYmd") String workYmd);

    /**
     * 본인 소속부서 NODE_CD 조회 (마감 커버리지 판정용).
     * <p>없으면 null. TB_USER.
     */
    String selectUserNodeCd(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("siteCd") String siteCd
            , @Param("userCd") String userCd
    );

    /**
     * 해당 월이 본인 부서를 덮는 마감(CLOSED)으로 닫혀 있는지 여부.
     * <p>prafta-028 부서단위 마감 커버리지 판정: '*'(전체) / 자기노드 / INC_SUB='Y' 상위노드.
     *   1 이상이면 마감(closed). TB_ATTD_CLOSE + TB_SITE_NODE 재귀.
     */
    int countCoveringClose(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("siteCd") String siteCd
            , @Param("nodeCd") String nodeCd
            , @Param("closeYm") String closeYm
    );

    // ====================================================================
    // prafta-app check-out: 셀프 퇴근 (쓰기)
    // ====================================================================

    /**
     * 본인의 열린 근태(미퇴근) 중 가장 최근 WORK_YMD/WORK_SEQ 1건 조회.
     * <p>열린 근태 = DEL_YN='N' && CHECK_IN_TIME 有 && CHECK_OUT_TIME NULL,
     *   WORK_YMD &gt;= fromYmd(D+1 윈도우 하한). 없으면 null.
     *   workYmd(Low-1) 가 주어지면 그 일자의 열린건만 대상(우선), 미전달이면 fromYmd 이상 최신 폴백.
     *   사업장/마감/윈도우 상한 재검증은 ServiceImpl 에서 수행한다(여기서는 후보만 좁힌다).
     */
    OpenAttdResult selectOpenAttd(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("siteCd") String siteCd
            , @Param("userCd") String userCd
            , @Param("fromYmd") String fromYmd
            , @Param("workYmd") String workYmd
    );

    /**
     * prafta-app-026: 재퇴근 대상(D+1 윈도우 내 최신 출근행) 1건 조회.
     * <p>대상 = DEL_YN='N' && CHECK_IN_TIME 有 && WORK_YMD &gt;= fromYmd(D+1 윈도우 하한),
     *   <b>CHECK_OUT_TIME 유무 무관</b>(이미 퇴근한 슬롯도 재퇴근 대상). workYmd 가 주어지면 그 일자로 한정.
     *   정렬: WORK_YMD DESC, WORK_SEQ DESC LIMIT 1 → tail 슬롯만 선정(새 출근 발생 시 이전 슬롯은 제외).
     *   반환 {@code checkOutTime} 이 null 이면 최초 퇴근, 非null 이면 재퇴근(시각 덮어쓰기).
     *   사업장/마감/윈도우 상한 재검증은 ServiceImpl 에서 수행한다(여기서는 후보만 좁힌다).
     */
    OpenAttdResult selectReCheckOutTarget(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("siteCd") String siteCd
            , @Param("userCd") String userCd
            , @Param("fromYmd") String fromYmd
            , @Param("workYmd") String workYmd
    );

    /**
     * 사업장 지오펜스 기준값(중심좌표 LAT/LON + 허용반경 GPS_RANGE) 단건 조회.
     * <p>지오펜스 판정용. LAT/LON/GPS_RANGE 중 결측이면 서비스가 온사이트 폴백(A안).
     */
    SiteGeofenceResult selectSiteGeofence(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("siteCd") String siteCd
    );

    /** GPS_ID 선채번 (회사별 시퀀스, 'GPS_ID' 키). YYYYMMDD + 시퀀스. */
    String selectGpsId(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 출근 레코드의 퇴근 채움 UPDATE. 동시성 가드(CHECK_OUT_TIME IS NULL) 포함.
     * @return 영향 행 수(0이면 이미 퇴근됨/대상 없음 → 서비스에서 거부).
     */
    int updateCheckOut(@Param("c") CheckOutCommand command);

    /**
     * prafta-app-026: 재퇴근(이미 퇴근된 슬롯)용 퇴근시각 덮어쓰기 UPDATE.
     * <p>NULL 가드 없음(last-write-wins). WHERE = ATTD_ID + CMPNY_CD + USER_CD + DEL_YN='N'(스코프 가드).
     *   CHECK_OUT_DATE/TIME/METHOD/DEVICE + UPDATE_NO/UPDATE_DATE 를 갱신한다.
     * @return 영향 행 수(0이면 대상 없음 → 서비스에서 거부).
     */
    int updateReCheckOut(@Param("c") CheckOutCommand command);

    /** 퇴근 GPS 기록 INSERT (TB_USER_ATTD_GPS, GPS_INFO_TYPE='02'). */
    int insertCheckOutGps(@Param("g") CheckOutGpsCommand command);

    /**
     * prafta-app-026: 해당 ATTD_ID 의 퇴근 GPS 행(GPS_INFO_TYPE='02') 물리 삭제(hard DELETE).
     * <p>TB_USER_ATTD_GPS 에는 DEL_YN 컬럼이 없고, 외근 판정 EXISTS 가 DEL_YN 을 보지 않으므로
     *   재퇴근 시 delete-then-insert 로 외근↔온사이트 전환을 정확히 반영하려면 hard DELETE 여야 한다.
     * @return 삭제 행 수(0=기존 외근 GPS 없었음).
     */
    int deleteCheckOutGps(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("attdId") String attdId
    );

    // ====================================================================
    // prafta-app-003 A1: 셀프 출근 (check-in, 쓰기)
    // ====================================================================

    /** ATTD_ID 선채번 (회사별 시퀀스, 'ATTD_ID' 키). YYYYMMDD + 시퀀스. */
    String selectAttdId(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 동시 출근(TOCTOU) 방지용 본인 행 비관적 잠금(FOR UPDATE).
     * <p>checkIn 트랜잭션 시작부에서 호출해 동일 사용자의 동시 출근을 직렬화한다.
     *   tb_user_attd_mgmt 자연키 UNIQUE 부재를 보완(count→insert 경쟁 차단).
     *   잠금은 트랜잭션 커밋/롤백 시 해제. @return 잠근 USER_CD(존재 확인용), 없으면 null.
     */
    String lockUserForCheckIn(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("siteCd") String siteCd
            , @Param("userCd") String userCd
    );

    /**
     * 본인의 그 일자 근태 레코드 수(DEL_YN='N'). WORK_SEQ 산정 + 출근횟수 제한 판정용.
     */
    int countAttdByYmd(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("siteCd") String siteCd
            , @Param("userCd") String userCd
            , @Param("workYmd") String workYmd
    );

    /**
     * 본인의 그 일자 "열린(미퇴근) 근태" 수(DEL_YN='N' && CHECK_IN_TIME 有 && CHECK_OUT_TIME NULL).
     * <p>재출근 가드(§5.2): 직전 구간 미퇴근이면 재출근 차단.
     */
    int countOpenAttdByYmd(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("siteCd") String siteCd
            , @Param("userCd") String userCd
            , @Param("workYmd") String workYmd
    );

    /**
     * 본인의 "직전일(WORK_YMD = today-1) 열린 근태" 수(DEL_YN='N' && CHECK_IN_TIME 有 && CHECK_OUT_TIME NULL).
     * <p>다음날 출근 게이트(prafta-app-021 §7.6): 직전일(D) 미퇴근이면 D+1 출근만 차단.
     *   D+2 이상 과거 미퇴근은 차단하지 않는다(출근 허용, 근태 보정 §7.4 로 해소).
     */
    int countPastOpenAttd(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("siteCd") String siteCd
            , @Param("userCd") String userCd
            , @Param("yesterdayYmd") String yesterdayYmd
    );

    /**
     * (작업지시서_소속이동중_출퇴근-사업장필터-근본수정 재작업 — qa 권고) 직전일(WORK_YMD = yesterdayYmd)
     * 열린(미퇴근) 근태 중 1건의 실제 SITE_CD 조회. {@link #countPastOpenAttd} 는 건수만 반환하므로,
     * D+1 출근 게이트에서 그 D-1 레코드가 실귀속된 사업장 기준으로 마감 판정을 하기 위해 사용한다.
     * 없으면 null. (WORK_SEQ DESC LIMIT 1 — 여러 건이면 최신 구간 대표값.)
     */
    OpenAttdResult selectPastOpenAttd(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("userCd") String userCd
            , @Param("yesterdayYmd") String yesterdayYmd
    );

    /** 출근 레코드 INSERT (TB_USER_ATTD_MGMT, CHECK_OUT_* 는 NULL). */
    int insertCheckIn(@Param("c") CheckInCommand command);

    /** 출근 GPS 기록 INSERT (TB_USER_ATTD_GPS, GPS_INFO_TYPE='01'). */
    int insertCheckInGps(@Param("g") CheckInGpsCommand command);
}
