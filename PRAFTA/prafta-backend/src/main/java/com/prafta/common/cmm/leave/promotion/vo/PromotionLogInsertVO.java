package com.prafta.common.cmm.leave.promotion.vo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code tb_leave_promotion_log} 촉진 진행 마스터 INSERT 1행 운반체 (PRAFTA-COM-008-A-0/A-2).
 *
 * <p>1차 통지(A-2)·2차 직권지정(A-4)이 회차별 1행을 멱등 적재한다.
 * 멱등은 UNIQUE(CMPNY_CD, DEDUP_KEY)로 강제하며, 중복 INSERT 는 DuplicateKeyException 으로
 * 상위(서비스)가 흡수한다.
 *
 * <p>PROMO_ID 는 서비스에서 {@code selectNextPromoId} 로 채번하여 세팅한다.
 */
@Getter
@Setter
public class PromotionLogInsertVO {

    /** 촉진 진행 ID (PK, varchar(20)) — 서비스에서 채번하여 세팅 */
    private String promoId;

    /** 회사 코드 */
    private String cmpnyCd;

    /** 사업장 코드 (도래 판정 시점 사용자 소속, NULL 허용) */
    private String siteCd;

    /** 대상 근로자 코드 */
    private String userCd;

    /** 역산 기준 본연차 부여 ID */
    private String baseGrantId;

    /** 역산 기준 본연차 사용가능 종료일 (YYYYMMDD) */
    private String baseAvailToDate;

    /** 촉진 단계 [SYS068] FIRST / SECOND */
    private String promoStage;

    /** 1차 통지 발송일 (YYYYMMDD, 1차 행에만) */
    private String noticedDate;

    /** 1차 자발 지정 일수 스냅샷 */
    private BigDecimal stage1DesignatedDays;

    /** 2차 직권 지정 대상 잔여 일수 */
    private BigDecimal stage2TargetDays;

    /** 2차 직권 지정 통보일 (YYYYMMDD, 2차 행에) */
    private String stage2DesignatedDate;

    /** 진행 상태 [SYS075] NOTICED / DESIGNATED / COMPLETED / CLOSED */
    private String status;

    /** 중복 통지/지정 방지 키 */
    private String dedupKey;

    /** 등록자 */
    private String insertNo;
}
