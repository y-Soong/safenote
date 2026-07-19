package com.prafta.app.tbm.admin.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * R3 출결 리스트 응답(진행 LIVE / 종료 COMPLETED).
 *
 * <p>PRAFTA-SUBCON-T5: 타사(지정 체인) 참석자가 섞이므로 소속 표시(affilCmpnyNm)를 추가한다.
 * 표시값은 서버가 개설사 직하 <b>1차 회사명으로 접은 relabel 값</b>이며(마스터 §1-3), 2차 이하
 * 회사코드/회사명은 응답에 싣지 않는다.
 */
@Getter
@Builder
public class AdminAttendeeListResponse {
    private List<AttendeeItem> attendees;
    private int totalCount;

    @Getter
    @Builder
    public static class AttendeeItem {
        private String attendanceCd;
        private String userNm;
        private String userTypeCd;
        private String deptNm;
        private String entryAt;
        private boolean exited;
        private String exitAt;
        private String completionStatusCd;
        private Integer distanceM;
        /** 소속(1차 relabel). 자사 참석자는 자사명. */
        private String affilCmpnyNm;
    }
}
