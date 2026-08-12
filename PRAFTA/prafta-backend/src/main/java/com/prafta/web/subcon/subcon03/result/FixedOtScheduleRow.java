package com.prafta.web.subcon.subcon03.result;

/**
 * PRAFTA-FIXEDOT-3(M21): 하도급 공유 스냅샷의 "고정연장 실적(분)" 산출 입력행 —
 * (근로자, 근무일) 단위 그날 유효 근무타입의 소정 1·2구간 + 고정연장(전방·후방) 시각.
 *
 * <p>{@code SnapshotSourceRow}(ATTD/OT_ONLY/LEAVE_ONLY 3쿼리 공용 record)의 컬럼 순서 계약을
 * 건드리지 않기 위해 <b>별도 경량 쿼리</b>로 분리했다. 서비스가 (userCd, workYmd) 키로 매칭해
 * {@code FixedOtMinutesUtils} 로 실적을 파생 계산한다.
 *
 * <p>쿼리는 고정연장이 설정된 근무타입이 배정된 날만 반환한다(고정연장 없는 사업장이면 0건 —
 * 기존 스냅샷 생성 경로와 완전히 동일하게 동작).
 */
public record FixedOtScheduleRow(
    String userCd
    , String workYmd
    , String fstSchStrTime
    , String fstSchEndTime
    , String secSchStrTime
    , String secSchEndTime
    , String preFixedOtStrTime
    , String preFixedOtEndTime
    , String fixedOtStrTime
    , String fixedOtEndTime
){
}
