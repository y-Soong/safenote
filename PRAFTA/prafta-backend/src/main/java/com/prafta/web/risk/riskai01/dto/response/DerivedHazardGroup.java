package com.prafta.web.risk.riskai01.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 도출된 유해요인 1건 + 해당 유해요인에 대한 개선안 그룹(v3).
 *
 * <p>{@code markers} 는 근거 각주([n]) 배열(코퍼스 근거부재 자유생성이면 빈 배열),
 *    {@code measures} 는 이 유해요인과 연결된 개선안(감소대책) 목록.
 * <p>응답 DTO 겸 HAZARD_JSON 저장 스키마로 공용한다(Jackson 직렬화/역직렬화).
 *    구버전 저장분([{text,markers}])은 파싱 시 measures=null → 서비스에서 빈 리스트로 정규화(하위호환 read).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DerivedHazardGroup(
    String text
    , List<String> markers
    , List<DerivedItem> measures
) {}
