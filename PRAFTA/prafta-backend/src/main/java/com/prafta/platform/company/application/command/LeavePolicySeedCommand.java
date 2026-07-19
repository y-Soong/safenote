package com.prafta.platform.company.application.command;

import lombok.Getter;
import lombok.Setter;

/**
 * 신규 고객사 기본 연차정책 시드 Command (PRAFTA 멀티테넌시 3/3).
 *
 * <p>정책 INSERT 후 AUTO_INCREMENT 로 발급된 {@code policySeq} 를 회수해
 * 사용정책(TB_LEAVE_USAGE_POLICY) 1:1 INSERT 에 그대로 재사용한다.
 * MyBatis 가 keyProperty 로 값을 되돌려 넣어야 하므로 record 가 아닌 가변 클래스로 둔다.
 */
@Getter
@Setter
public class LeavePolicySeedCommand {

    /** 대상 회사코드(신규 발급분). */
    private final String cmpnyCd;

    /** 정책 적용 시작일(YYYYMMDD, 생성일). */
    private final String applyFromDate;

    /** 감사 컬럼 기록자(프로비저닝을 수행한 플랫폼 운영자). */
    private final String insertNo;

    /** 정책 INSERT 후 MyBatis 가 채워 넣는 AUTO_INCREMENT 키. 사용정책 INSERT 에 사용한다. */
    private Long policySeq;

    public LeavePolicySeedCommand(String cmpnyCd, String applyFromDate, String insertNo) {
        this.cmpnyCd = cmpnyCd;
        this.applyFromDate = applyFromDate;
        this.insertNo = insertNo;
    }
}
