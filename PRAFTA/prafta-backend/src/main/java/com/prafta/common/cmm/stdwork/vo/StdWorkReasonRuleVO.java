package com.prafta.common.cmm.stdwork.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 소정-02: SYS083 사유코드의 <b>정책 규칙</b> 운반체.
 *
 * <p>2단계(분 단위 원장)의 차감·부여 분기가 소비할 데이터 진입점이다. 사유코드 상수를
 * if 로 나열하는 하드코딩을 막기 위해, 규칙을 tb_syst_val_d 의 INFO 컬럼에 싣고 여기로 읽는다
 * (plan §1.4 / §5.3, ★노무사 확인 포인트 N-1 은 이 데이터의 변경으로 흡수된다).
 */
@Getter
@Setter
public class StdWorkReasonRuleVO {

    /** 사유코드 [SYS083] */
    private String reasonCd;

    /** 사유 명칭 */
    private String reasonNm;

    /**
     * 종일 사용 시 차감 규칙 (VAL_D_INFO_1).
     *
     * <p>{@code FIXED_480} = 480분(1일 등가) 고정 / {@code DAILY_STD} = 그날 소정근로분.
     */
    private String deductRule;

    /**
     * 연차 부여 규칙 (VAL_D_INFO_2).
     *
     * <p>{@code NO_PRORATE} = 비례 없음 / {@code PRORATE} = 시행령 별표2 비례부여.
     */
    private String grantRule;

    /** 사유 설명 (근거 행정해석 포함) */
    private String reasonDesc;

    /** 정렬 순번 */
    private Integer sortIdx;
}
