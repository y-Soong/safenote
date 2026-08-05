package com.prafta.common.cmm.schedule.mapper.result;

/**
 * 연차 잠금일 1건(prafta-com-016 공통 스케줄 변경 가드 → E3 당일분모 전환 확장).
 * MyBatis map-underscore-to-camel-case 매핑(WORK_YMD→workYmd, USE_UNIT_TYPE→useUnitType,
 * PENDING_YN→pendingYn).
 */
public class LeaveLockDayResult {

    /** 잠긴 날짜(YYYYMMDD). */
    private String workYmd;

    /**
     * 그 날짜 연차의 사용단위 코드(USE_UNIT_TYPE). 종일='00'.
     * 미결 시간차 잠금(pendingYn='Y')이면 null(REQ 에 단위 미보유 — 시간차 확정 술어라 시간민감).
     */
    private String useUnitType;

    /**
     * 'Y' = 미결 시간차 신청(REQ_TYPE='05', REQ_STATUS='01', 시각 보유) 잠금(E3 신규).
     * 'N' = 확정 연차(use 행) 잠금(기존 com-016).
     */
    private String pendingYn;

    public String getWorkYmd() {
        return workYmd;
    }

    public void setWorkYmd(String workYmd) {
        this.workYmd = workYmd;
    }

    public String getUseUnitType() {
        return useUnitType;
    }

    public void setUseUnitType(String useUnitType) {
        this.useUnitType = useUnitType;
    }

    public String getPendingYn() {
        return pendingYn;
    }

    public void setPendingYn(String pendingYn) {
        this.pendingYn = pendingYn;
    }
}
