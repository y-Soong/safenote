package com.prafta.web.subcon.subcon02.application.command;

import lombok.Getter;

/**
 * 사업장 연동 링크 INSERT 커맨드(STATUS='PROPOSED' 고정).
 *
 * <p>linkId 는 useGeneratedKeys(bigint AUTO_INCREMENT) 회수용이라 record 가 아닌
 * 클래스로 둔다(MyBatis 가 setter 로 주입 — T1 RelationInsertCommand 패턴 승계). 나머지 필드는 불변.
 */
@Getter
public class SiteLinkInsertCommand {

    /** 생성된 링크ID(useGeneratedKeys 회수). */
    private Long linkId;

    private final Long relationId;
    private final String srcCmpnyCd;
    private final String srcSiteCd;
    private final String dstCmpnyCd;
    private final String proposeUserCd;
    private final String insertNo;

    public SiteLinkInsertCommand(Long relationId, String srcCmpnyCd, String srcSiteCd,
            String dstCmpnyCd, String proposeUserCd, String insertNo) {
        this.relationId = relationId;
        this.srcCmpnyCd = srcCmpnyCd;
        this.srcSiteCd = srcSiteCd;
        this.dstCmpnyCd = dstCmpnyCd;
        this.proposeUserCd = proposeUserCd;
        this.insertNo = insertNo;
    }

    public void setLinkId(Long linkId) {
        this.linkId = linkId;
    }
}
