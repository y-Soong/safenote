package com.prafta.common.cmm.leave.vo;

import lombok.Builder;
import lombok.Getter;

/**
 * 페이징 메타(요소 목록 없이 page/size/totalCount만).
 *
 * <p>요소 목록을 별도 필드로 노출하는 응답(예: attd09 대시보드)에서
 * 목록 중복을 피하기 위해 {@link PagedResult}와 분리하여 사용한다.
 */
@Getter
@Builder
public class PagingMetaVO {

    /** 현재 페이지(1-based) */
    private final int page;

    /** 페이지 크기 */
    private final int size;

    /** 전체 행 수 */
    private final long totalCount;
}
