package com.prafta.web.user.user01.dto;

import lombok.Data;

@Data
public class UserInfoReq {
	String cmpnyCd;
    String userId;
    String userPw;
    String userNm;
    String mblNo;
    String email;
    String gender;
    String birthDt;
    String siteCd;
    String nodeCd;
    String authCd;
    String useYn;    
}
