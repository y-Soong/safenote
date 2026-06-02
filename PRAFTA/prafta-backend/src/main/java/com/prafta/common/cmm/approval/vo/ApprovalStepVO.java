package com.prafta.common.cmm.approval.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 연차 요청 결재라인 단계 1행 (tb_user_attd_req_approval).
 *
 * <p>prafta-019-D. 사용자가 신청 시 직접 구성한 결재자 순서(스텝)를 담는다.
 * 저장 구조는 D가 제공하고, 라인 생성·진행(승인/반려 전환)은 작업 E가 수행한다.
 */
@Getter
@Setter
public class ApprovalStepVO {

    /** 연관 요청 (tb_user_attd_req.REQ_ID) */
    private String reqId;

    /** 결재 단계 (1부터) */
    private Integer approvalStep;

    /** 회사 코드 */
    private String cmpnyCd;

    /** 지정 결재자 사용자 코드 */
    private String approverUserCd;

    /** 결재자명 (조회 시 채움) */
    private String approverUserNm;

    /** 단계 상태 [SYS044] 00대기/01신청/02승인/03반려 */
    private String approvalStatus;

    /** 단계 상태명 (조회 시 채움) */
    private String approvalStatusNm;

    /** 결재 코멘트 */
    private String approvalComment;

    /** 처리 일시 (ISO 8601 문자열) */
    private String approvalDate;

    /** 입력/수정자 */
    private String insertNo;
    private String updateNo;
}
