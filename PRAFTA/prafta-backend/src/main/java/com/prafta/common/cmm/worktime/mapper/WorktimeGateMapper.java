package com.prafta.common.cmm.worktime.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * prafta-app-022: 근무중 게이트(WorktimeGate) 공용 Mapper.
 *
 * <p>"근무중" 판정에 필요한 최소 조회만 제공한다. 식별값(cmpnyCd/siteCd/userCd)은
 * 호출부에서 TokenInfo(gv_*) 로 도출한 값만 전달한다(IDOR 차단). 본 매퍼는 본문으로
 * 식별값을 받지 않는다.
 *
 * <p>앱/웹 어느 모듈에서도 호출 가능한 공용 영역이므로 {@code com.prafta.common.cmm.worktime}
 * 에 둔다.
 */
@Mapper
public interface WorktimeGateMapper {

    /**
     * DB 서버 NOW() 기준 오늘 일자(YYYYMMDD).
     *
     * <p>JVM 시계가 아닌 DB 시계를 단일 기준으로 삼아 자정 경계에서 판정과 데이터의
     * 일자 불일치(분산 시계 스큐)를 방지한다(attd01/home01 의 DB now 기준 today 패턴과 일관).
     */
    String selectTodayYmd();

    /**
     * 오늘(todayYmd) 본인 열린 근태(미퇴근) 건수.
     *
     * <p>열린 근태 = DEL_YN='N' AND CHECK_IN_TIME IS NOT NULL AND CHECK_OUT_TIME IS NULL.
     * 1건 이상이면 근무중으로 본다(2구간 중 1구간 진행·초과근무 진행 모두 포함).
     * {@code AppAttd01Mapper.selectOpenAttd} 의 열린 근태 정의를 당일(WORK_YMD) COUNT 로 차용.
     *
     * @param cmpnyCd  회사코드 (TokenInfo.gv_cmpnyCd)
     * @param siteCd   사업장코드 (TokenInfo.gv_siteCd)
     * @param userCd   사용자코드 (TokenInfo.gv_userCd)
     * @param todayYmd 오늘 일자(YYYYMMDD, DB NOW 기준)
     * @return 오늘 본인 열린 근태 건수(0 이면 미근무)
     */
    int countOpenAttdToday(@Param("cmpnyCd") String cmpnyCd,
                           @Param("siteCd") String siteCd,
                           @Param("userCd") String userCd,
                           @Param("todayYmd") String todayYmd);

    /**
     * 전일(todayYmd-1) 본인 열린 근태(미퇴근) 건수.
     *
     * <p>prafta-app-031 1.1: 전일 미퇴근(open slot)도 근무중으로 인정(com-003-A 선례).
     * 열린 근태 정의/테이블/컬럼은 {@link #countOpenAttdToday} 와 동일하되 WORK_YMD 만
     * 전일 일자로 본다. 전일 일자는 SQL 에서 todayYmd 기준으로 1일 차감해 산출한다.
     *
     * @param cmpnyCd  회사코드 (TokenInfo.gv_cmpnyCd)
     * @param siteCd   사업장코드 (TokenInfo.gv_siteCd)
     * @param userCd   사용자코드 (TokenInfo.gv_userCd)
     * @param todayYmd 오늘 일자(YYYYMMDD, DB NOW 기준) — 쿼리 내부에서 전일로 환산
     * @return 전일 본인 열린 근태 건수(0 이면 전일 미퇴근 없음)
     */
    int countOpenAttdPrevDay(@Param("cmpnyCd") String cmpnyCd,
                             @Param("siteCd") String siteCd,
                             @Param("userCd") String userCd,
                             @Param("todayYmd") String todayYmd);
}
