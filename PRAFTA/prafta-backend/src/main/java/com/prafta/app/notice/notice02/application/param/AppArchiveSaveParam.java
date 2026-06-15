package com.prafta.app.notice.notice02.application.param;

import java.util.ArrayList;
import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.app.notice.notice02.application.model.AppArchiveFileModel;
import com.prafta.app.notice.notice02.dto.request.AppArchiveFileRequest;
import com.prafta.app.notice.notice02.dto.request.AppArchiveSaveRequest;

/**
 * 앱 자료실 생성 파라미터. cmpnyCd/userCd/authCd/siteCd 는 JWT 클레임에서만 도출(IDOR 차단).
 * 첨부 다건은 Model 리스트로 담는다. 팝업/고정/대상 개념이 없으므로 받지 않는다(서버 강제 고정).
 */
public record AppArchiveSaveParam(
    String archiveTypeCd
    , String title
    , String content
    , String editPwd
    , List<AppArchiveFileModel> fileList
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String gvSiteCd
){
    public static AppArchiveSaveParam from(AppArchiveSaveRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        List<AppArchiveFileModel> fileModels = new ArrayList<>();
        if (request.getFileList() != null) {
            for (AppArchiveFileRequest f : request.getFileList()) {
                if (f == null) continue;
                fileModels.add(new AppArchiveFileModel(f.getFileMgmtCd(), f.getSortIdx()));
            }
        }

        return new AppArchiveSaveParam(
            request.getArchiveTypeCd()
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
