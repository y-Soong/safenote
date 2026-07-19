package com.prafta.common.cmm.dailycontract.application.command;

/**
 * 계약서 양식 신규 버전 INSERT 커맨드 (TB_DAILY_CONTRACT).
 *
 * <p>등록/교체는 한 트랜잭션 안에서 "기존 활성 USE_YN='N' → 버전 MAX+1 INSERT" 로 처리한다
 * (활성 1건 보장은 UX_DAILY_CONTRACT_ACTIVE 기능성 유니크가 백스톱).
 */
public record ContractInsertCommand(
    String cmpnyCd
    , String siteCd
    , int contractVer
    , String contractNm
    , String fileMgmtCd     // 계약서 이미지 파일코드(TB_FILE_INFO, FILE_TYPE[SYS010] 007)
    , String insertNo       // 등록 관리자 USER_CD
) {
}
