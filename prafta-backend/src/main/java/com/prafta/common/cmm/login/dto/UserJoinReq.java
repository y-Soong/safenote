package com.prafta.common.cmm.login.dto;

import lombok.Data;

@Data
public class UserJoinReq{
    String cmpnyCd;
    String userId;
    String userPw;
    String userNm;
    String siteCd;
    String nodeCd;
    String authCd;
    String mblNo;
    String email;
    String gender;
    String birthDt;
    String useYn;
    
}
