package com.prafta.web.subcon.subcon03.dto.response;

import java.util.List;

import com.prafta.web.subcon.subcon03.result.ChainSiteResult;
import com.prafta.web.subcon.subcon03.result.ShareCmpnyResult;

import lombok.Builder;
import lombok.Value;

/**
 * 공유 요청 생성 후보 응답(관계 ACCEPTED 상대 회사 + 그 회사와 체인이 있는 내 사업장).
 *
 * <p>siteList 는 prvCmpnyCd 를 지정했을 때만 채워진다(제공사 사업장 목록은 어떤 경우에도 미노출).
 */
@Value
@Builder
public class ShareReqCandidatesResponse {
    List<ShareCmpnyResult> cmpnyList;
    List<ChainSiteResult> siteList;
}
