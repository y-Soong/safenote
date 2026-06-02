package com.prafta.web.attd.attd04.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AttdStdTimeRuleRequest {
	@FieldLabel("출근시간 표준화 타입")
	@NotBlank
	private String startStdTimeType;
	
	@FieldLabel("퇴근시간 표준화 타입")
	@NotBlank
	private String endStdTimeType;
}
