package com.prafta.web.subcon.subcon01.dto.response;

import com.prafta.web.subcon.subcon01.result.CmpnyExactResult;

import lombok.Builder;
import lombok.Value;

/**
 * 회사 정확일치 조회 응답.
 *
 * <p>미존재/비활성/자기회사 전부 동일한 200 + cmpny=null(사유 무구분 — 열거 방지, §6-1).
 */
@Value
@Builder
public class CmpnyExactSearchResponse {
    CmpnyExactResult cmpny;
}
