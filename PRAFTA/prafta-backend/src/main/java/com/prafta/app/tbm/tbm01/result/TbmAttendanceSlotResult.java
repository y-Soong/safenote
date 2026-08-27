package com.prafta.app.tbm.tbm01.result;

/**
 * 출결 슬롯(UNIQUE 키) 점유 행 조회 결과.
 *
 * <p>UNIQUE(CMPNY,SESSION,USER_TYPE,USER) 단위로 기존 출결 행을 확인해 입실 처리 분기:
 * <ul>
 *   <li>행 없음 → INSERT(신규 입실)</li>
 *   <li>DEL_YN='N' → 이미 입실(멱등)</li>
 *   <li>DEL_YN='Y' → 내보내기 후 재입실: 해당 행 RESTORE(DEL_YN='N' 복구)</li>
 * </ul>
 */
public record TbmAttendanceSlotResult(
    String attendanceCd
    , String delYn
){
}
