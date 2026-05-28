package com.prafta.web.user.user01.dto.response;

import java.util.List;

import com.prafta.web.user.user01.result.UserHireDateHistoryResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 입사일 변경 이력 목록 응답.
 * list는 INSERT_DATE 내림차순(최신 변경이 첫 행)으로 정렬되어 내려간다.
 */
@Getter
@Builder
public class HireDateHistoryResponse {
    private List<UserHireDateHistoryResult> list;
}
