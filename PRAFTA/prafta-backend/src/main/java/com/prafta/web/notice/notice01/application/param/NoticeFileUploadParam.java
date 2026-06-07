package com.prafta.web.notice.notice01.application.param;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 공지 첨부 단건 업로드 파라미터.
 *
 * <p>cmpnyCd/userCd/siteCd 는 JWT 클레임에서만 도출(IDOR 차단)하고, 파일 본문은 multipart 로 받는다.
 * tb_file_info 에 선저장 후 발급된 FILE_MGMT_CD 를 프론트가 save-notice 의 fileList 에 매핑한다.
 */
public record NoticeFileUploadParam(
    String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , MultipartFile file
){
    public static NoticeFileUploadParam from(TokenInfo tokenInfo, MultipartFile file) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        if (file == null || file.isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new NoticeFileUploadParam(
            tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , file
        );
    }
}
