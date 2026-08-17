package com.prafta.web.attd.reqinbox.service;

import java.util.List;

import com.prafta.web.attd.reqinbox.dto.response.ProcessedReqListResponse;
import com.prafta.web.attd.reqinbox.result.PendingReqResult;
import com.prafta.web.attd.reqinbox.result.PendingSchedReqResult;

/** 요청 승인 관리 통합 대기요청 서비스 (prafta-019 후속). */
public interface ReqInboxService {

    /**
     * 매니저 스코프 내 대기 요청 목록.
     *
     * <p>매니저 전용. 일반 작업자 호출은 차단한다(반려 endpoint 와 동일 게이트).
     *
     * @param authCd       JWT 기반 권한코드(역할 게이트용). body 위조 불가.
     * @param reqTypeGroup "correction"(근태보정 01/02) | "overtime"(초과 03/04).
     *                     스케줄 수정('10')은 본 메서드 대상이 아니며
     *                     {@link #getPendingSchedRequests} 전용 경로로 처리한다.
     */
    List<PendingReqResult> getPendingRequests(String cmpnyCd, String siteCd, String authCd, String reqTypeGroup);

    /**
     * 스케줄 수정('10') 대기 목록.
     *
     * <p>매니저 전용({@link #getPendingRequests} 와 동일 게이트). 반환 컬럼 세트가 달라
     * (현재→요청 스케줄 비교값 동봉) 메서드를 분리한다.
     *
     * @param authCd JWT 기반 권한코드(역할 게이트용). body 위조 불가.
     */
    List<PendingSchedReqResult> getPendingSchedRequests(String cmpnyCd, String siteCd, String authCd);

    /**
     * 내 처리 이력 — 로그인 관리자가 승인/반려 처리한 요청 목록 (탭별, 최근 300건).
     *
     * <p>매니저 전용({@link #getPendingRequests} 와 동일 게이트). 조회 스코프가
     * "처리자 = 본인"이라 타 관리자 처리분은 나오지 않는다.
     *
     * @param userCd       JWT 기반 본인 사용자코드(처리자 필터). body 위조 불가.
     * @param authCd       JWT 기반 권한코드(역할 게이트용).
     * @param reqTypeGroup "correction" | "overtime" | "schedule" | "leave"
     */
    ProcessedReqListResponse getProcessedRequests(String cmpnyCd, String siteCd, String userCd,
                                                  String authCd, String reqTypeGroup);
}
