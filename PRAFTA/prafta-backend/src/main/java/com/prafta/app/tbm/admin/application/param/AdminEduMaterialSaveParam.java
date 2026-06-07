package com.prafta.app.tbm.admin.application.param;

import java.util.Collections;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.tbm.admin.dto.request.AdminEduMaterialItemRequest;
import com.prafta.app.tbm.admin.dto.request.AdminEduMaterialSaveRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * R5 교육자료 등록/수정 파라미터(멀티파트 A안).
 *
 * <p>mtrlCd: 등록이면 null, 수정이면 path 값. files: 멀티파트 신규 업로드 파일 배열(인덱스=item.fileIndex).
 * 식별자는 JWT 클레임에서만 도출(IDOR 차단).
 */
public record AdminEduMaterialSaveParam(
    String mtrlCd
    , String title
    , String mtrlType
    , String siteCd
    , String contents
    , String useYn
    , List<AdminEduMaterialItemRequest> items
    , List<MultipartFile> files
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static AdminEduMaterialSaveParam from(String mtrlCd, AdminEduMaterialSaveRequest request,
            List<MultipartFile> files, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AdminEduMaterialSaveParam(
            mtrlCd
            , request.getTitle()
            , request.getMtrlType()
            , request.getSiteCd()
            , request.getContents()
            , request.getUseYn()
            , request.getItems() != null ? request.getItems() : Collections.emptyList()
            , files != null ? files : Collections.emptyList()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
