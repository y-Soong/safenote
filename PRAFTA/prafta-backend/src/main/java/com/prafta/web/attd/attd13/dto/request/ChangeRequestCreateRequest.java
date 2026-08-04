package com.prafta.web.attd.attd13.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * 관리자 연차 변경(이동)/삭제 발의 요청 (PRAFTA-COM-008-C, POST /webApi/attd13/change-requests).
 *
 * <p>대상 연차는 TARGET_LEAVE_ID 로 지정한다. 대상 근로자/회사/사업장 정합은 서버가
 * LEAVE_ID 로 재조회하여 IDOR 검증한다(클라이언트 식별값 불신뢰).
 *
 * <p>대문자 필드는 Jackson 프로퍼티 망글링(target_LEAVE_ID 등)을 막기 위해 @JsonProperty 로 JSON 키를 필드명 그대로 고정한다.
 */
@Getter
@Setter
public class ChangeRequestCreateRequest {

    /** 대상 연차 사용 ID (tb_user_leave_use.LEAVE_ID). */
    @NotBlank
    @JsonProperty("TARGET_LEAVE_ID")
    private String TARGET_LEAVE_ID;

    /** 요청 유형 MOVE:이동 / DELETE:삭제. */
    @NotBlank
    @JsonProperty("REQ_TYPE")
    private String REQ_TYPE;

    /**
     * 이동 대상일 (YYYYMMDD, MOVE 시 필수 / DELETE 시 무시).
     * F7c(sec Low-003): 형식 1차 방어 — 실재 날짜(2월 31일 등) 검증은 서버 validateMove 의 LocalDate 파싱.
     */
    @Pattern(regexp = "\\d{8}")
    @JsonProperty("MOVE_TARGET_DATE")
    private String MOVE_TARGET_DATE;

    /** 요청 사유 (필수). */
    @NotBlank
    @JsonProperty("REQ_REASON")
    private String REQ_REASON;
}
