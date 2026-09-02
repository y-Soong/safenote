package com.prafta.web.location.location01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.location.location01.result.LocationConsentHistRow;
import com.prafta.web.location.location01.result.LocationConsentStatusRow;
import com.prafta.web.location.location01.result.LocationPurgeHistRow;

/**
 * 위치정보 동의 현황(Location_01) 매퍼 — 위치정보 동의철회·중지 S5.
 *
 * <p>관리자가 <b>"지금 누가 위치정보 동의를 철회/중지한 상태인가"</b> 를 확인하고,
 * 그 사람의 <b>전이 이력</b>과 <b>파기 이력</b>을 열람하는 조회 전용 매퍼다.
 *
 * <p>★인가는 서비스가 강제한다({@code AuthRoleUtils.isManager} + {@code canManageNode}).
 * 매퍼는 서비스가 확정한 스코프({@code scopeNodeCd})만 받아 쓴다.
 *
 * <p>★일용직({@code TB_DAILY_USER})도 포함한다 — 동의 이력 테이블 주석이 두 계통을 모두
 * 대상으로 명시하고 있고, 일용직만 빠지면 "전원 확인" 이 되지 않는다.
 */
@Mapper
public interface Location01Mapper {

    /**
     * 회사 내 사용자별 위치정보 동의 현재 상태 목록.
     *
     * @param termsVersion 현재 시행 중인 005 버전(서비스가 resolve — 클라 값 미신뢰)
     * @param scopeNodeCd  부서 스코프. null 이면 사업장 전체(master/hr)
     * @param stateFilter  {@code 'ISSUE'} = 동의 상태가 아닌 사람만, 그 외/null = 전체
     */
    List<LocationConsentStatusRow> selectConsentStatusList(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("siteCd") String siteCd
            , @Param("termsVersion") String termsVersion
            , @Param("scopeNodeCd") String scopeNodeCd
            , @Param("stateFilter") String stateFilter);

    /** 특정 사용자의 위치정보 동의 전이 이력(최신순). */
    List<LocationConsentHistRow> selectConsentHistList(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("userCd") String userCd);

    /** 특정 사용자의 위치정보 파기 이력(최신순). ★좌표는 애초에 저장되지 않는다. */
    List<LocationPurgeHistRow> selectPurgeHistList(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("userCd") String userCd);

    /**
     * 대상 사용자가 조회 범위 안에 있는지 확인(상세 조회 IDOR 가드).
     *
     * <p>목록과 상세가 다른 EP 라, 상세에서 대상 검증을 하지 않으면 userCd 만 바꿔
     * <b>범위 밖 직원의 철회 이력</b>을 열람할 수 있다.
     *
     * @return 1 이면 범위 안
     */
    int countUserInScope(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("siteCd") String siteCd
            , @Param("userCd") String userCd
            , @Param("scopeNodeCd") String scopeNodeCd);
}
