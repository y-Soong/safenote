package com.prafta.platform.common.command;

/**
 * TB_CMPNY 단건 INSERT 커맨드(플랫폼 영역 공용).
 *
 * <p>신규 고객사 프로비저닝 및 최초 플랫폼 운영자 회사(prafta_system_admin) 생성에 사용한다.
 * CONTRACT_END_DATE 는 별도 마이그({@code prafta-cmpny-contract-end-date.sql})로 추가된 컬럼이며,
 * 무기한/미정 계약은 null 을 허용한다.
 */
public record CompanyInsertCommand(
    String cmpnyCd
    , String cmpnyNm
    , String bsnsLcnNo
    , String useYn
    , String contractYn
    , String contractEndDate
    , String insertNo
) {
}
