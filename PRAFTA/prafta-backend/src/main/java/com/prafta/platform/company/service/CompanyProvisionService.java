package com.prafta.platform.company.service;

import com.prafta.platform.company.application.param.CompanyProvisionParam;
import com.prafta.platform.company.dto.response.CmpnyCdCheckResponse;
import com.prafta.platform.company.dto.response.CompanyProvisionResponse;

/**
 * 신규 고객사 프로비저닝 서비스.
 *
 * <p>회사/최초 사업장/노드/master 계정/권한·운영사변수 템플릿 복제/기본 근무타입/휴일 시드를
 * 단일 트랜잭션으로 all-or-nothing 생성한다(휴일 동기화만 best-effort 분리).
 */
public interface CompanyProvisionService {

    CompanyProvisionResponse provisionCompany(CompanyProvisionParam param);

    /**
     * 회사코드 사용 가능 여부(입력 즉시 안내용).
     *
     * @param cmpnyCd 운영자가 입력한 회사코드(정규화 전 원본)
     * @return 정규화 결과·형식 유효성·중복 여부
     */
    CmpnyCdCheckResponse checkCmpnyCdAvailable(String cmpnyCd);
}
