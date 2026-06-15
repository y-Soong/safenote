package com.prafta.common.cmm.dailyjoin.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일일사용자 회원가입 요청.
 * 비로그인 외부 화면에서 전달되며, JWT 토큰이 없으므로 회사/사업장 정보는 본문으로 받는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class InsertDailyUserRequest {
    private String cmpnyCd;
    private String siteCd;
    private String userId;
    private String userPw;
    private String userNm;
    private String mblNo;
    private String certNo;
    private List<AgrTermsRequest> agrTermsList;
}
