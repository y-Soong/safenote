package com.prafta.web.attd.attd07.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.dto.request.MonthlyAttdListRequest;

public record MonthlyAttdListParam(
	      String workYm
	      , String siteCd
	      , String nodeCd
	      , String incSubNodeYn
	      , String userNm
	      , String employmentType
	      , String gvCmpnyCd
	      , String gvAuthCd
	      , String gvUserCd
	      , String gvSiteCd
	  ){
	      public static MonthlyAttdListParam from(MonthlyAttdListRequest request, TokenInfo tokenInfo) {

	          if (request == null)
	              throw new ApiException(CommonErrorCode.COMMON_400_001);
	          if (tokenInfo == null)
	              throw new ApiException(CommonErrorCode.COMMON_400_001);

	          if (request.getWorkYm() == null || request.getWorkYm().isBlank())
	              throw new ApiException(CommonErrorCode.COMMON_400_001);
	          if (request.getSiteCd() == null || request.getSiteCd().isBlank())
	              throw new ApiException(CommonErrorCode.COMMON_400_001);
	          if (request.getNodeCd() == null || request.getNodeCd().isBlank())
	              throw new ApiException(CommonErrorCode.COMMON_400_001);

	          return new MonthlyAttdListParam(
	                request.getWorkYm()
	              , request.getSiteCd()
	              , request.getNodeCd()
	              , request.getIncSubNodeYn() == null ? "N" : request.getIncSubNodeYn()
	              , request.getUserNm()
	              , request.getEmploymentType()
	              , tokenInfo.gv_cmpnyCd()
	              , tokenInfo.gv_authCd()
	              , tokenInfo.gv_userCd()
	              , tokenInfo.gv_siteCd()
	          );
	      }
	  }
