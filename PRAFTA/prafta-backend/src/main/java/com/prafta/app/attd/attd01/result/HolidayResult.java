package com.prafta.app.attd.attd01.result;

/**
 * prafta-app-002: 휴일 결과 (TB_HOLIDAY).
 *
 * <p>매핑 대상: AppAttd01Mapper.selectHolidaysByRange.
 * <p>HOLIDAY_YMD 는 date 타입 → SQL 에서 DATE_FORMAT(..,'%Y%m%d') 로 YYYYMMDD 문자열 변환해 받는다.
 *   USE_YN='Y' 만 조회.
 */
public record HolidayResult(
    String holidayYmd
    , String holidayNm
    , String holidayType
) {
}
