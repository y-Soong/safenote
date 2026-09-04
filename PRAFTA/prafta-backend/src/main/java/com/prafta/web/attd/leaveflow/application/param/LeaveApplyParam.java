package com.prafta.web.attd.leaveflow.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.leaveflow.dto.request.LeaveApplyRequest;

/**
 * 연차 신청 Param (prafta-019-E). 신청자(userCd)·회사·사업장은 토큰에서 강제(요청 body 신뢰 금지).
 */
public record LeaveApplyParam(
      String leaveCd
    , String leaveType
    , String workYmd
    , String useUnitType
    /** 반차(01) 파트: START(시작기준=늦게 출근) / END(종료기준=일찍 퇴근). 반차 외 단위에서는 무시(HB-02). */
    , String halfPart
    , String startTime
    , String endTime
    , String reason
    , String nodeCd
    , List<String> approverUserCds
    , boolean isBorrow
    , String gvCmpnyCd
    , String gvSiteCd
    , String gvUserCd
    /**
     * 연차 신청 증빙 필수화(2026-08-29): 증빙 파일 업로드 응답의 fileMgmtCd. nullable(증빙 불필요 타입은 null).
     * 서버 강제 검증(4중 — 존재/타입/회사/소유권)은 {@code LeaveFlowServiceImpl.submitLeave} 가 수행한다.
     */
    , String evidenceFileId
    /** 휴게시간 무시 요청(BW-04, 앱 미러). 'Y'/'N' — 미전송(null)은 'N'. */
    , String brkWaiveYn
) {
    public static LeaveApplyParam from(LeaveApplyRequest request, TokenInfo tokenInfo) {
        if (request == null || tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getLeaveCd() == null || request.getLeaveCd().isBlank()
                || request.getWorkYmd() == null || request.getWorkYmd().isBlank()
                || request.getUseUnitType() == null || request.getUseUnitType().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new LeaveApplyParam(
              request.getLeaveCd()
            , request.getLeaveType()
            , request.getWorkYmd()
            , request.getUseUnitType()
            , request.getHalfPart()
            , request.getStartTime()
            , request.getEndTime()
            , request.getReason()
            , request.getNodeCd()
            , request.getApproverUserCds()
            , Boolean.TRUE.equals(request.getIsBorrow()) // 기본 false(미전송/null → false)
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_userCd()
            , request.getEvidenceFileId()
            , "Y".equals(request.getBrkWaiveYn()) ? "Y" : "N" // 미전송/null/'N' → 'N'
        );
    }
}
