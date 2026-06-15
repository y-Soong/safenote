package com.prafta.web.attd.attd13.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd13.dto.request.ChangeRequestCreateRequest;

/**
 * 관리자 연차 변경(이동)/삭제 발의 Param (PRAFTA-COM-008-C).
 *
 * <p>발의자(관리자) 식별은 토큰에서만 도출. 대상 근로자/사업장 정합은 서비스가 LEAVE_ID 재조회로 검증한다.
 */
public record ChangeRequestCreateParam(
      String targetLeaveId
    , String reqType
    , String moveTargetDate
    , String reqReason
    , String gvCmpnyCd
    , String gvAuthCd
    , String gvUserCd
) {
    public static final String REQ_TYPE_MOVE = "MOVE";
    public static final String REQ_TYPE_DELETE = "DELETE";

    public static ChangeRequestCreateParam from(ChangeRequestCreateRequest request, TokenInfo tokenInfo) {
        if (request == null || tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getTARGET_LEAVE_ID() == null || request.getTARGET_LEAVE_ID().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String reqType = request.getREQ_TYPE();
        if (!REQ_TYPE_MOVE.equals(reqType) && !REQ_TYPE_DELETE.equals(reqType)) {
            throw new ApiException(AttdErrorCode.ATTD_400_123);
        }
        // 사유 필수(§3-1)
        if (request.getREQ_REASON() == null || request.getREQ_REASON().isBlank()) {
            throw new ApiException(AttdErrorCode.ATTD_400_120);
        }
        // MOVE 는 이동 대상일 필수
        String moveTargetDate = request.getMOVE_TARGET_DATE();
        if (REQ_TYPE_MOVE.equals(reqType) && (moveTargetDate == null || moveTargetDate.isBlank())) {
            throw new ApiException(AttdErrorCode.ATTD_400_122);
        }
        if (REQ_TYPE_DELETE.equals(reqType)) {
            moveTargetDate = null; // DELETE 시 이동일 무시
        }
        return new ChangeRequestCreateParam(
              request.getTARGET_LEAVE_ID()
            , reqType
            , moveTargetDate
            , request.getREQ_REASON()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_userCd()
        );
    }
}
