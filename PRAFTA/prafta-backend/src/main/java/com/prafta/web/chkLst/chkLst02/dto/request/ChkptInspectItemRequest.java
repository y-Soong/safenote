package com.prafta.web.chkLst.chkLst02.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChkptInspectItemRequest {
	private String cmpnyCd;

	// PRAFTA-SUBCON-T0-02: 사업장 키 전환 — 행별 사업장코드 필수
	@FieldLabel("사업장코드")
	@NotBlank
	private String siteCd;
	private String chkLstType;
	private String inspectItemCd;
	
	@FieldLabel("점검항목명")
	@NotBlank
	private String inspectItemSubj;
	private int sortIdx;
	private String strDate;
	private String useYn;
}
