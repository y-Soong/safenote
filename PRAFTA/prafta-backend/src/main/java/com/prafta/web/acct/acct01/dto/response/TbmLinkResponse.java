package com.prafta.web.acct.acct01.dto.response;

import java.util.List;

import com.prafta.web.acct.acct01.result.TbmLinkResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TbmLinkResponse {
    private List<TbmLinkResult> tbmList;
    private String notice;
}
