package com.prafta.web.subcon.subcon03.application.command;

import lombok.Getter;

/**
 * 데이터 공유 요청 INSERT 커맨드(STATUS='REQUESTED' 고정).
 *
 * <p>shareReqId 는 useGeneratedKeys(bigint AUTO_INCREMENT) 회수용이라 record 가 아닌 클래스로 둔다
 * (MyBatis 가 setter 로 주입 — T2 SiteLinkInsertCommand 패턴 승계). 나머지 필드는 불변.
 * targetSiteCd 는 클라 입력이 아니라 서버가 사업장 연동 체인으로 해석한 값만 들어온다.
 */
@Getter
public class ShareReqInsertCommand {

    /** 생성된 공유요청ID(useGeneratedKeys 회수). */
    private Long shareReqId;

    private final Long relationId;
    private final String reqCmpnyCd;
    private final String reqSiteCd;
    private final String prvCmpnyCd;
    private final String targetSiteCd;
    private final String dataType;
    private final String periodStr;
    private final String periodEnd;
    private final String closedOnlyYn;
    private final String purpose;
    private final String reqUserCd;
    private final String insertNo;

    public ShareReqInsertCommand(Long relationId, String reqCmpnyCd, String reqSiteCd, String prvCmpnyCd,
            String targetSiteCd, String dataType, String periodStr, String periodEnd, String closedOnlyYn,
            String purpose, String reqUserCd, String insertNo) {
        this.relationId = relationId;
        this.reqCmpnyCd = reqCmpnyCd;
        this.reqSiteCd = reqSiteCd;
        this.prvCmpnyCd = prvCmpnyCd;
        this.targetSiteCd = targetSiteCd;
        this.dataType = dataType;
        this.periodStr = periodStr;
        this.periodEnd = periodEnd;
        this.closedOnlyYn = closedOnlyYn;
        this.purpose = purpose;
        this.reqUserCd = reqUserCd;
        this.insertNo = insertNo;
    }

    public void setShareReqId(Long shareReqId) {
        this.shareReqId = shareReqId;
    }
}
