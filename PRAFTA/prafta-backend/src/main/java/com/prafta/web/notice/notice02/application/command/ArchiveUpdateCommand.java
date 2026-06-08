package com.prafta.web.notice.notice02.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice02.application.param.ArchiveSaveParam;

/**
 * 자료실 마스터 UPDATE 커맨드. EDIT_PWD 는 수정에서 변경하지 않는다.
 * 자료타입/제목/내용만 갱신(강제값 NOTICE_TYPE='ARCHIVE' 등은 mapper WHERE/SET 에서 보존).
 */
public record ArchiveUpdateCommand(
    String noticeId
    , String archiveTypeCd
    , String title
    , String content
    , String gvCmpnyCd
    , String gvUserCd
){
    public static ArchiveUpdateCommand from(ArchiveSaveParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ArchiveUpdateCommand(
            param.noticeId()
            , param.archiveTypeCd()
            , param.title()
            , param.content()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
