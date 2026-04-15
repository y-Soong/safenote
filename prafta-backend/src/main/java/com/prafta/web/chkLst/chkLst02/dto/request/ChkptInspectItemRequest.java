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
