package com.prafta.web.user.user01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MyPasswdRequest {
    private String cmpnyCd;
    private String userCd;
    private String currentPw;
    private String newPw;
}
