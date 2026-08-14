package com.prafta.web.attd.reqinbox.service;

import java.util.List;

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
}
