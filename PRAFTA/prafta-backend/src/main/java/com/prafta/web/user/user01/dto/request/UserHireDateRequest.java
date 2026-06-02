package com.prafta.web.user.user01.dto.request;

import java.math.BigDecimal;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 입사일 변경 요청 (PRAFTA-017-4 → prafta-032 수동 연차 조정 전환).
 *
 * <p>prafta-032(D1): 처리방식 자동계산(handlingType, SYS039)을 폐기한다. 입사일 UPDATE +
 * 변경 이력 INSERT + (관리자 입력 목표 법정 부여량과의) 차액 추가/회수를 단일 트랜잭션으로 수행한다.
 * 차액 = (목표 법정 부여량) − (현재 법정 부여량). 양수면 추가 부여(D4), 음수면 회수(D5), 0이면 무처리.
 * cmpnyCd는 서버가 토큰으로 강제하므로 신뢰하지 않는다(IDOR 방지).
 */
@Getter
@Setter
@NoArgsConstructor
public class UserHireDateRequest {
    private String cmpnyCd;

    @FieldLabel("사용자코드")
    @NotBlank
    @Size(max = 20)
    private String userCd;

    /** YYYY-MM-DD 또는 YYYYMMDD (서버에서 YYYYMMDD로 정규화 후 형식 재검증) */
    @FieldLabel("변경할 입사일")
    @NotBlank
    @Size(max = 10)
    private String newHireDate;

    /**
     * 목표 법정 부여량(법정 휴가 STATUTORY_* 총 부여 일수, prafta-032 D2). nullable.
     * 미입력(null) 시 현재값 유지(=차액 0, 연차 조정 없음).
     */
    @FieldLabel("수정 법정 부여량")
    private BigDecimal targetStatutoryGrantDays;

    /** 변경 사유 (CHANGE_REASON NOT NULL, varchar(1000)) */
    @FieldLabel("변경 사유")
    @NotBlank
    @Size(max = 1000)
    private String changeReason;

    /**
     * 회수 사유(prafta-032 D3/D5, WITHDRAW_REASON varchar(500)). nullable.
     * 차액 음수(회수 발생) 시 서비스 계층에서 필수 검증한다.
     */
    @FieldLabel("회수 사유")
    @Size(max = 500)
    private String withdrawReason;
}
