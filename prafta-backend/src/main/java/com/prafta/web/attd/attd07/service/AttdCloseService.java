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
}
