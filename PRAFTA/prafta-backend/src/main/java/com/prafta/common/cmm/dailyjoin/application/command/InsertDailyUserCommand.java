package com.prafta.common.cmm.dailyjoin.application.command;

import com.prafta.common.cmm.dailyjoin.application.param.InsertDailyUserParam;

/**
 * 일일사용자 회원가입 - TB_DAILY_USER insert 커맨드.
 * 비로그인 외부 화면 가입이므로 INSERT_NO/UPDATE_NO 는 발급된 USER_CD 를 사용한다.
 * REG_TYPE='01'(직접가입) 고정. ACCOUNT_STATUS 는 흐름별로 다르다:
 * 가입(from)='04'(승인대기 — 입장 승인제 D6), 승인 후 로그인 재활성(ofReactivate)='01'(활성화).
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
            , "04"      // ACCOUNT_STATUS : 04 = 승인대기(입장 승인제 D6 — 슬롯 미점유, 승인 후 첫 로그인 시 '01' 전이)
        );
    }

    /**
     * prafta-app-032 B — 로그인 자동 재활성 전용 팩토리.
     * 비활성 일용직 로그인 시 기존 행 값(ID/이름/비번 해시/휴대폰 파생)을 그대로 재활성 커맨드로 싣는다.
     * 비번/휴대폰을 변경하지 않으므로 호출자는 기존 행에서 읽은 값을 그대로 전달한다.
     * REG_TYPE='01'/USE_YN='Y'/ACCOUNT_STATUS='01' 고정(입장 승인제: 승인 후 로그인 = 활성 상태로 전이).
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
