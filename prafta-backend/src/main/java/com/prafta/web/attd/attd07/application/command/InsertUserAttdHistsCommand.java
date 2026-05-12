package com.prafta.web.attd.attd07.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.application.model.UpdateUserAttdInfosModel;

public record InsertUserAttdHistsCommand(
	String histId
	, String attdId
    , String siteCd
    , String histType
    , String processReason
    , String workYmd
    
    , String befCheckInDate
    , String befCheckInTime
    , String befCheckOutDate
    , String befCheckOutTime
    
    , String aftCheckInDate
    , String aftCheckInTime
    , String aftCheckOutDate
    , String aftCheckOutTime
    
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static InsertUserAttdHistsCommand from(String histId, String attdId, UpdateUserAttdInfosModel model) {

    	if (histId == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - histId");
    	
    	if (attdId == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - attdId");
    	
        if (model == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - UpdateUserAttdInfosModel");

        return new InsertUserAttdHistsCommand(
    		histId
        	, attdId
            , model.siteCd()
            , "01"					// histType[SYS032] 01:»ý¼º
            , model.reason()
            , model.workYmd()
            
            , model.oriCheckInDate()
            , model.oriCheckInTime()
            , model.oriCheckOutDate()
            , model.oriCheckOutTime()
            
            , model.checkInDate()
            , model.checkInTime()
            , model.checkOutDate()
            , model.checkOutTime()
            
            , model.gvCmpnyCd()
            , model.gvUserCd()
        );
    }
}
