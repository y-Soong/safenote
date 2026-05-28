package com.prafta.web.attd.attd07.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 근태 마감 상태/차단 사유 조회 요청 (GET).
 *
 * <p>prafta-019-C 근태 마감.
 */
@Getter
@Setter
@NoArgsConstructor
public class AttdCloseStatusRequest {

    @FieldLabel("사업장 코드")
    @NotBlank
    @Size(max = 50)
    private String siteCd;

    /** 조회 대상 부서 노드 (비어 있으면 전체 사업장 — master/hr 만 허용) */
    @FieldLabel("부서 코드")
    @Size(max = 50)
    private String nodeCd;

    /** 하위부서 포함 여부 (Y/N) */
    @FieldLabel("하위부서 포함")
    @Pattern(regexp = "[YN]")
    private String incSubNodeYn;

    /** 마감 기준월 (YYYYMM) */
    @FieldLabel("마감 기준월")
    @NotBlank
    @Pattern(regexp = "\\d{6}")
    private String closeYm;
}
