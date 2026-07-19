package com.prafta.web.chkLst.chkLst02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChkptInspectItemListRequest{
	private String siteCd;	// PRAFTA-SUBCON-T0-02: 사업장코드(필수)
	private String codeCd;
}
