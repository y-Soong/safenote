package com.prafta.web.notice.notice01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice01.application.param.NoticeSaveParam;

/**
 * 공지 마스터 INSERT 커맨드.
 * noticeId 는 서버 채번값, editPwdHash 는 BCrypt 해시(평문 금지), pinOrder 는 정규화 완료값.
 */
public record NoticeSaveCommand(
    String noticeId
    , String title
    , String content
    , String editPwdHash
    , String targetScope
    , String includeDailyYn
    , String popupYn
    , String popupFromYmd
    , String popupToYmd
    , String pinYn
    , Integer pinOrder
    , String gvCmpnyCd
    , String gvUserCd
){
    public static NoticeSaveCommand from(NoticeSaveParam param, String noticeId,
                                         String editPwdHash, Integer normalizedPinOrder) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new NoticeSaveCommand(
            noticeId
            , param.title()
            , param.content()
            , editPwdHash
            , param.targetScope()
            , param.includeDailyYn()
            , param.popupYn()
            , param.popupFromYmd()
            , param.popupToYmd()
            , param.pinYn()
            , normalizedPinOrder
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
