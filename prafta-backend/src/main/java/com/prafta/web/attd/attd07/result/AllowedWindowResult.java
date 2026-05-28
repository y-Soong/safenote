package com.prafta.web.attd.attd07.result;

/**
 * {@code Attd07Mapper.selectAllowedWindow}의 결과.
 *
 * (a) SCH_MGMT의 스케줄된 근무 구간(plan1/plan2)과
 * (b) FNC_STD_TIME으로 표준화된 실제 근무 구간(act1/act2)을
 * (cmpny, site, user, workYmd) 튜플 단위로 함께 담는다.
 *
 * 시간 컬럼은 0 패딩 적용 HH:mm 형식(varchar(4))이며, 데이터가 없으면 null.
 *
 * <p><b>중요:</b> 본 record의 컴포넌트 순서는 반드시
 * {@code Attd07Mapper.xml}의 {@code selectAllowedWindow} SELECT 절 컬럼 순서와
 * 동일해야 한다. MyBatis가 record 매핑 시 컴파일러 옵션 {@code -parameters}가
 * 없으면 위치(position) 기반으로 생성자 인자에 채우기 때문이다.
 * (PRAFTA-003-1 후속 — 컬럼 어긋남으로 인한 ATTD_400_014 오발화 수정.)
 */
public record AllowedWindowResult(
      String plan1Start
    , String plan1End
    , String plan2Start
    , String plan2End

    /** 1차 근태 row — 출근 raw 일자 / 표준화 일자 / 표준화 시각. */
    , String act1InDate
    , String act1InStdDate
    , String act1InStdTime

    /** 1차 근태 row — 퇴근 raw 일자 / 표준화 일자 / 표준화 시각. */
    , String act1OutDate
    , String act1OutStdDate
    , String act1OutStdTime

    /** 2차 근태 row — 출근 raw 일자 / 표준화 일자 / 표준화 시각. */
    , String act2InDate
    , String act2InStdDate
    , String act2InStdTime

    /** 2차 근태 row — 퇴근 raw 일자 / 표준화 일자 / 표준화 시각. */
    , String act2OutDate
    , String act2OutStdDate
    , String act2OutStdTime
) {
}
