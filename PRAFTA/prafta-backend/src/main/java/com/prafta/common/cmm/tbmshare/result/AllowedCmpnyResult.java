package com.prafta.common.cmm.tbmshare.result;

/**
 * 입실 허용 회사 1행(PRAFTA-SUBCON-T5) — 개설사 + 지정 체인.
 *
 * <p>웹 콘솔의 "대상 회사" 셀렉트 소스. {@code cmpnyNm} 은 <b>조회자 기준 relabel</b> 된 표시명이며
 * (2차 이하 회사는 1차 회사명으로 접힘), 실제 데이터 기록은 {@code cmpnyCd} 로 한다.
 */
public record AllowedCmpnyResult(
    String cmpnyCd
    , String cmpnyNm
) {
}
