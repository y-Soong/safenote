package com.prafta.web.risk.riskai01.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 도출된 유해요인/개선안 1건. {@code markers} 는 근거 각주([n]) 배열(코퍼스 근거부재 자유생성이면 빈 배열).
 *
 * <p>응답 DTO 겸 HAZARD_JSON/MEASURE_JSON 저장 스키마로 공용한다(Jackson 직렬화/역직렬화).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DerivedItem(
    String text
    , List<String> markers
) {}
