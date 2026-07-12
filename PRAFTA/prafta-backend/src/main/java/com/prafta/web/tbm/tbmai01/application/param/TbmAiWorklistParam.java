package com.prafta.web.tbm.tbmai01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbmai01.dto.request.TbmAiWorklistRequest;

/**
 * TBM AI 분석 워크리스트 조회 파라미터.
 *
 * <p>식별자(cmpnyCd/userCd/authCd/siteCd)는 JWT 클레임에서만 도출한다(IDOR 차단).
 *    page/size 는 여기서 기본·상한 정규화한다(page&lt;1→1, size null/&lt;1→20, size&gt;100→100).
 */
public record TbmAiWorklistParam(
    String keyword
    , String fileNm
    , String mtrlItemType
    , String aiStatus
    , String siteCd
    , int page
    , int size
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String gvSiteCd
) {
    /** 페이지 크기 기본값. */
    private static final int DEFAULT_SIZE = 20;
    /** 페이지 크기 상한(과대조회 방어). */
    private static final int MAX_SIZE = 100;
    /** [Low] 페이지 번호 상한(offset = (page-1)*size int 오버플로우 방어). 100000*100 = 1천만으로 int 범위 내. */
    private static final int MAX_PAGE = 100000;

    public static TbmAiWorklistParam from(TbmAiWorklistRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // 페이지 정규화(1-base, 하한 1, 상한 MAX_PAGE 클램프로 offset int 오버플로우 방어)
        int page = (request.getPage() == null || request.getPage() < 1) ? 1 : Math.min(request.getPage(), MAX_PAGE);
        // 크기 정규화(기본 20, 상한 100 클램프)
        int size;
        if (request.getSize() == null || request.getSize() < 1) {
            size = DEFAULT_SIZE;
        } else {
            size = Math.min(request.getSize(), MAX_SIZE);
        }

        return new TbmAiWorklistParam(
            request.getKeyword()
            , request.getFileNm()
            , request.getMtrlItemType()
            , request.getAiStatus()
            , request.getSiteCd()
            , page
            , size
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_siteCd()
        );
    }
}
