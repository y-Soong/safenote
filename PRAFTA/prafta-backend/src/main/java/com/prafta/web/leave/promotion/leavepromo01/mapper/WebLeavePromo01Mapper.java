package com.prafta.web.leave.promotion.leavepromo01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.leave.promotion.leavepromo01.application.query.PromotionTargetSearchQuery;
import com.prafta.web.leave.promotion.leavepromo01.result.PromotionTargetRowResult;
import com.prafta.web.leave.promotion.leavepromo01.vo.DesignateTargetMetaVO;

/**
 * prafta-com-008-A-4: 2차 회사직권 대상자 조회/메타 전용 Mapper(웹).
 *
 * <p>모든 조회는 CMPNY_CD + SITE_CD(세션 고정 검증 통과값) 스코프로 격리한다. 노드 cascade 는
 * Attd11/Attd12 RECURSIVE node_tree 패턴 재사용. SQL 규칙: leading comma, {@code #{...}}, SELECT * 금지.
 */
@Mapper
public interface WebLeavePromo01Mapper {

    /**
     * 2차 회사직권 대상자 목록(스코프 내 SECOND 촉진 도래자 + 미사용 연차수).
     *
     * <p>대상 = AXIS7='Y' + 스코프(사업장+노드 cascade) 활성 사용자 + SECOND 마스터
     * (STATUS IN ('NOTICED','DESIGNATED')) 보유 + 미사용 연차(본연차+근속가산 ACTIVE 잔여)&gt;0.
     * 사용자명 LIKE / 1년차(HIRE_DATE) 필터 적용.
     */
    List<PromotionTargetRowResult> selectDesignateTargets(PromotionTargetSearchQuery query);

    /**
     * 직권지정 대상 사용자의 메타(사업장/부서/입사일/기준 본연차 만료일). 스코프 밖/미해당이면 null.
     *
     * <p>siteCd/nodeCd 는 클라 불신뢰 — 서버 재조회하여 권한(canManageUser)·등록 스코프에 사용한다.
     * baseAvailToDate = SECOND 마스터의 BASE_AVAIL_TO_DATE(회차 키 산출). SECOND 마스터가 없으면
     * (=2차 미도래) baseAvailToDate 가 null → 서비스가 대상 아님으로 거부.
     */
    DesignateTargetMetaVO selectDesignateTargetMeta(@Param("cmpnyCd") String cmpnyCd,
                                                    @Param("siteCd") String siteCd,
                                                    @Param("userCd") String userCd);
}
