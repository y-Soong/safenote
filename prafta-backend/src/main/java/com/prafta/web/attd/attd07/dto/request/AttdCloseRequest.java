package com.prafta.web.attd.attd07.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 근태 마감 / 마감 해제 요청 (POST).
 *
 * <p>prafta-019-C 근태 마감.
 */
@Getter
@Setter
@NoArgsConstructor
public class AttdCloseRequest {

    @FieldLabel("사업장 코드")
    @NotBlank
    @Size(max = 50)
    private String siteCd;

    /** 마감 대상 부서 노드 (비어 있으면 전체 사업장 마감 — master/hr 만 허용) */
    @FieldLabel("부서 코드")
    @Size(max = 50)
    private String nodeCd;

    /** 하위부서 포함 여부 (Y/N) — 조회 시 '하위부서 조회' 체크값과 동일 */
    @FieldLabel("하위부서 포함")
    @Pattern(regexp = "[YN]")
    private String incSubNodeYn;

    /** 마감 기준월 (YYYYMM) */
    @FieldLabel("마감 기준월")
    @NotBlank
    @Pattern(regexp = "\\d{6}")
    private String closeYm;

    /** 마감/해제 사유 (선택) */
    @FieldLabel("사유")
    @Size(max = 500)
    private String closeDesc;
}
