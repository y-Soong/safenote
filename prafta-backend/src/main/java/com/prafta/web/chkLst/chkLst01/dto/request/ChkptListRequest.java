package com.prafta.web.chkLst.chkLst01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChkptListRequest{
	private String siteCd;
	private String chkptNm;
	private String chkLstType;
	private String useYn;
}
