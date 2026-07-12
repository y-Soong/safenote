package com.prafta.platform.company.application.command;

/**
 * 신규 고객사 최초 노드(TB_SITE_NODE) INSERT 커맨드.
 *
 * <p>Baim01.insertSiteNodeInfo 의 최초 1depth 노드 패턴(NODE_CD='n1', NODE_TYPE='00001')을 미러하되,
 * 노드-관리자 정합성 가드(User01.selectNodeHasAdmin)를 만족시키기 위해 MAIN_ADMIN_CD 에 신규 master
 * USER_CD 를 지정한다(관리·승인 주체 부재 방지 — 이후 일반 사용자 생성 가능).
 */
public record SiteNodeInsertCommand(
    String cmpnyCd
    , String siteCd
    , String nodeCd
    , String nodeNm
    , String nodeType
    , String mainAdminCd
    , String insertNo
) {
}
