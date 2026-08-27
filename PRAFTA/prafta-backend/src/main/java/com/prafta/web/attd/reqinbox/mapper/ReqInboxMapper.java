package com.prafta.web.attd.reqinbox.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.reqinbox.result.PendingReqResult;
import com.prafta.web.attd.reqinbox.result.PendingSchedReqResult;
import com.prafta.web.attd.reqinbox.result.ProcessedLeaveChangeResult;
import com.prafta.web.attd.reqinbox.result.ProcessedReqResult;
import com.prafta.web.attd.reqinbox.result.ReqSummaryResult;

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
     * 기본 근무타입 변경('14') 대기 목록 + 현재→요청 근무타입 비교값(PRAFTA-002 기본근무타입-승인제).
     *
     * <p>{@link #selectPendingSchedRequests} 와 컬럼 세트는 같으나 "현재(cur*)" 조인 축이 다르다
     * (근무일 축이 아닌 사용자축 TB_USER.DEFAULT_SCH_CD) — statement 를 분리한다(기존 3탭 무회귀).
     *
     * @param reqType 조회 대상 REQ_TYPE (AttdReqTypeUtils.REQ_TYPE_DEFAULT_SCH_CHANGE)
     * @param asOfYmd 유효버전 판정 기준일(YYYYMMDD, 서비스 계층이 산출한 "명일") — R.WORK_YMD 가
     *                NULL 이라 이 파라미터로 대체한다(요청/현재 스케줄 양쪽 동일 기준일 사용)
     */
    List<PendingSchedReqResult> selectPendingDefaultSchChangeRequests(@Param("cmpnyCd") String cmpnyCd,
                                                                      @Param("siteCds") List<String> siteCds,
                                                                      @Param("reqType") String reqType,
                                                                      @Param("asOfYmd") String asOfYmd);

    /**
     * 내가 결재라인에서 승인('02')/반려('03') 처리한 요청 이력
     * (근태보정/초과/스케줄 탭 — TB_USER_ATTD_REQ_APPROVAL.APPROVER_USER_CD 기준, 근태결재선통합 P2-2).
     *
     * <p>종전 REQ.PROCESS_USER_CD 기준은 다단계 결재의 중간 단계 승인자 처리 이력이 누락되는
     * 결함이 있어 {@link #selectProcessedLeaveApprovals}(연차 탭)와 동형 패턴으로 전환했다.
     *
     * @param siteCds  조회 대상 SITE_CD 목록(접수함다중사업장권한확장-001 원장 기반 접근가능 목록)
     * @param reqTypes 조회 대상 REQ_TYPE 목록 (근태보정 01/02, 초과 03/04, 스케줄 10)
     */
    List<ProcessedReqResult> selectProcessedRequests(@Param("cmpnyCd") String cmpnyCd,
                                                     @Param("siteCds") List<String> siteCds,
                                                     @Param("callerUserCd") String callerUserCd,
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

    /**
     * 요청 1건 요약(REQ_ID/SITE_CD/REQ_TYPE/USER_CD) — {@code GET /reqinbox/approval-line}(근태결재선통합
     * P3-1)의 IDOR 검증(소속 사업장 접근 가능 여부 / 연차 요청자 본인 여부) 용도. 없으면 null(호출부가
     * ATTD_404_001 처리).
     */
    ReqSummaryResult selectReqSummaryByReqId(@Param("cmpnyCd") String cmpnyCd, @Param("reqId") String reqId);
}
