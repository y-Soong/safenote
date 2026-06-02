package com.prafta.web.user.user01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WithdrawMyAccountRequest {
    private String cmpnyCd;
    private String userCd;
}
