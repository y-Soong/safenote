package com.prafta.web.notice.notice01.application.command;

import java.util.List;

import com.prafta.web.notice.notice01.application.model.NoticeFileModel;

/**
 * 공지 첨부 다건 INSERT 커맨드(foreach). tb_file_info 에 선저장된 FILE_MGMT_CD 를 매핑.
 */
public record NoticeFileSaveCommand(
    String noticeId
    , List<NoticeFileModel> fileList
    , String gvCmpnyCd
    , String gvUserCd
){
    public static NoticeFileSaveCommand of(String noticeId, List<NoticeFileModel> fileList,
                                           String gvCmpnyCd, String gvUserCd) {
        return new NoticeFileSaveCommand(noticeId, fileList, gvCmpnyCd, gvUserCd);
    }
}
