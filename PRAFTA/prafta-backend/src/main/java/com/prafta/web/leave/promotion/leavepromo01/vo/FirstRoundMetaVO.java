package com.prafta.web.leave.promotion.leavepromo01.vo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 1차 독촉 대상자의 회차 메타 운반체(서버 재조회 — 클라 불신뢰, {@code DesignateTargetMetaVO} 미러).
 *
 * <p>본 VO 가 null 이면 "1차 통지 대상 아님(FIRST 마스터 부재)" 또는 "스코프 밖(타 사업장/비활성 계정)"
 * 이므로 독촉을 건너뛴다.
 */
@Getter
@Setter
public class FirstRoundMetaVO {

    /** 대상 사용자 사업장 코드(서버 조회, 권한 검증·outbox 적재용). */
    private String siteCd;

    /** 대상 사용자 소속 부서 코드(서버 조회). */
    private String nodeCd;

    /** FIRST 회차 기준 본연차 만료일 (YYYYMMDD). */
    private String baseAvailToDate;

    /** 1차 통지일 (YYYYMMDD). 제출 기한(=통지일+10일) 산출 기준. */
    private String noticedDate;

    /** 1차 자발 지정 일수 스냅샷. 0 이면 미제출(독촉 가능). */
    private BigDecimal stage1DesignatedDays;
}
