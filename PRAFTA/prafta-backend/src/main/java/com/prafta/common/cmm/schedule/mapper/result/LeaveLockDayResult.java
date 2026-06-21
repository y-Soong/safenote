package com.prafta.common.cmm.schedule.mapper.result;

/**
 * 확정 연차 잠금일 1건(prafta-com-016 공통 스케줄 변경 가드).
 * MyBatis map-underscore-to-camel-case 매핑(WORK_YMD→workYmd, USE_UNIT_TYPE→useUnitType).
 */
public class LeaveLockDayResult {

    /** 잠긴 날짜(YYYYMMDD). */
    private String workYmd;

    /** 그 날짜 연차의 사용단위 코드(USE_UNIT_TYPE). 종일='00'. */
    private String useUnitType;

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
}
