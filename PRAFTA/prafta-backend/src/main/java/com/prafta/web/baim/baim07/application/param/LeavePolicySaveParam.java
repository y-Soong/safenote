package com.prafta.web.baim.baim07.application.param;

import com.prafta.common.cmm.leave.command.LeavePolicyCommand;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim07.dto.request.LeavePolicySaveRequest;

/**
 * 정책 생성/변경 진입 Param.
 *
 * <p>JWT 클레임을 함께 운반하여 서비스 계층이 권한 가드를 수행할 수 있도록 한다
 * (정책서 §8.5.7 + PRAFTA-017 권한 가드 패턴).
 *
 * <p>본 Param은 일종의 어댑터로, body 입력값을 {@link LeavePolicyCommand}로 변환하는
 * {@link #toCommand()}를 제공한다.
 */
public record LeavePolicySaveParam(
      LeavePolicyCommand command
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {

    public static LeavePolicySaveParam from(LeavePolicySaveRequest request, TokenInfo tokenInfo) {
        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 통합 화면(Baim_07)에서 프리셋 개념이 제거되어 본 endpoint는 항상 CUSTOM 정책만 다룬다.
        // 보안 검토(PRAFTA-017-1 Medium): 요청값을 신뢰하지 않고 "CUSTOM" 으로 고정한다.
        // 임의 문자열(예: "ZZZ")이 POLICY_PRESET / HISTORY 스냅샷에 영속되는 데이터 정합성 결함 차단.
        // (axis1~7은 validateAxisMatrix가 enum 화이트리스트로 별도 검증)
        String policyPreset = "CUSTOM";

        LeavePolicyCommand command = new LeavePolicyCommand(
              policyPreset
            , request.getAxis1GrantBase()
            , request.getAxis2FiscalStartMm()
            , request.getAxis2FiscalStartDd()
            , request.getAxis3FirstYearMethod()
            , request.getAxis3PregrantYn()
            , request.getAxis4ProrateRounding()
            , request.getAxis5TenureMode()
            , request.getAxis5StartYear()
            , request.getAxis5Interval()
            , request.getAxis5MaxDays()
            , request.getAxis6ValidityMonths()
            , request.getAxis7UsePromotion()
            , request.getStatutoryAutoGrantYn() // 소정-05: 미전송/비정상 값은 서비스에서 'Y'(기존 동작) 정규화
            , request.getAprvUseYn()
            , request.getApplyFromDate()
            , request.getUsageUnit() // LC-10: 반반차는 'QUARTER_DAY' 값으로 표현(구 allowQuarter 토글 폐기)
            , request.getAllowRemnantRoundUp() // PC-05(D3): 짜투리 잔여 보전 옵션(Y/N)
            , request.getChangeReason()
            , request.getBrkWaiveAllowYn() // BW-04: 휴게 미이용 요청 허용(Y/N). 미전송은 서비스에서 'Y' 정규화
        );

        return new LeavePolicySaveParam(
              command
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }

    /** 서비스 계층 호출용 헬퍼 (가독성 목적). */
    public LeavePolicyCommand toCommand() {
        return command;
    }
}
