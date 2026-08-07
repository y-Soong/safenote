package com.prafta.web.baim.baim07.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 정책 생성/변경 요청. baim07 POST /policy, PUT /policy/{policySeq} 공용.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.2 (7개 axis)
 *
 * <p>1차 검증(jakarta.validation)으로 형식만 점검하고, 비즈니스 검증(Cross-axis 매트릭스)은
 * {@code LeavePolicyServiceImpl.validateAxisMatrix}에서 일괄 처리한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class LeavePolicySaveRequest {

    // 통합 화면(Baim_07)에서 프리셋 개념이 제거되어 항상 "CUSTOM" 으로 정규화된다.
    // null/blank 로 들어와도 Param.from()에서 "CUSTOM" 으로 채우므로 @NotBlank 를 두지 않는다.
    @FieldLabel("프리셋")
    @Size(max = 30)
    private String policyPreset;

    // ===== 7개 axis =====
    @FieldLabel("AXIS1 부여 기준")
    @NotBlank
    @Size(max = 20)
    private String axis1GrantBase;

    @FieldLabel("AXIS2 회계연도 시작월")
    @Size(max = 2)
    private String axis2FiscalStartMm;

    @FieldLabel("AXIS2 회계연도 시작일")
    @Size(max = 2)
    private String axis2FiscalStartDd;

    @FieldLabel("AXIS3 첫해 처리")
    @NotBlank
    @Size(max = 30)
    private String axis3FirstYearMethod;

    @FieldLabel("AXIS3 일괄선부여")
    @Size(max = 1)
    private String axis3PregrantYn;

    @FieldLabel("AXIS4 반올림")
    @Size(max = 20)
    private String axis4ProrateRounding;

    @FieldLabel("AXIS5 근속 모드")
    @NotBlank
    @Size(max = 10)
    private String axis5TenureMode;

    @FieldLabel("AXIS5 가산 시작 연차")
    @NotNull
    @Min(1)
    @Max(99)
    private Integer axis5StartYear;

    @FieldLabel("AXIS5 가산 주기")
    @NotNull
    @Min(1)
    @Max(99)
    private Integer axis5Interval;

    @FieldLabel("AXIS5 최대 연차일수")
    @NotNull
    @Min(0)
    @Max(99)
    private Integer axis5MaxDays;

    @FieldLabel("AXIS6 유효기간")
    @NotNull
    @Min(1)
    @Max(99)
    private Integer axis6ValidityMonths;

    @FieldLabel("AXIS7 사용촉진")
    @NotBlank
    @Size(max = 1)
    private String axis7UsePromotion;

    @FieldLabel("법정연차 결재 여부")
    @Size(max = 1)
    private String aprvUseYn;

    @FieldLabel("정책 적용 시작일")
    @NotBlank
    @Size(max = 8)
    private String applyFromDate;

    // ===== TB_LEAVE_USAGE_POLICY =====
    // prafta-024: 사용 단위를 단일 선택으로 전환.
    // HB-04(2026-08-07): 반반차 폐지 — 선택지는 FULL_DAY/HALF_DAY/HOUR_2/HOUR_1/MIN_30 5종이다.
    //   구 'QUARTER_DAY' 는 서버에서 HALF_DAY 로 축소 정규화된다(fail-closed, 신규 선택 불가).
    // 값 화이트리스트 및 AXIS4=HALF_DAY 강제 규칙은 LeavePolicyServiceImpl 에서 검증/정규화한다.
    @FieldLabel("사용 단위")
    @Size(max = 20)
    private String usageUnit;

    // PC-05(D3): 짜투리 잔여 보전 옵션. 'Y' = 잔여 < 최소 사용단위 요금일 때 최소단위 1건 사용
    //   허용 + 부족분 회사 부담 / 'N'(기본) = 시스템 미개입(소멸 임박 리포트 지원).
    //   Y/N 정규화는 LeavePolicyServiceImpl.buildNewPolicyVO 에서 수행(비정상 값 → 'N').
    @FieldLabel("짜투리 잔여 보전")
    @Size(max = 1)
    private String allowRemnantRoundUp;

    // ===== 메타 =====
    @FieldLabel("변경 사유")
    @Size(max = 500)
    private String changeReason;
}
