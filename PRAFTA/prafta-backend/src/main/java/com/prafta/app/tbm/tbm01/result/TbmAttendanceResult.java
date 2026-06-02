package com.prafta.app.tbm.tbm01.result;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-app-004-C: tb_tbm_attendance 본인 출결 조회 결과.
 * <p>입실/종료 검증(기입실 여부, 미종료 여부)에 사용. GPS 좌표는 응답에 싣지 않는다(D5).
 */
@Getter
@Setter
public class TbmAttendanceResult {
    private String attendanceCd;
    private String sessionCd;
    private Date entryAt;
    private Integer entryDistanceM;
    private Date exitAt;
    private String completionStatusCd;  // SYS053
}
