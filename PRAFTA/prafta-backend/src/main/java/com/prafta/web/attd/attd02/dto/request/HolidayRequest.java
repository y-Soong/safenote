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

    /* 신규 등록 시에는 빈 값으로 들어와 서비스단에서 채번한다. 수정/삭제 시에만 값이 존재한다. */
    @FieldLabel("휴일규칙ID")
    @Size(max = 10)
    private String holidayId;

    @FieldLabel("휴일규칙명")
    @NotBlank
    @Size(max = 200)
    private String holidayNm;

    /* 프론트는 'YYYY-MM-DD'(10자리, 하이픈 포함)로 전송하며 매퍼에서 하이픈을 제거해 적재한다. */
    @FieldLabel("휴일일자")
    @NotBlank
    @Size(max = 10)
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