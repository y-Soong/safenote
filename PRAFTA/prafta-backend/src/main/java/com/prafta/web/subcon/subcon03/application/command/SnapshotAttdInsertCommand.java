package com.prafta.web.subcon.subcon03.application.command;

import java.math.BigDecimal;

/**
 * 근태 스냅샷 상세행 INSERT 커맨드 1건(PRAFTA-SUBCON-T3 §5-5).
 *
 * <p>PII 최소수집(D8): 인적 정보는 성명(workerNm) + 소속표시(affilCmpnyNm) 뿐이다.
 * 원본 USER_CD/USER_ID/부서/연락처/기기ID/연차사유는 어떤 필드에도 담지 않는다.
 * workerSeq 는 스냅샷 스코프 로컬 번호(그룹핑 전용)다.
 */
public record SnapshotAttdInsertCommand(
    Long snapshotId
    , int workerSeq
    , String workerType
    , String workerNm
    , String affilCmpnyNm
    , String rowType
    , String workYmd
    , int workSeq
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
    , int otMinutes
    /**
     * PRAFTA-FIXEDOT-3(M21): 고정연장 실적(분) — 실근태 ∩ 근무타입 고정연장 구간(정책 ①, 커버분만).
     * 승인 시점 파생 산출값을 스냅샷으로 고정 복사한다(원장 아님). 고정연장 미설정 타입은 0.
     */
    , int fixedOtMinutes
    /**
     * PRAFTA-SUBCON-T8-1: 정상근무 인정시간(분) — (실제∩스케줄)−휴게−확정 시각연차 겹침(Attd_08 산식 파리티).
     * 승인 시점 파생 산출값을 스냅샷으로 고정 복사한다. ★null = 산출 불가(OT_ONLY/LEAVE_ONLY/미출근·미퇴근/
     * 스케줄 시각 공백) — fixedOtMinutes 의 0 과 계약이 다르므로 int 금지(Integer 유지).
     */
    , Integer recogMinutes
    , String leaveNm
    , BigDecimal leaveDays
    , Integer leaveMinutes
    , String leaveEndYmd
    , String insertNo
){
}
