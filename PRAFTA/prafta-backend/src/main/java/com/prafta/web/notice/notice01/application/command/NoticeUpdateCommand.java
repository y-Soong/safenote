package com.prafta.web.notice.notice01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice01.application.param.NoticeSaveParam;

/**
 * 공지 마스터 UPDATE 커맨드.
 * EDIT_PWD 는 수정에서 변경하지 않는다(별도 비번 변경 기능 없음). pinOrder 는 정규화 완료값.
 */
public record NoticeUpdateCommand(
    String noticeId
    , String title
    , String content
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
    public static NoticeUpdateCommand from(NoticeSaveParam param, Integer normalizedPinOrder) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new NoticeUpdateCommand(
            param.noticeId()
            , param.title()
            , param.content()
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
