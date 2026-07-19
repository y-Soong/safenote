package com.prafta.web.subcon.subcon02.application.command;

/**
 * 미러 사업장(TB_SITE) 복제 INSERT 커맨드(PRAFTA-SUBCON-T2 §5-4 #3).
 *
 * <p>복제 값은 SQL(INSERT ... SELECT)이 원본 행에서 직접 읽는다(사용자 입력 미복제 — 주입 면 차단).
 * siteNo 는 DST 회사 내 중복 시 "SITE_NO-{LINK_ID}" 접미 보정된 확정값(D4)을 서비스가 주입한다.
 */
public record MirrorSiteInsertCommand(
    String srcCmpnyCd
    , String srcSiteCd
    , String dstCmpnyCd
    , String dstSiteCd
    , String siteNo
    , String insertNo
){
}
