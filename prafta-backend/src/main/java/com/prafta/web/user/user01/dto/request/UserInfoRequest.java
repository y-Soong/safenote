package com.prafta.web.user.user01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserInfoRequest {
	private String cmpnyCd;
	private String userCd;
    private String userId;
    private String userPw;
    private String userNm;
    private String mblNo;
    private String email;
    private String gender;
    private String birthDt;
    private String siteCd;
    private String nodeCd;
    private String oriNodeCd;
    private String authCd;
    private String useYn;
}
