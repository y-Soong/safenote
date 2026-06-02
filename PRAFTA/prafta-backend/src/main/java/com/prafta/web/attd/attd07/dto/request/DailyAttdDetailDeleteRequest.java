package com.prafta.web.attd.attd07.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DailyAttdDetailDeleteRequest {

    @NotBlank
    private String siteCd;

    @NotBlank
    private String userCd;

    @NotBlank
    private String attdId;

    @NotBlank
    private String reason;
}
