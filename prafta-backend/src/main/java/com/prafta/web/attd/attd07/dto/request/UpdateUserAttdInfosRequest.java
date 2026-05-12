package com.prafta.web.attd.attd07.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserAttdInfosRequest {
	private String attdId;
    private String siteCd;
    private String nodeCd;
    private String userCd;
    private String userId;
    private String workSeq;
    private String workYmd;
    
    /* 기존 출퇴근 값 */
    private String oriCheckInDate;
    private String oriCheckInTime;
    private String oriCheckOutDate;
    private String oriCheckOutTime;
    
    private String checkInDate;
    private String checkInTime;
    private String checkInMethod;
    
    private String checkOutDate;
    private String checkOutTime;
    private String checkOutMethod;
    private String reason;
}
