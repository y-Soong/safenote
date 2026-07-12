package com.prafta.platform.company.application.command;

/**
 * 신규 고객사 최초 사업장(TB_SITE) INSERT 커맨드.
 *
 * <p>Baim01(사업장 관리) INSERT 패턴을 미러한다. 프로비저닝 시 SITE_NM=회사명,
 * SITE_ADMIN_CD=신규 master USER_CD, STR_DATE=오늘(YYYYMMDD)로 1건 생성한다.
 */
public record SiteInsertCommand(
    String cmpnyCd
    , String siteCd
    , String siteNo
    , String siteNm
    , String strDate
    , String siteAdminCd
    , String insertNo
) {
}
