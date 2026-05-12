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
	      , String gvCmpnyCd
	      , String gvAuthCd
	      , String gvUserCd
	  ){
	      public static MonthlyAttdListParam from(MonthlyAttdListRequest request, TokenInfo tokenInfo) {

	          if (request == null)
	              throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - MonthlyAttdListRequest");
	          if (tokenInfo == null)
	              throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - TokenInfo");

	          if (request.getWorkYm() == null || request.getWorkYm().isBlank())
	              throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - workYm");
	          if (request.getSiteCd() == null || request.getSiteCd().isBlank())
	              throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - siteCd");
	          if (request.getNodeCd() == null || request.getNodeCd().isBlank())
	              throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - nodeCd");

	          return new MonthlyAttdListParam(
	                request.getWorkYm()
	              , request.getSiteCd()
	              , request.getNodeCd()
	              , request.getIncSubNodeYn() == null ? "N" : request.getIncSubNodeYn()
	              , request.getUserNm()
	              , tokenInfo.gv_cmpnyCd()
	              , tokenInfo.gv_authCd()
	              , tokenInfo.gv_userCd()
	          );
	      }
	  }
