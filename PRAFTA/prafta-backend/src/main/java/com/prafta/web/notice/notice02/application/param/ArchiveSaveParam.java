package com.prafta.web.notice.notice02.application.param;

import java.util.ArrayList;
import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice02.application.model.ArchiveFileModel;
import com.prafta.web.notice.notice02.dto.request.ArchiveFileRequest;
import com.prafta.web.notice.notice02.dto.request.ArchiveSaveRequest;

/**
 * 자료실 생성/수정 파라미터.
 * 첨부 다건은 Model 리스트로 담는다. cmpnyCd/userCd 는 JWT 클레임에서만 도출(IDOR 차단).
 * 팝업/고정/대상 개념이 없으므로 해당 필드를 받지 않는다(서버가 저장 시 강제 고정).
 */
public record ArchiveSaveParam(
    String noticeId
    , String archiveTypeCd
    , String title
    , String content
    , String editPwd
    , List<ArchiveFileModel> fileList
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String gvSiteCd
){
    public static ArchiveSaveParam from(ArchiveSaveRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        List<ArchiveFileModel> fileModels = new ArrayList<>();
        if (request.getFileList() != null) {
            for (ArchiveFileRequest f : request.getFileList()) {
                if (f == null) continue;
                fileModels.add(new ArchiveFileModel(f.getFileMgmtCd(), f.getSortIdx()));
            }
        }

        return new ArchiveSaveParam(
            request.getNoticeId()
            , request.getArchiveTypeCd()
            , request.getTitle()
            , request.getContent()
            , request.getEditPwd()
            , fileModels
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_siteCd()
        );
    }
}
