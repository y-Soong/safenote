package com.prafta.common.cmm.stdwork.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 소정-02: TB_USER_STD_WORK_HOURS 단건 운반체 (근로자별 소정근로시간 이력 1행).
 *
 * <p>DDL: {@code sql/migration/sojeong-1-1-user-std-work-hours-ddl.sql}
 *
 * <p>이력 원칙상 한 근로자에 여러 행이 존재하며, 특정일 기준 유효 행은 1건이다
 * ({@code APPLY_STR_DATE <= 기준일 <= COALESCE(APPLY_END_DATE, '99991231')}).
 */
@Getter
@Setter
public class StdWorkHoursVO {

    /** 회사 코드 */
    private String cmpnyCd;

    /** 사용자 코드 */
    private String userCd;

    /** 적용 시작일 (YYYYMMDD, 당일 포함) */
    private String applyStrDate;

    /** 적용 종료일 (YYYYMMDD, 당일 포함. null = 무기한) */
    private String applyEndDate;

    /** 주 소정근로 분 (2400 = 주 40시간) */
    private Integer weekStdMinutes;

    /** 사유코드 [SYS083] — {@code StdWorkReasonCd} */
    private String reasonCd;

    /** 사유 명칭 (SYS083 조인 결과, 목록 표기용) */
    private String reasonNm;

    /** 사유 상세 (자유 텍스트) */
    private String reasonDetail;

    /** 입력자 */
    private String insertNo;

    /** 입력일시 (yyyy-MM-dd'T'HH:mm:ss) */
    private String insertDate;

    /** 수정자 */
    private String updateNo;

    /** 수정일시 (yyyy-MM-dd'T'HH:mm:ss) */
    private String updateDate;
}
