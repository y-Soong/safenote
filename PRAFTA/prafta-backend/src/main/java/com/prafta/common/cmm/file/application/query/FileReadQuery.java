package com.prafta.common.cmm.file.application.query;

/**
 * 저장 파일 원본 read 조회 키(회사코드 + 파일코드).
 *
 * <p>PRAFTA-WEB_003: 위험성평가 유해요인 사진(INIT_FILE_MGMT_CD)을 서버측에서 바이트로 읽어
 *    Claude vision 에 전달하기 위한 조회 키. 식별자(cmpnyCd)는 호출부에서 JWT 로만 도출한다.
 */
public record FileReadQuery(
    String cmpnyCd
    , String fileMgmtCd
) {}
