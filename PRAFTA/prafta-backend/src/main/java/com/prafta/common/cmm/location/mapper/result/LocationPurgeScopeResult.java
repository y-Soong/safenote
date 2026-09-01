package com.prafta.common.cmm.location.mapper.result;

/**
 * 위치정보 파기 대상 집계 — 파기 이력에 남길 건수/기간.
 *
 * <p>★record 위치 매핑이므로 {@code LocationPurgeMapper.selectPurgeScope} 의 SELECT 순서와
 * 아래 컴포넌트 순서를 반드시 일치시킬 것.
 *
 * <p>★좌표 값은 담지 않는다 — 이 record 는 이력 기록용이고, 이력에 좌표가 남으면
 * 파기가 파기가 아니게 된다.
 *
 * @param attdGpsRows       출퇴근 좌표 보유 행 수
 * @param tbmAttendanceRows TBM 입실 좌표 보유 행 수
 * @param tbmSessionRows    TBM 개설자 좌표 보유 행 수
 * @param oldestCollected   파기 대상 중 최초 수집일(YYYYMMDD). 대상이 없으면 null
 * @param latestCollected   파기 대상 중 최종 수집일(YYYYMMDD). 대상이 없으면 null
 */
public record LocationPurgeScopeResult(
        int attdGpsRows
        , int tbmAttendanceRows
        , int tbmSessionRows
        , String oldestCollected
        , String latestCollected
) {
    /** 파기할 좌표가 하나도 없는가(이미 철회했거나 애초에 외근/TBM GPS 기록이 없는 계정). */
    public boolean isEmpty() {
        return attdGpsRows == 0 && tbmAttendanceRows == 0 && tbmSessionRows == 0;
    }
}
