package com.prafta.web.subcon.subcon03.dto.response;

import java.util.List;

import com.prafta.web.subcon.subcon03.result.ShareReqResult;

import lombok.Builder;
import lombok.Value;

/**
 * 데이터 공유 요청 목록 응답(전 상태 반환 — 목록이 곧 이력. 프론트가 direction 으로 보낸/받은 2분류).
 */
@Value
@Builder
public class ShareReqListResponse {
    List<ShareReqResult> reqs;
}
