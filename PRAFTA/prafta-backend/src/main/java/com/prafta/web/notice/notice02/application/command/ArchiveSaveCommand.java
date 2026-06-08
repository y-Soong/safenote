package com.prafta.web.notice.notice02.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice02.application.param.ArchiveSaveParam;

/**
 * 자료실 마스터 INSERT 커맨드.
 * noticeId 는 서버 채번값('A'+YYYYMMDD+SEQ), editPwdHash 는 BCrypt 해시(평문 금지).
 * NOTICE_TYPE/POPUP_YN/PIN_YN/TARGET_SCOPE 등 강제값은 mapper XML 에서 리터럴 고정한다.
 */
public record ArchiveSaveCommand(
    String noticeId
    , String archiveTypeCd
    , String title
    , String content
    , String editPwdHash
    , String gvCmpnyCd
    , String gvUserCd
){
    public static ArchiveSaveCommand from(ArchiveSaveParam param, String noticeId, String editPwdHash) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ArchiveSaveCommand(
            noticeId
            , param.archiveTypeCd()
            , param.title()
            , param.content()
            , editPwdHash
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
