package com.prafta.web.subcon.subcon01.application.command;

import lombok.Getter;

/**
 * 연동 관계 INSERT 커맨드(STATUS='REQUESTED' 고정).
 *
 * <p>relationId 는 useGeneratedKeys(bigint AUTO_INCREMENT) 회수용이라 record 가 아닌
 * 클래스로 둔다(MyBatis 가 setter 로 주입). 나머지 필드는 불변.
 */
@Getter
public class RelationInsertCommand {

    /** 생성된 관계ID(useGeneratedKeys 회수). */
    private Long relationId;

    private final String reqCmpnyCd;
    private final String tgtCmpnyCd;
    private final String reqUserCd;
    private final String insertNo;

    public RelationInsertCommand(String reqCmpnyCd, String tgtCmpnyCd, String reqUserCd, String insertNo) {
        this.reqCmpnyCd = reqCmpnyCd;
        this.tgtCmpnyCd = tgtCmpnyCd;
        this.reqUserCd = reqUserCd;
        this.insertNo = insertNo;
    }

    public void setRelationId(Long relationId) {
        this.relationId = relationId;
    }
}
