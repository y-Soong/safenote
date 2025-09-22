package com.prafta.common.cmm.login.dto;

import com.prafta.common.dto.cmmReqDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserJoinReqDto extends cmmReqDto{
    private String cmpnyCd;
    private String userId;
    private String userPw;
    private String userNm;
    private String siteCd;
    private String authCd;
    private String mblNo;
    private String email;
    private String gender;
    private String birthDt;
    private String useYn;
    
}
