package com.prafta.web.attd.reqinbox.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.reqinbox.result.PendingReqResult;
import com.prafta.web.attd.reqinbox.result.PendingSchedReqResult;
import com.prafta.web.attd.reqinbox.result.ProcessedLeaveChangeResult;
import com.prafta.web.attd.reqinbox.result.ProcessedReqResult;

/**
 * 요청 승인 관리 통합 대기요청 조회 Mapper (prafta-019 후속).
 */
@Mapper
public interface ReqInboxMapper {

    /**
     * 매니저 스코프(회사+사업장) 내 대기('01') 요청 목록.
     *
     * @param reqTypes 조회 대상 REQ_TYPE 목록 (근태보정 01/02, 초과 03)
     */
    List<PendingReqResult> selectPendingRequests(@Param("cmpnyCd") String cmpnyCd,
                                                 @Param("siteCd") String siteCd,
                                                 @Param("reqTypes") List<String> reqTypes);

    /**
     * 스케줄 수정('10') 대기 목록 + 현재→요청 스케줄 비교값.
     *
     * <p>기존 {@link #selectPendingRequests} 와 컬럼 세트가 달라 statement 를 분리한다
     * (기존 3탭 무회귀 — plan 결정 B). 스코프는 동일하게 CMPNY_CD(토큰) + SITE_CD(토큰).
     *
     * @param reqType 조회 대상 REQ_TYPE (AttdReqTypeUtils.REQ_TYPE_SCHED_MODIFY)
     */
    List<PendingSchedReqResult> selectPendingSchedRequests(@Param("cmpnyCd") String cmpnyCd,
                                                           @Param("siteCd") String siteCd,
                                                           @Param("reqType") String reqType);

    /**
     * 내가 승인('02')/반려('03') 처리한 요청 이력 (근태보정/초과/스케줄 탭 — PROCESS_USER_CD 기준).
     *
     * @param reqTypes 조회 대상 REQ_TYPE 목록 (근태보정 01/02, 초과 03/04, 스케줄 10)
     */
    List<ProcessedReqResult> selectProcessedRequests(@Param("cmpnyCd") String cmpnyCd,
                                                     @Param("siteCd") String siteCd,
                                                     @Param("processUserCd") String processUserCd,
                                                     @Param("reqTypes") List<String> reqTypes);

    /**
     * 내가 결재라인에서 승인('02')/반려('03') 처리한 연차 요청 이력 (연차 탭 — APPROVER_USER_CD 기준).
     */
    List<ProcessedReqResult> selectProcessedLeaveApprovals(@Param("cmpnyCd") String cmpnyCd,
                                                           @Param("approverUserCd") String approverUserCd);

    /**
     * 내가 확인(CONFIRMED)/반려(REJECTED)한 연차 변경(이동/삭제) 요청 이력 (연차 탭 보조 섹션).
     */
    List<ProcessedLeaveChangeResult> selectProcessedLeaveChangeRequests(@Param("cmpnyCd") String cmpnyCd,
                                                                        @Param("siteCd") String siteCd,
                                                                        @Param("confirmUserCd") String confirmUserCd);
}
