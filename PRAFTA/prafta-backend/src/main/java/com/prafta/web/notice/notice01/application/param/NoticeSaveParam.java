package com.prafta.web.notice.notice01.application.param;

import java.util.ArrayList;
import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice01.application.model.NoticeFileModel;
import com.prafta.web.notice.notice01.application.model.NoticeTargetModel;
import com.prafta.web.notice.notice01.dto.request.NoticeFileRequest;
import com.prafta.web.notice.notice01.dto.request.NoticeSaveRequest;
import com.prafta.web.notice.notice01.dto.request.NoticeTargetRequest;

/**
 * 공지 생성/수정 파라미터.
 * 대상/첨부 다건은 Model 리스트로 담는다(ChkptInfoModel 패턴).
 * cmpnyCd/userCd 는 JWT 클레임에서만 도출(IDOR 차단).
 */
public record NoticeSaveParam(
    String noticeId
    , String title
    , String content
    , String editPwd
    , String targetScope
    , String includeDailyYn
    , String popupYn
    , String popupFromYmd
    , String popupToYmd
    , String pinYn
    , Integer pinOrder
    , List<NoticeTargetModel> targetList
    , List<NoticeFileModel> fileList
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String gvSiteCd
    , String gvNodeCd
){
    public static NoticeSaveParam from(NoticeSaveRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        List<NoticeTargetModel> targetModels = new ArrayList<>();
        if (request.getTargetList() != null) {
            for (NoticeTargetRequest t : request.getTargetList()) {
                if (t == null) continue;
                targetModels.add(new NoticeTargetModel(
                    t.getSiteCd(), t.getNodeCd(), t.getIncludeDescendantsYn()));
            }
        }

        List<NoticeFileModel> fileModels = new ArrayList<>();
        if (request.getFileList() != null) {
            for (NoticeFileRequest f : request.getFileList()) {
                if (f == null) continue;
                fileModels.add(new NoticeFileModel(f.getFileMgmtCd(), f.getSortIdx()));
            }
        }

        return new NoticeSaveParam(
            request.getNoticeId()
            , request.getTitle()
            , request.getContent()
            , request.getEditPwd()
            , request.getTargetScope()
            , request.getIncludeDailyYn()
            , request.getPopupYn()
            , request.getPopupFromYmd()
            , request.getPopupToYmd()
            , request.getPinYn()
            , request.getPinOrder()
            , targetModels
            , fileModels
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_nodeCd()
        );
    }
}
