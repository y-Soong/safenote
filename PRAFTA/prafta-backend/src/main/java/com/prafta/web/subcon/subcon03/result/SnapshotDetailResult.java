package com.prafta.web.subcon.subcon03.result;

import java.math.BigDecimal;

/**
 * 수신 스냅샷 상세행 1건(읽기전용 — PRAFTA-SUBCON-T3 §5-8).
 *
 * <p>응답에 원본 USER_CD·하위 회사 식별자·릴레이 묶음(BUNDLE) 정보는 존재하지 않는다.
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record SnapshotDetailResult(
    Long detailId
    , Integer workerSeq
    , String workerType
    , String workerNm
    , String affilCmpnyNm
    , String rowType
    , String workYmd
    , Integer workSeq
    , String schNm
    , String schType
    , String planStrTime
    , String planEndTime
    , Integer planBrkMin
    , String checkInDate
    , String checkInTime
    , String checkOutDate
    , String checkOutTime
    , String attdStatusCd
    , Integer otMinutes
    , String leaveNm
    , BigDecimal leaveDays
    , Integer leaveMinutes
    , String leaveEndYmd
    /**
     * PRAFTA-FIXEDOT-3(M21): 고정연장 실적(분) — 실근태가 근무타입 고정연장 구간을 커버한 분.
     * 고정연장 미설정 타입은 0. "연장 미이행" 배지는 스냅샷 대상이 아니다(plan §5-2).
     * ⚠️ record 끝 = SELECT 끝 동일 순서(MyBatis 위치 기반 매핑).
     */
    , Integer fixedOtMinutes
    /**
     * PRAFTA-SUBCON-T8-1: 정상근무 인정시간(분) — (실제∩스케줄)−휴게−확정 시각연차 겹침(Attd_08 산식 파리티,
     * 스냅샷 생성 시점 고정값). NULL = 산출 불가(OT_ONLY/LEAVE_ONLY/미출근·미퇴근/스케줄 공백) 또는 구스냅샷
     * — 화면은 '-' 표시(0 과 구분, 클라 재계산 폴백 금지).
     * ⚠️ record 끝 = SELECT 끝 동일 순서(MyBatis 위치 기반 매핑) — 중간 삽입 절대 금지.
     */
    , Integer recogMinutes
){
}
