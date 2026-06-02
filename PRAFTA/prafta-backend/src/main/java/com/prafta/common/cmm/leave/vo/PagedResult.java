package com.prafta.common.cmm.leave.vo;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 페이징 결과 공용 운반체 (정책 변경 이력 등).
 *
 * <p>본 VO는 LeavePolicyService 응답 전용으로 도입되었다. 다른 모듈에서 사용 시
 * 별도 검토 후 위치 이동을 고려한다(common.dto 등).
 *
 * @param <T> 페이지 요소 타입
 */
@Getter
@Builder
public class PagedResult<T> {

    /** 현재 페이지(1-based) */
    private final int page;

    /** 페이지 크기 */
    private final int size;

    /** 전체 행 수 */
    private final long totalCount;

    /** 현재 페이지 요소 목록 */
    private final List<T> items;
}
