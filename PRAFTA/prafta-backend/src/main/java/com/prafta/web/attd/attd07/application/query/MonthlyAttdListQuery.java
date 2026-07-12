package com.prafta.web.attd.attd07.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.application.param.MonthlyAttdListParam;

public record MonthlyAttdListQuery(
      String workYm
      , String siteCd
      , String nodeCd
      , String incSubNodeYn
      , String userNm
      , String employmentType
      , String gvCmpnyCd
      , String gvAuthCd
      , String gvUserCd
  ){
      public static MonthlyAttdListQuery from(MonthlyAttdListParam param) {

          if (param == null)
              throw new ApiException(CommonErrorCode.COMMON_400_001);

          return new MonthlyAttdListQuery(
                param.workYm()
              , param.siteCd()
              , param.nodeCd()
              , param.incSubNodeYn()
              , param.userNm()
              , param.employmentType()
              , param.gvCmpnyCd()
              , param.gvAuthCd()
              , param.gvUserCd()
          );
      }
  }