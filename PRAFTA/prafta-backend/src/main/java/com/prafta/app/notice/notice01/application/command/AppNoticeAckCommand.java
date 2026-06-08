package com.prafta.app.notice.notice01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.app.notice.notice01.application.param.AppNoticeAckParam;

/**
 * 앱 공지 확인/숨김/열람 UPSERT 커맨드.
 * ackType: CONFIRMED / SNOOZED. snoozeUntilYmd: SNOOZED 시 오늘+7(YYYYMMDD), 그 외 null.
 * read 갱신(LAST_READ_DATE)은 별도 upsertNoticeRead 매퍼 사용(ackType=null 케이스 분리).
 */
public record AppNoticeAckCommand(
    String noticeId
    , String ackType
    , String snoozeUntilYmd
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AppNoticeAckCommand confirm(AppNoticeAckParam param) {
        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        return new AppNoticeAckCommand(
            param.noticeId(), "CONFIRMED", null, param.gvCmpnyCd(), param.gvUserCd());
    }

    public static AppNoticeAckCommand snooze(AppNoticeAckParam param, String snoozeUntilYmd) {
        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        return new AppNoticeAckCommand(
            param.noticeId(), "SNOOZED", snoozeUntilYmd, param.gvCmpnyCd(), param.gvUserCd());
    }

    /** 열람(read) 전용 — ackType 변경 없이 LAST_READ_DATE 만 갱신/생성. */
    public static AppNoticeAckCommand read(AppNoticeAckParam param) {
        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        return new AppNoticeAckCommand(
            param.noticeId(), null, null, param.gvCmpnyCd(), param.gvUserCd());
    }
}
