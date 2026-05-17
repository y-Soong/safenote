package com.prafta.web.attd.attd02.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HolidayRequest {

    @FieldLabel("사업장코드")
    @Size(max = 50)
    private String siteCd;

    @FieldLabel("휴일규칙ID")
    @NotBlank
    @Size(max = 10)
    private String holidayId;

    @FieldLabel("휴일규칙명")
    @NotBlank
    @Size(max = 200)
    private String holidayNm;

    @FieldLabel("휴일일자")
    @NotBlank
    @Size(max = 8)
    private String holidayYmd;

    @FieldLabel("휴일타입")
    @NotBlank
    @Size(max = 2)
    private String holidayType;

    @FieldLabel("매년반복여부")
    private boolean repeatYearly;

    @FieldLabel("사용여부")
    @NotBlank
    @Size(max = 1)
    private String useYn;
}