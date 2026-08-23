package com.prafta.web.attd.reqinbox.service;

import java.util.List;

import com.prafta.common.cmm.siteauth.result.AccessibleSiteResult;
import com.prafta.web.attd.reqinbox.dto.response.ProcessedReqListResponse;
import com.prafta.web.attd.reqinbox.result.PendingReqResult;
import com.prafta.web.attd.reqinbox.result.PendingSchedReqResult;

/** 요청 승인 관리 통합 대기요청 서비스 (prafta-019 후속). */
public interface ReqInboxService {

    /**
     * 매니저 스코프 내 대기 요청 목록.
     *
     * <p>매니저 전용. 일반 작업자 호출은 차단한다(반려 endpoint 와 동일 게이트).
     * (접수함다중사업장권한확장-002) 접근 가능 사업장 스코프는 {@code SiteAccessService}
     * 로 구한다 — {@code reqSiteCd} 가 있으면 그 값이 접근 가능 목록에 포함되는지 검증(IDOR 가드,
     * 실패 시 {@code COMMON_403_003}) 후 그 1건으로 좁히고, 없으면 접근 가능 목록 전체를 사용한다.
     *
     * @param userCd       JWT 기반 본인 사용자코드(사업장 원장 조회 기준). body 위조 불가.
     * @param authCd       JWT 기반 권한코드(역할 게이트 + 사업장 원장 role fast-path 용). body 위조 불가.
     * @param reqTypeGroup "correction"(근태보정 01/02) | "overtime"(초과 03/04).
     *                     스케줄 수정('10')은 본 메서드 대상이 아니며
     *                     {@link #getPendingSchedRequests} 전용 경로로 처리한다.
     * @param reqSiteCd    프론트가 선택한 사업장(선택값, 빈 값이면 전체 접근가능 사업장).
     */
    List<PendingReqResult> getPendingRequests(String cmpnyCd, String siteCd, String userCd, String authCd,
                                              String reqTypeGroup, String reqSiteCd);

    /**
     * 스케줄 수정('10') 대기 목록.
     *
     * <p>매니저 전용({@link #getPendingRequests} 와 동일 게이트). 반환 컬럼 세트가 달라
     * (현재→요청 스케줄 비교값 동봉) 메서드를 분리한다. 사업장 스코프 해석은
     * {@link #getPendingRequests} 와 동일 규칙.
     *
     * @param userCd    JWT 기반 본인 사용자코드(사업장 원장 조회 기준). body 위조 불가.
     * @param authCd    JWT 기반 권한코드(역할 게이트용). body 위조 불가.
     * @param reqSiteCd 프론트가 선택한 사업장(선택값, 빈 값이면 전체 접근가능 사업장).
     */
    List<PendingSchedReqResult> getPendingSchedRequests(String cmpnyCd, String siteCd, String userCd,
                                                        String authCd, String reqSiteCd);

    /**
     * 내 처리 이력 — 로그인 관리자가 승인/반려 처리한 요청 목록 (탭별, 최근 300건).
     *
     * <p>매니저 전용({@link #getPendingRequests} 와 동일 게이트). 조회 스코프가
     * "처리자 = 본인"이라 타 관리자 처리분은 나오지 않는다. 연차(leave) 탭의 결재라인 이력
     * (selectProcessedLeaveApprovals)은 SITE_CD 하드필터가 애초에 없어 사업장 스코프와 무관하다
     * (§0.2-3) — 연차 변경 이력(leaveChangeList) 보조 섹션만 사업장 스코프가 적용된다.
     *
     * @param userCd       JWT 기반 본인 사용자코드(처리자 필터 + 사업장 원장 조회 기준). body 위조 불가.
     * @param authCd       JWT 기반 권한코드(역할 게이트용).
     * @param reqTypeGroup "correction" | "overtime" | "schedule" | "leave"
     * @param reqSiteCd    프론트가 선택한 사업장(선택값, 빈 값이면 전체 접근가능 사업장).
     */
    ProcessedReqListResponse getProcessedRequests(String cmpnyCd, String siteCd, String userCd,
                                                  String authCd, String reqTypeGroup, String reqSiteCd);

    /**
     * 관리자가 접근 가능한 사업장 목록 (접수함다중사업장권한확장-002, 프론트 셀렉터용).
     *
     * <p>매니저 전용({@link #getPendingRequests} 와 동일 게이트).
     *
     * @param userCd JWT 기반 본인 사용자코드(사업장 원장 조회 기준). body 위조 불가.
     * @param authCd JWT 기반 권한코드(역할 게이트 + 사업장 원장 role fast-path 용). body 위조 불가.
     */
    List<AccessibleSiteResult> getAccessibleSites(String cmpnyCd, String userCd, String authCd);
}
