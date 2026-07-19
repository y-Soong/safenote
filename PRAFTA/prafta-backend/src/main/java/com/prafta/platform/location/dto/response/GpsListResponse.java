package com.prafta.platform.location.dto.response;

import java.util.List;

import com.prafta.platform.location.application.result.GpsInfoResult;

import lombok.Builder;
import lombok.Value;

/**
 * 위치정보 조회 응답 DTO (GET /platformApi/location/gps-lists).
 *
 * <p>PII 최소화: USER_CD 중심 — 실명/휴대폰 평문 미포함(요청서 §5-3).
 */
@Value
@Builder
public class GpsListResponse {

    /** 위치정보 목록(측정시각 오름차순, 최대 1000건). */
    List<GpsInfoResult> gpsList;

    /** 반환 건수(열람 로그 RESULT_CNT 와 동일). */
    int resultCnt;

    /** 1000건 초과 절단 여부(true 면 프론트가 안내 배너 노출). */
    boolean truncated;
}
