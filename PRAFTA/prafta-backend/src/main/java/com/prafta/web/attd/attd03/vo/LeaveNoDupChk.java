package com.prafta.web.attd.attd03.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LeaveNoDupChk{
	
    String cmpnyCd;
    String leaveCd;
    String leaveNo;
    String leaveNm;
}
