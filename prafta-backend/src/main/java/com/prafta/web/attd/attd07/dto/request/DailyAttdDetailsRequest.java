package com.prafta.web.attd.attd07.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DailyAttdDetailsRequest {

    // attdId / nodeCd 는 호출 흐름(AttdDayDetailPop)에서 빈 문자열 전달 가능성이 있어 nullable 유지.
    private String attdId;

    @NotEmpty
    private String siteCd;

    @NotEmpty
    private String userCd;

    private String userId;

    @NotEmpty
    @Pattern(regexp = "\\d{8}", message = "근무 일자는 YYYYMMDD 형식이어야 합니다.")
    private String workYmd;

    private String nodeCd;
}
