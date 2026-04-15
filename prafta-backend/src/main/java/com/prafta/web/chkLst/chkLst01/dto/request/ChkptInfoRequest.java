package com.prafta.web.chkLst.chkLst01.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChkptInfoRequest{
	private String chk;
	
	@FieldLabel("사업장코드")
	@NotBlank()
	private String siteCd;
	private String siteNm;
	
	@FieldLabel("체크리스트유형")
	@NotBlank()
	private String chkLstType;
	private String chkptCd;
	
	@FieldLabel("점검항목명")
	@NotBlank()
	private String chkptNm;
	private String chkptDesc;
	private String useYn;
	private String mgmtUserCd;
	private String mgmtUserNm;
}
