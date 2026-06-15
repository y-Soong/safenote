package com.prafta.app.notice.notice02.application.command;

import java.util.List;

import com.prafta.app.notice.notice02.application.model.AppArchiveFileModel;

/**
 * 앱 자료실 첨부 다건 INSERT 커맨드(foreach). tb_notice_file 공용 매핑 재사용.
 */
public record AppArchiveFileSaveCommand(
    String noticeId
    , List<AppArchiveFileModel> fileList
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AppArchiveFileSaveCommand of(String noticeId, List<AppArchiveFileModel> fileList,
                                               String gvCmpnyCd, String gvUserCd) {
        return new AppArchiveFileSaveCommand(noticeId, fileList, gvCmpnyCd, gvUserCd);
    }
}
