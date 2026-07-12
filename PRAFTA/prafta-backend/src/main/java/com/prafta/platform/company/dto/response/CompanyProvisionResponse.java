package com.prafta.platform.company.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 신규 고객사 프로비저닝 응답 DTO.
 *
 * <p>운영자가 고객사 관리자에게 전달할 최소 정보만 담는다.
 * 초기 비밀번호 평문은 응답하지 않으며(보안), "초기 비밀번호 = 휴대폰번호" 안내 문구만 제공한다.
 */
@Value
@Builder
public class CompanyProvisionResponse {

    /** 발급된 회사코드(20자 랜덤). */
    String cmpnyCd;

    /** 최초 master 계정 로그인 ID. */
    String masterUserId;

    /** 초기 비밀번호 안내 문구(평문 비밀번호 미포함). */
    String initialPasswordGuide;
}
