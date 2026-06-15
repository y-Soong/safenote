package com.prafta.web.user.user01.application.model;

public record UserInfoModel(
	String cmpnyCd
	, String userCd
    , String userId
    , String userPw
    , String userNm
    , String mblNo
    , String email
    , String gender
    , String birthDt
    , String siteCd
    , String nodeCd
    , String oriNodeCd
    , String authCd
    , String rankCd
    , String useYn
    // PRAFTA-COM-008-E-5 — 기본 근무타입(SCH_CD, 선택). blank 면 미변경.
    , String defaultSchCd

    , String gvUserCd
) {

}
