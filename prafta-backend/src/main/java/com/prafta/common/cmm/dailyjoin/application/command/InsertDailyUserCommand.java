package com.prafta.common.cmm.dailyjoin.application.command;

import com.prafta.common.cmm.dailyjoin.application.param.InsertDailyUserParam;

/**
 * 일일사용자 회원가입 - TB_DAILY_USER insert 커맨드.
 * 비로그인 외부 화면 가입이므로 INSERT_NO/UPDATE_NO 는 발급된 USER_CD 를 사용한다.
 * REG_TYPE='01'(직접가입), ACCOUNT_STATUS='01'(활성화) 고정.
 */
public record InsertDailyUserCommand(
    String cmpnyCd
    , String siteCd
    , String userCd
    , String userId
    , String userNm
    , String userPw

    , String mblNoEnc
    , String mblNoHmac
    , String mblNoLast4

    , String regType
    , String useYn
    , String accountStatus
) {
    public static InsertDailyUserCommand from(
        InsertDailyUserParam param
        , String userCd
        , String userPw
        , String mblNoEnc
        , String mblNoHmac
        , String mblNoLast4
    ) {
        return new InsertDailyUserCommand(
            param.cmpnyCd()
            , param.siteCd()
            , userCd
            , param.userId()
            , param.userNm()
            , userPw

            , mblNoEnc
            , mblNoHmac
            , mblNoLast4

            , "01"      // REG_TYPE : 01 = 직접가입(링크/QR 회원가입 고정)
            , "Y"       // USE_YN
            , "01"      // ACCOUNT_STATUS : 01 = 활성화
        );
    }
}
