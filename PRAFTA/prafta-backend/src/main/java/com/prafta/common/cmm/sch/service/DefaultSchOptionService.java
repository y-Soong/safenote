package com.prafta.common.cmm.sch.service;

import java.util.List;

import com.prafta.common.cmm.sch.vo.SchOptionVO;

/**
 * 기본 근무타입 옵션 조회/검증 공용 서비스 (PRAFTA-COM-008-E-5/E-8).
 *
 * <p>User_01 기본 근무타입 select 채움, 로그인 게이트 팝업 선택지 채움, 저장 시 화이트리스트 검증을
 * 단일 출처로 제공한다(클라 제출값 신뢰 금지).
 */
public interface DefaultSchOptionService {

    /**
     * 대상 사업장의 활성(USE_YN='Y') 근무타입 옵션 목록.
     *
     * @param cmpnyCd 회사 코드
     * @param siteCd  사업장 코드
     * @return 옵션 목록(빈 목록 가능)
     */
    List<SchOptionVO> getActiveSchOptions(String cmpnyCd, String siteCd);

    /**
     * 선택한 기본 근무타입(schCd)이 해당 사업장의 활성 근무타입인지 검증한다.
     * blank 면 미설정으로 보고 false. (호출부에서 "선택지 없음"과 "유효하지 않음"을 구분 처리)
     *
     * @return 유효하면 true
     */
    boolean isValidDefaultSch(String cmpnyCd, String siteCd, String schCd);
}
