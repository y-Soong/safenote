package com.prafta.web.subcon.subcon03.application.command;

import lombok.Getter;

/**
 * 스냅샷 헤더 INSERT 커맨드(PRAFTA-SUBCON-T3 §5-6 #6).
 *
 * <p>ownerCmpnyCd 는 클라 바디가 아니라 <b>DB 의 SHARE_REQ.REQ_CMPNY_CD</b> 에서만 주입한다
 * (수신사 테넌트 스코프의 근거 — 위조 시 타사 열람 가능). snapshotId 는 useGeneratedKeys 회수용.
 */
@Getter
public class SnapshotInsertCommand {

    /** 생성된 스냅샷ID(useGeneratedKeys 회수). */
    private Long snapshotId;

    private final Long shareReqId;
    private final String ownerCmpnyCd;
    private final int version;
    private final String unclosedIncludedYn;
    private final int consentExcludedCnt;
    private final String relayIncludedYn;
    private final String insertNo;

    public SnapshotInsertCommand(Long shareReqId, String ownerCmpnyCd, int version, String unclosedIncludedYn,
            int consentExcludedCnt, String relayIncludedYn, String insertNo) {
        this.shareReqId = shareReqId;
        this.ownerCmpnyCd = ownerCmpnyCd;
        this.version = version;
        this.unclosedIncludedYn = unclosedIncludedYn;
        this.consentExcludedCnt = consentExcludedCnt;
        this.relayIncludedYn = relayIncludedYn;
        this.insertNo = insertNo;
    }

    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
    }
}
