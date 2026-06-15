package com.prafta.web.attd.attd07.service;

import com.prafta.web.attd.attd07.application.param.AttdCloseParam;
import com.prafta.web.attd.attd07.application.param.AttdCloseStatusParam;
import com.prafta.web.attd.attd07.dto.response.AttdCloseStatusResponse;

/**
 * 근태 마감 서비스 (prafta-019-C).
 *
 * <p>정책서: attd/§13(근태 마감), 재기획서 §3.3(자동마감 금지, 강제마감 미도입)·§9.4.
 * 마감 단위 = 회사+사업장+마감월(YYYYMM).
 */
public interface AttdCloseService {

    /** 마감 상태 + 차단 사유 현황(§13.3) + 이력 조회. */
    AttdCloseStatusResponse getCloseStatus(AttdCloseStatusParam param);

    /** 근태 마감 실행. 차단 사유가 0건일 때만 가능(자동/강제 마감 금지). 매니저 권한 필요. */
    void closeAttendance(AttdCloseParam param);

    /** 근태 마감 해제. 매니저 권한 필요. */
    void uncloseAttendance(AttdCloseParam param);

    /**
     * 사용자 소속부서(nodeCd) 기준으로 해당 월이 마감 커버리지에 포함되는지 (PRAFTA-028).
     *
     * <p>덮임 조건: 같은 월에 CLOSED 인 마감행 중 전체('*') / 해당 부서 / (INC_SUB_YN='Y' 인) 상위 부서가
     * 하나라도 존재. 근태/근무계획/요청 등 쓰기 가드에서 사용한다.
     */
    boolean isClosedForNode(String cmpnyCd, String siteCd, String nodeCd, String closeYm);

    /**
     * 사용자(userCd)의 소속부서 기준으로 해당 월이 마감 커버리지에 포함되는지 (PRAFTA-028).
     *
     * <p>부서코드를 직접 알기 어려운 호출부(근무계획·근태삭제 등)를 위해 내부에서 사용자 부서를 해석한다.
     */
    boolean isClosedForUser(String cmpnyCd, String siteCd, String userCd, String closeYm);

    /**
     * 노드(부서) 관리 권한 여부 (PRAFTA-028) — master/hr 또는 해당/상위 부서의 정·부 관리자.
     *
     * <p>근태 마감뿐 아니라 근태/초과근무 승인·반려·직접수정 등 관리자 액션의 권한 게이트에 공통 사용한다.
     */
    boolean canManageNode(String authCd, String userCd, String cmpnyCd, String siteCd, String nodeCd);

    /**
     * 대상 사용자(targetUserCd) 관리 권한 여부 (PRAFTA-041-4) — master/hr 또는 대상 사용자가 소속한
     * 부서(및 상위 부서)의 정·부 관리자.
     *
     * <p>근무계획(attd05) 등 "특정 사용자 단위" 쓰기 권한 게이트에 사용한다. 내부에서 대상 사용자의
     * 소속 부서(NODE_CD)를 서버 조회하여 {@link #canManageNode} 로 위임한다(클라이언트 nodeCd 불신뢰).
     * master/hr 은 부서 조회 없이 즉시 true.
     */
    boolean canManageUser(String authCd, String requesterUserCd, String cmpnyCd, String siteCd, String targetUserCd);

    /**
     * 노드(부서) 관리 권한 여부 — safe 제외 변형 (PRAFTA-COM-008-C 작업1).
     *
     * <p>{@link #canManageNode}와 동일하되, 전사 통과 역할에서 <b>safe(안전관리자)를 제외</b>한다
     * (master/hr 만 전사). 그 외 노드 정·부 관리자 cascade 판정은 동일하다(역할 무관·관리자 지정 기반).
     * 연차 변경 동의 관리(attd13)는 safe 권한을 부여하지 않는다는 확정 결정에 사용한다.
     */
    boolean canManageNodeExcludeSafe(String authCd, String userCd, String cmpnyCd, String siteCd, String nodeCd);

    /**
     * 대상 사용자 관리 권한 여부 — safe 제외 변형 (PRAFTA-COM-008-C 작업1).
     *
     * <p>{@link #canManageUser}와 동일하되, 전사 통과 역할에서 <b>safe 를 제외</b>한다(master/hr 만 전사).
     * 대상 사용자 소속 부서를 서버 조회 후 {@link #canManageNodeExcludeSafe}로 위임한다.
     */
    boolean canManageUserExcludeSafe(String authCd, String requesterUserCd, String cmpnyCd, String siteCd, String targetUserCd);
}
