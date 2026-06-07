package com.prafta.app.tbm.admin.dto.response;

import java.util.List;

import com.prafta.app.tbm.admin.result.AdminContentOptionResult;

import lombok.Builder;
import lombok.Getter;

/** T-K 콘텐츠 선택 옵션 응답. */
@Getter
@Builder
public class AdminContentOptionResponse {
    private List<AdminContentOptionResult> contentList;
}
