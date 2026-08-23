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
     * 매니저 스코프(회사+접근가능 사업장) 내 대기('01') 요청 목록.
     *
     * @param siteCds  조회 대상 SITE_CD 목록(접수함다중사업장권한확장-001 원장 기반 접근가능 목록,
     *                 또는 프론트가 특정 사업장을 선택했으면 그 1건으로 좁힌 목록)
     * @param reqTypes 조회 대상 REQ_TYPE 목록 (근태보정 01/02, 초과 03)
     */
    List<PendingReqResult> selectPendingRequests(@Param("cmpnyCd") String cmpnyCd,
                                                 @Param("siteCds") List<String> siteCds,
                                                 @Param("reqTypes") List<String> reqTypes);

    /**
     * 스케줄 수정('10') 대기 목록 + 현재→요청 스케줄 비교값.
     *
     * <p>기존 {@link #selectPendingRequests} 와 컬럼 세트가 달라 statement 를 분리한다
     * (기존 3탭 무회귀 — plan 결정 B). 스코프는 CMPNY_CD(토큰) + SITE_CD IN(접근가능 목록)
     * (접수함다중사업장권한확장-002).
     *
     * @param reqType 조회 대상 REQ_TYPE (AttdReqTypeUtils.REQ_TYPE_SCHED_MODIFY)
     */
    List<PendingSchedReqResult> selectPendingSchedRequests(@Param("cmpnyCd") String cmpnyCd,
                                                           @Param("siteCds") List<String> siteCds,
                                                           @Param("reqType") String reqType);

    /**
     * 내가 승인('02')/반려('03') 처리한 요청 이력 (근태보정/초과/스케줄 탭 — PROCESS_USER_CD 기준).
     *
     * @param siteCds  조회 대상 SITE_CD 목록(접수함다중사업장권한확장-001 원장 기반 접근가능 목록)
     * @param reqTypes 조회 대상 REQ_TYPE 목록 (근태보정 01/02, 초과 03/04, 스케줄 10)
     */
    List<ProcessedReqResult> selectProcessedRequests(@Param("cmpnyCd") String cmpnyCd,
                                                     @Param("siteCds") List<String> siteCds,
                                                     @Param("processUserCd") String processUserCd,
                                                     @Param("reqTypes") List<String> reqTypes);

    /**
     * 내가 결재라인에서 승인('02')/반려('03') 처리한 연차 요청 이력 (연차 탭 — APPROVER_USER_CD 기준).
     *
     * <p>결재라인 기반 조회라 SITE_CD 하드필터가 애초에 없다(§0.2-3) — 무수정, siteCds 파라미터 없음.
     */
    List<ProcessedReqResult> selectProcessedLeaveApprovals(@Param("cmpnyCd") String cmpnyCd,
                                                           @Param("approverUserCd") String approverUserCd);

    /**
     * 내가 확인(CONFIRMED)/반려(REJECTED)한 연차 변경(이동/삭제) 요청 이력 (연차 탭 보조 섹션).
     *
     * @param siteCds 조회 대상 SITE_CD 목록(접수함다중사업장권한확장-001 원장 기반 접근가능 목록)
     */
    List<ProcessedLeaveChangeResult> selectProcessedLeaveChangeRequests(@Param("cmpnyCd") String cmpnyCd,
                                                                        @Param("siteCds") List<String> siteCds,
                                                                        @Param("confirmUserCd") String confirmUserCd);
}
