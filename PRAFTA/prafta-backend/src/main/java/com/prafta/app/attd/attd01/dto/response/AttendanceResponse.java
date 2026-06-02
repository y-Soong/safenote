package com.prafta.app.attd.attd01.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-002: 슬롯(구간)별 실 근태 정보 응답 (계약 §3.1 slots[].attendance).
 *
 * <p>한계(스키마 갭, plan §0-2/§4-1):
 *   - checkOutSiteName 은 레코드 단일 SITE_CD 기준이므로 checkInSiteName 과 항상 동일.
 *   - isDifferentSite 는 퇴근지 분리 컬럼이 없어 현재 항상 false.
 * <p>checkInOffsite/checkOutOffsite (prafta-app-003 B-2): 해당 ATTD_ID 에 출근'01'/퇴근'02'
 *   GPS 행이 존재하면 true(근무지 외=외근). GPS 행은 지오펜스 밖일 때만 저장되므로(A0-2 확정 모델)
 *   행 존재=외근 좌표. (구 모델의 GpsValid(유효/Mock) 판정은 폐기 — Mock 은 애초에 저장 안 됨.)
 */
@Getter
@Builder
public class AttendanceResponse {
    private final String checkInDate;       // YYYYMMDD
    private final String checkInTime;       // HHMM
    private final String checkInSiteName;
    private final String checkOutDate;      // YYYYMMDD (퇴근 전 null)
    private final String checkOutTime;      // HHMM
    private final String checkOutSiteName;
    // 외근 여부 — 'is' 접두가 아니므로 getter getCheckInOffsite→"checkInOffsite" 정상(키 일관성 위해 @JsonProperty 명시).
    @JsonProperty("checkInOffsite")
    private final boolean checkInOffsite;   // 출근 GPS 행(01) 존재 = 외근
    @JsonProperty("checkOutOffsite")
    private final boolean checkOutOffsite;  // 퇴근 GPS 행(02) 존재 = 외근
    // Jackson 이 boolean is* getter 에서 "is" 를 떼고 직렬화하는 것을 방지(계약 키 고정).
    @JsonProperty("isMissingCheckOut")
    private final boolean isMissingCheckOut;
    @JsonProperty("isDifferentSite")
    private final boolean isDifferentSite;  // 스키마 한계로 현재 항상 false
}
