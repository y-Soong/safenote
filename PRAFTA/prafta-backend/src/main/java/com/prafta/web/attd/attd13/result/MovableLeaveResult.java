package com.prafta.web.attd.attd13.result;

import java.math.BigDecimal;

/**
 * 근로자 본인 이동 가능 연차일 1건 (PRAFTA-COM-008-C, C-5a).
 *
 * <p>SELECT 컬럼 순서 = 생성자 인자 순서(MyBatis 위치 기반 매핑).
 *
 * <p>위치선택 확장(2026-08-18): 앱 발의 화면의 단위별 조건부 UI("원 시각/파트 유지" 요약·종료 파생 표시)용
 * 원본 시각/분 3개를 <b>마지막에</b> 추가(기존 8 컴포넌트 순서 불변 — SELECT 끝과 1:1).
 */
public record MovableLeaveResult(
      String leaveId
    , String leaveCd
    , String startDate
    , BigDecimal leaveDays
    , String useUnitType
    , String promotionStage
    , String designatorType
    , String availToDate

    /** 시작 시각 (HHmm. 시간차 02/03/04 + 반차 01 보유. 종일 00 은 NULL) */
    , String startTime

    /** 종료 시각 (HHmm. 시간차/반차 보유. END &lt; START 면 익일 표기 규약) */
    , String endTime

    /** 차감 분 (시간차 원본 총량 — 대표행 보유, 불변식 1. 종료 파생 표시용) */
    , Integer leaveMinutes
) {
}
