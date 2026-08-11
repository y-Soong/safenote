package com.prafta.web.attd.attd01.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SchInfoRequest{
	private String cmpnyCd;
	private String siteCd;
	private String schCd;
	
	@FieldLabel("근무코드")
	@NotNull
	@Size(max = 50)
	private String schNo;
	private String schType;
	private String applyDate;
	
	private String fstSchStrTime;
	private String fstSchEndTime;
	private String fstSchBrkMin;
	private String fstBrkStrTime;
	private String fstBrkEndTime;

	private String secSchStrTime;
	private String secSchEndTime;
	private String secSchBrkMin;
	private String secBrkStrTime;
	private String secBrkEndTime;

	// PRAFTA-FIXEDOT-1: 전방(출근 전)·후방(퇴근 후) 고정연장근무 FROM/TO (HHMM, 선택 — 쌍 완결성 V1)
	private String preFixedOtStrTime;
	private String preFixedOtEndTime;
	private String fixedOtStrTime;
	private String fixedOtEndTime;

	private String useYn;
}
