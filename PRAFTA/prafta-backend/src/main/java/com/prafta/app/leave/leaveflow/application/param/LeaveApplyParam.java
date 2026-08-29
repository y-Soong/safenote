package com.prafta.app.leave.leaveflow.application.param;

import java.util.List;

import com.prafta.app.leave.leaveflow.dto.request.LeaveApplyRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-018-B: 앱 연차 신청 Param.
 *
 * <p>웹 {@code com.prafta.web.attd.leaveflow.application.param.LeaveApplyParam} 미러.
 * 신청자(cmpny/site/user)는 토큰에서만 강제한다(요청 body 식별값 신뢰 금지 — IDOR 차단).
 *   nodeCd 는 본문으로 받지 않으며, 결재선 자기승인 판정은 서버 USER→NODE 조인으로 독립 수행하므로 불필요하다.
 *   approverUserCds/presetId 만 본문 신뢰(소유권은 서버 검증).
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
    , List<String> approverUserCds
    , String presetId
    , boolean isBorrow
    , String gvCmpnyCd
    , String gvSiteCd
    , String gvUserCd
    /**
     * 연차 기간(From-To) 신청 묶음 ID (prafta-leavemulti). 같은 신청에서 분해된 날짜별 REQ 가 동일 값.
     * <p><b>단일일 신청은 항상 null</b> 이며, null 이면 알림 dedupKey 가 기존(REQ 단위)으로 동작한다
     * — 단일일 경로 무회귀의 근거다. 생성 경로가 {@link #from} 하나뿐이라 여기서 null 로 고정된다.
     */
    , String groupId
    /**
     * 연차 신청 증빙 필수화(2026-08-29): 증빙 파일 업로드 응답의 fileMgmtCd. nullable(증빙 불필요 타입은 null).
     * 서버 강제 검증은 {@code AppLeaveFlowServiceImpl.submitLeave} 가 EVIDENCE_YN='Y' 타입에 한해 수행한다.
     */
    , String evidenceFileId
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
            , request.getApproverUserCds()
            , request.getPresetId()
            , Boolean.TRUE.equals(request.getIsBorrow()) // 기본 false(미전송/null → false)
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_userCd()
            , null   // groupId — 단일일 신청은 묶음이 아니다(무회귀: 기존 동작 완전 동일)
            , request.getEvidenceFileId()
        );
    }

    /**
     * 기간(From-To) 신청 분해용 — 이 param 을 기준으로 "특정 날짜 1건" 파생 param 을 만든다.
     *
     * <p>기간신청은 종일({@code useUnitType='00'})만 대상이므로 반차/시간차 관련 필드
     * (halfPart/startTime/endTime)는 원본값을 그대로 승계한다(호출부가 null 로 넘긴다).
     * 가불은 1차 범위에서 제외이므로 {@code isBorrow=false} 로 고정한다 — 가불 분기 미진입.
     *
     * @param targetYmd 파생 대상 근무일 (YYYYMMDD)
     * @param groupId   묶음 ID (null 이면 단일일과 동일 동작)
     */
    public LeaveApplyParam deriveForDate(String targetYmd, String groupId) {
        return new LeaveApplyParam(
              leaveCd
            , leaveType
            , targetYmd
            , useUnitType
            , halfPart
            , startTime
            , endTime
            , reason
            , approverUserCds
            , presetId
            , false          // 가불 미사용 고정(1차 범위 — 다일에서 가불 분기 진입 금지)
            , gvCmpnyCd
            , gvSiteCd
            , gvUserCd
            , groupId
            , evidenceFileId // 기간신청은 날짜별로 분해되지만 증빙 파일은 신청 1건 대표값 그대로 승계
        );
    }
}
