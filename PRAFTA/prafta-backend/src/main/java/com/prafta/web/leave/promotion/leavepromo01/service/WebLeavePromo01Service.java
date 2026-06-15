package com.prafta.web.leave.promotion.leavepromo01.service;

import com.prafta.common.cmm.leave.promotion.autobatch.BatchProposal;
import com.prafta.common.dto.TokenInfo;
import com.prafta.web.leave.promotion.leavepromo01.application.param.PromotionDesignateParam;
import com.prafta.web.leave.promotion.leavepromo01.application.param.PromotionTargetSearchParam;
import com.prafta.web.leave.promotion.leavepromo01.dto.request.AutoBatchCommitRequest;
import com.prafta.web.leave.promotion.leavepromo01.dto.request.AutoBatchPreviewRequest;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.AutoBatchCommitResponse;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.PromotionDesignateResultResponse;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.PromotionExcelUploadResponse;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.PromotionTargetListResponse;

import org.springframework.web.multipart.MultipartFile;

/**
 * prafta-com-008-A-4: 2차 회사직권 연차 사용촉진 웹 서비스.
 *
 * <p>노드 권한(prafta-042 canManageNode/canManageUser) + cross-site IDOR 를 서버에서 강제한다.
 * 촉진 연차 등록은 공용 헬퍼({@code LeavePromotionRegistrationService}, stage=SECOND/COMPANY)를 재사용하고,
 * 지정 완료 시 근로자 PUSH(SYS045 LEAVE_PROMOTION_DESIGNATED)를 outbox 에 afterCommit 적재한다.
 */
public interface WebLeavePromo01Service {

    /** 조회조건(사업장/부서+하위/사용자명/1년차)별 2차 대상자 + 미사용 연차수. 노드 권한 게이트. */
    PromotionTargetListResponse getDesignateTargets(PromotionTargetSearchParam param);

    /** 특정 사용자에 날짜 다건 직권지정(2차/회사직권). 대상자 권한 재검증 + 마스터 갱신 + PUSH. */
    PromotionDesignateResultResponse designate(PromotionDesignateParam param);

    /**
     * prafta-com-008-A-5: 자동배치 프리뷰(2전략, 등록 없음). 노드 권한 게이트 + 순수 계산 위임.
     */
    BatchProposal previewAutoBatch(AutoBatchPreviewRequest request, TokenInfo tokenInfo);

    /**
     * prafta-com-008-A-5: 자동배치 커밋. 관리자 확인본 proposal 의 각 (user,date)를 공용 등록 헬퍼
     * (SECOND/COMPANY)로 등록(DIRECT_USE_KEY 멱등 = TOCTOU 방어) + 사용자별 권한 재검증 + 마스터/PUSH.
     */
    AutoBatchCommitResponse commitAutoBatch(AutoBatchCommitRequest request, TokenInfo tokenInfo);

    /**
     * prafta-com-008-A-6: 조회조건 기준 일괄지정 엑셀 양식(.xlsx) 바이트. 노드 권한 게이트.
     */
    byte[] buildExcelTemplate(PromotionTargetSearchParam param);

    /**
     * prafta-com-008-A-6: 엑셀 업로드(행=사용자-연차날짜) → register(SECOND/COMPANY) 일괄. 실패행은
     * AES-GCM 보관 후 failsToken 발급(2시트 다운로드용). 노드 권한·IDOR 서버 강제.
     */
    PromotionExcelUploadResponse uploadExcel(MultipartFile file, TokenInfo tokenInfo);

    /**
     * prafta-com-008-A-6: 실패행 2시트(.xlsx) 다운로드. failsToken 소유자(cmpny+user) 재검증 후 복호화.
     * 토큰 무효/만료/도용이면 null(컨트롤러가 404).
     */
    byte[] downloadFails(String token, TokenInfo tokenInfo);
}
