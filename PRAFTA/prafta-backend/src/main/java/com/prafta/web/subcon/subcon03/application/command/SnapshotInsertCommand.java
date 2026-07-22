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

    /**
     * [PS-04] 마감분만 부분 포함 표식(D-3) — 'Y':커버리지 필터로 일부 제외(자체 또는 릴레이 병합) /
     * 'N':전체 포함. 신규 스냅샷에 NULL 금지(NULL 은 구본 전용 값 — D-4).
     */
    private final String closedPartialYn;

    /**
     * [PS-04] 마감 커버리지 요약 JSON(월·부서명 단위까지 — 성명/USER_CD 절대 금지, 공통 §11).
     * closedOnlyYn='N'/RISK/NEARMISS 는 null(가이드 대상 아님).
     */
    private final String coverageMeta;

    private final int consentExcludedCnt;
    private final String relayIncludedYn;
    private final String insertNo;

    public SnapshotInsertCommand(Long shareReqId, String ownerCmpnyCd, int version, String unclosedIncludedYn,
            String closedPartialYn, String coverageMeta,
            int consentExcludedCnt, String relayIncludedYn, String insertNo) {
        this.shareReqId = shareReqId;
        this.ownerCmpnyCd = ownerCmpnyCd;
        this.version = version;
        this.unclosedIncludedYn = unclosedIncludedYn;
        this.closedPartialYn = closedPartialYn;
        this.coverageMeta = coverageMeta;
        this.consentExcludedCnt = consentExcludedCnt;
        this.relayIncludedYn = relayIncludedYn;
        this.insertNo = insertNo;
    }

    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
    }
}
