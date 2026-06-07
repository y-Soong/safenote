package com.prafta.web.notice.notice01.application.command;

import java.util.List;

import com.prafta.web.notice.notice01.application.model.NoticeTargetModel;

/**
 * 공지 대상 다건 INSERT 커맨드(foreach).
 * targetList 의 순번(TARGET_SEQ)은 mapper 의 foreach index 로 채번한다.
 */
public record NoticeTargetSaveCommand(
    String noticeId
    , List<NoticeTargetModel> targetList
    , String gvCmpnyCd
    , String gvUserCd
){
    public static NoticeTargetSaveCommand of(String noticeId, List<NoticeTargetModel> targetList,
                                             String gvCmpnyCd, String gvUserCd) {
        return new NoticeTargetSaveCommand(noticeId, targetList, gvCmpnyCd, gvUserCd);
    }
}
