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

    /**
     * prafta-app-032 B — 로그인 자동 재활성 전용 팩토리.
     * 비활성 일용직 로그인 시 기존 행 값(ID/이름/비번 해시/휴대폰 파생)을 그대로 재활성 커맨드로 싣는다.
     * 비번/휴대폰을 변경하지 않으므로 호출자는 기존 행에서 읽은 값을 그대로 전달한다.
     * REG_TYPE='01'/USE_YN='Y'/ACCOUNT_STATUS='01' 고정(재활성 = 활성 상태로 복귀).
     */
    public static InsertDailyUserCommand ofReactivate(
        String cmpnyCd
        , String siteCd
        , String userCd
        , String userId
        , String userNm
        , String userPw
        , String mblNoEnc
        , String mblNoHmac
        , String mblNoLast4
    ) {
        return new InsertDailyUserCommand(
            cmpnyCd
            , siteCd
            , userCd
            , userId
            , userNm
            , userPw

            , mblNoEnc
            , mblNoHmac
            , mblNoLast4

            , "01"      // REG_TYPE : 01 = 직접가입
            , "Y"       // USE_YN
            , "01"      // ACCOUNT_STATUS : 01 = 활성화
        );
    }
}
