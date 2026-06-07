package com.prafta.web.notice.notice01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice01.application.param.NoticeAckParam;

/**
 * 공지 확인/숨김/열람 UPSERT 커맨드.
 * ackType: CONFIRMED / SNOOZED. snoozeUntilYmd: SNOOZED 시 오늘+7(YYYYMMDD), 그 외 null.
 * read 갱신(LAST_READ_DATE)은 별도 update 매퍼 사용(ackType=null 케이스 분리).
 */
public record NoticeAckCommand(
    String noticeId
    , String ackType
    , String snoozeUntilYmd
    , String gvCmpnyCd
    , String gvUserCd
){
    public static NoticeAckCommand confirm(NoticeAckParam param) {
        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        return new NoticeAckCommand(
            param.noticeId(), "CONFIRMED", null, param.gvCmpnyCd(), param.gvUserCd());
    }

    public static NoticeAckCommand snooze(NoticeAckParam param, String snoozeUntilYmd) {
        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        return new NoticeAckCommand(
            param.noticeId(), "SNOOZED", snoozeUntilYmd, param.gvCmpnyCd(), param.gvUserCd());
    }

    /** 열람(read) 전용 — ackType 변경 없이 LAST_READ_DATE 만 갱신/생성. */
    public static NoticeAckCommand read(NoticeAckParam param) {
        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        return new NoticeAckCommand(
            param.noticeId(), null, null, param.gvCmpnyCd(), param.gvUserCd());
    }
}
