package com.prafta.app.attd.attd01.service;

import com.prafta.app.attd.attd01.application.param.CheckInParam;
import com.prafta.app.attd.attd01.application.param.CheckOutParam;
import com.prafta.app.attd.attd01.application.param.DayDetailParam;
import com.prafta.app.attd.attd01.application.param.MonthParam;
import com.prafta.app.attd.attd01.application.param.TodayParam;
import com.prafta.app.attd.attd01.application.param.WeekParam;
import com.prafta.app.attd.attd01.dto.response.MyAttendanceDayResponse;
import com.prafta.app.attd.attd01.dto.response.MyMonthResponse;
import com.prafta.app.attd.attd01.dto.response.MyWeekResponse;

/**
 * prafta-app-002: 앱 본인 근태조회(attd01) 서비스 인터페이스.
 */
public interface AppAttd01Service {

    /** 오늘 근태 조회 (오늘 탭). */
    MyAttendanceDayResponse selectToday(TodayParam param);

    /** 임의 일자 상세 조회 (이번달 셀 선택 등). 응답 구조는 오늘과 동일. */
    MyAttendanceDayResponse selectDayDetail(DayDetailParam param);

    /** 이번주 7일 요약 조회. */
    MyWeekResponse selectWeek(WeekParam param);

    /** 이번달 일별 dayType + 합계 조회. */
    MyMonthResponse selectMonth(MonthParam param);

    /**
     * 셀프 퇴근(check-out). 열린 근태의 퇴근을 채우고 퇴근 GPS 를 기록한다.
     * <p>D+1 윈도우/사업장 동일성/월마감/동시성 검증을 통과해야 한다. 검증 실패 시 ApiException.
     * @return 갱신된 해당 근무일 카드(오늘 탭과 동일 구조).
     */
    MyAttendanceDayResponse checkOut(CheckOutParam param);

    /**
     * 셀프 출근(check-in). 그 일자 새 근태 레코드를 생성한다.
     * <p>구간기반 출근횟수(§5.1)/재출근(§5.2)/초과출근 차단(§5.3·§5.4)/연차일 차단(§8.3)/
     *   다음날 게이트(전날 미완료 차단)/월마감/지오펜스 검증을 통과해야 한다. 실패 시 ApiException.
     * @return 생성 반영된 해당 근무일 카드(오늘 탭과 동일 구조) + isOffsite.
     */
    MyAttendanceDayResponse checkIn(CheckInParam param);
}
