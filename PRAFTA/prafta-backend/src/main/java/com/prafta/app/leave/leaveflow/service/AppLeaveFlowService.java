package com.prafta.app.leave.leaveflow.service;

import com.prafta.app.leave.leaveflow.application.param.LeaveApplyMetaParam;
import com.prafta.app.leave.leaveflow.application.param.LeaveApplyParam;
import com.prafta.app.leave.leaveflow.application.param.LeaveApproverSearchParam;
import com.prafta.app.leave.leaveflow.application.param.LeaveDayScheduleParam;
import com.prafta.app.leave.leaveflow.application.param.LeaveDeductionPreviewParam;
import com.prafta.app.leave.leaveflow.dto.response.ApprovalPresetListResponse;
import com.prafta.app.leave.leaveflow.dto.response.ApproverSearchResponse;
import com.prafta.app.leave.leaveflow.dto.response.LeaveApplyMetaResponse;
import com.prafta.app.leave.leaveflow.dto.response.LeaveDayScheduleResponse;
import com.prafta.app.leave.leaveflow.dto.response.LeaveDeductionPreviewResponse;
import com.prafta.common.cmm.file.application.model.FileBytesResult;
import com.prafta.common.dto.TokenInfo;

import org.springframework.web.multipart.MultipartFile;

/**
 * prafta-app-018-A/B: 앱 연차 신청 폼 메타 조회 + 신청 쓰기 서비스.
 */
public interface AppLeaveFlowService {

    /** 신청 가능 연차종류 + 허용 사용단위(D2-a 계층) + 잔여 조회. */
    LeaveApplyMetaResponse selectApplyMeta(LeaveApplyMetaParam param);

    /** 본인 소유 결재선 프리셋 목록(mypage01 재사용). */
    ApprovalPresetListResponse selectApprovalPresets(LeaveApplyMetaParam param);

    /** 결재자 후보 검색(사업장 스코프, LIMIT/OFFSET, hasNext). */
    ApproverSearchResponse searchApprovers(LeaveApproverSearchParam param);

    /**
     * prafta-app-018-B: 연차 신청 1건 처리(요청 INSERT + 결재선 + 사용기록 + 부여 재계산).
     * 단위 게이팅(D2) + 구조검증/차감 + 사후마감 + 잔여검증을 모두 통과한 후에만 INSERT 한다.
     */
    void submitLeave(LeaveApplyParam param);

    /**
     * LC-07(T3): 예상 차감액 미리보기 — INSERT 없음(조회 전용, 웹 previewDeduction 미러).
     *
     * <p>검증 가드(출근기록/단위 게이팅/구조·스케줄·휴게/사후마감/1.0 점유/시간대 겹침)는
     * {@link #submitLeave} 와 동일하게 태워 "신청하면 거부될 값"을 미리 보여주지 않는다.
     * 잔여 부족은 에러가 아니라 플래그({@code insufficientBalance})로 응답한다.
     */
    LeaveDeductionPreviewResponse previewDeduction(LeaveDeductionPreviewParam param);

    /**
     * 신청 대상일의 근무/휴게 시각 조회(조회 전용) — 시간차 연차의 휴게 가로지름(ATTD_400_055)
     * 사전 안내용. 스케줄 없는 날은 {@code hasSchedule=false} 로 응답한다(에러 아님).
     */
    LeaveDayScheduleResponse selectDaySchedule(LeaveDayScheduleParam param);

    /**
     * 연차 신청 증빙 필수화(2026-08-29): 증빙 파일 업로드(업로드/제출 분리 아키텍처 — 신청 상태와 무관한
     * 임시 업로드). 저장된 {@code fileMgmtCd} 를 반환하며, 이후 {@code /apply}·{@code /apply-multi} 요청
     * 바디의 {@code evidenceFileId} 로 실어 보낸다.
     */
    String uploadEvidenceFile(TokenInfo tokenInfo, MultipartFile file);

    /**
     * 연차 신청 증빙 필수화(2026-08-29): 증빙 파일 열람. 본인(업로드 신청자) 또는 해당 요청 결재선에
     * 포함된 결재자만 접근 가능(IDOR 차단). 공개 정적 URL 금지 — 인증 스트림 서빙(SEC-1 원칙 재사용).
     */
    FileBytesResult loadEvidenceFile(TokenInfo tokenInfo, String fileMgmtCd);
}
