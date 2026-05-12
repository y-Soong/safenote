package com.prafta.web.attd.attd07.application.model;

public record UpdateUserAttdInfosModel(
	String attdId
    , String siteCd
    , String nodeCd
    , String userCd
    , String userId
    , String workSeq
    , String workYmd
    
    , String oriCheckInDate
    , String oriCheckInTime
    , String oriCheckOutDate
    , String oriCheckOutTime
    
    , String checkInDate
    , String checkInTime
    , String checkInMethod
    , String checkOutDate
    , String checkOutTime
    , String checkOutMethod
    , String reason
    , String gvCmpnyCd
    , String gvUserCd
) {
}
