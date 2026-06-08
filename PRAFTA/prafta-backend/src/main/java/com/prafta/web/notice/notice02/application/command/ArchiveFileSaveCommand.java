package com.prafta.web.notice.notice02.application.command;

import java.util.List;

import com.prafta.web.notice.notice02.application.model.ArchiveFileModel;

/**
 * 자료실 첨부 다건 INSERT 커맨드(foreach). tb_notice_file 공용 매핑 재사용.
 */
public record ArchiveFileSaveCommand(
    String noticeId
    , List<ArchiveFileModel> fileList
    , String gvCmpnyCd
    , String gvUserCd
){
    public static ArchiveFileSaveCommand of(String noticeId, List<ArchiveFileModel> fileList,
                                            String gvCmpnyCd, String gvUserCd) {
        return new ArchiveFileSaveCommand(noticeId, fileList, gvCmpnyCd, gvUserCd);
    }
}
