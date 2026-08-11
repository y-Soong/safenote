package com.prafta.web.attd.attd07.result;

/**
 * {@code Attd07Mapper.selectAllowedWindow}의 결과.
 *
 * (a) SCH_MGMT의 스케줄된 근무 구간(plan1/plan2)과
 * (b) 실제(raw) 출퇴근 근무 구간(act1/act2)을
 * (cmpny, site, user, workYmd) 튜플 단위로 함께 담는다.
 *
 * <p>초과근무 등록 가능 범위는 "실근태 − 스케줄" 로 계산하므로
 * 본 결과에는 raw 출퇴근 일자/시각만 담는다.
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

    /** 1차 근태 row — 출근 raw 일자 / 시각. */
    , String act1InDate
    , String act1InTime

    /** 1차 근태 row — 퇴근 raw 일자 / 시각. */
    , String act1OutDate
    , String act1OutTime

    /** 2차 근태 row — 출근 raw 일자 / 시각. */
    , String act2InDate
    , String act2InTime

    /** 2차 근태 row — 퇴근 raw 일자 / 시각. */
    , String act2OutDate
    , String act2OutTime

    // ────────────────────────────────────────────────────────────────
    // PRAFTA-FIXEDOT-2: 근무타입 고정연장근무(전방·후방 FROM/TO, HHMM, NULL=없음).
    //   OT 등록 가능 범위 = 실근태 − (소정 ∪ 고정연장 ∪ 연차면제) 로 확장(정책 ①·④).
    //   ⚠️ 아래 4필드는 반드시 record 끝 = SELECT 절 끝 동일 순서(위치 기반 매핑, 중간 삽입 금지).
    // ────────────────────────────────────────────────────────────────

    /** 전방 고정연장 시작 (HHMM, 소정 1구간 시작 이전) */
    , String preFixedOtStrTime
    /** 전방 고정연장 종료 (HHMM) */
    , String preFixedOtEndTime
    /** 후방 고정연장 시작 (HHMM, 소정 마지막 구간 종료 이후) */
    , String fixedOtStrTime
    /** 후방 고정연장 종료 (HHMM, 종료<=시작이면 자정 넘김 +1440 해석) */
    , String fixedOtEndTime
) {
}
