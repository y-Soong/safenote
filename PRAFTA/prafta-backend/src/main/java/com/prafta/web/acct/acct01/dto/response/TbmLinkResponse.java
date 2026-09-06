package com.prafta.web.acct.acct01.dto.response;

import java.util.List;

import com.prafta.web.acct.acct01.result.TbmLinkResult;

import lombok.Builder;
import lombok.Getter;

/**
 * TBM 연계 조회 응답. victimSeq/victimUserNm 은 요청 victimSeq echo(prafta-065). 대표 경로면 null.
 */
@Getter
@Builder
public class TbmLinkResponse {
    private List<TbmLinkResult> tbmList;
    private String notice;
    private Integer victimSeq;
    private String victimUserNm;
}
