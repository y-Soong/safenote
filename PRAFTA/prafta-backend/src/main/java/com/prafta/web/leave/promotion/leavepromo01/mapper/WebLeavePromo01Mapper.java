package com.prafta.web.leave.promotion.leavepromo01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.leave.promotion.leavepromo01.application.query.PromotionFirstTargetSearchQuery;
import com.prafta.web.leave.promotion.leavepromo01.application.query.PromotionTargetSearchQuery;
import com.prafta.web.leave.promotion.leavepromo01.result.PromotionFirstTargetRowResult;
import com.prafta.web.leave.promotion.leavepromo01.result.PromotionTargetRowResult;
import com.prafta.web.leave.promotion.leavepromo01.vo.DesignateTargetMetaVO;
import com.prafta.web.leave.promotion.leavepromo01.vo.FirstRoundMetaVO;

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

    // ============================================================
    // 1차 현황(작업지시서_연차촉진-1차현황-화면-및-배치활성화 §5) — 조회 / 독촉
    // ============================================================

    /**
     * 1차 통지 대상자 현황 raw 목록(스코프 내 FIRST 마스터 보유자).
     *
     * <p>대상 = AXIS7='Y' + 스코프(사업장 + 노드 cascade) 활성 사용자 + FIRST 마스터(DEL_YN='N',
     * STATUS 무필터) 보유자. 사용자명 LIKE 적용. 1년차 필터는 없다(1차 대상은 구조적으로 1년차 이상).
     *
     * <p>제출 여부 판정용 촉진 연차 최초 등록일(SUB)·독촉 이력(RM)은 <b>집계 LEFT JOIN 1본씩</b>으로
     * 싣는다(행별 스칼라 서브쿼리 = N+1 금지). 회차 스코프(이전 회차분 배제)는 서비스가 통지일 기준
     * 시간 비교로 적용한다.
     *
     * <p>⚠️ resultType 이 record 이므로 SELECT 컬럼 순서 = 생성자 인자 순서다.
     */
    List<PromotionFirstTargetRowResult> selectFirstTargets(PromotionFirstTargetSearchQuery query);

    /**
     * 독촉 대상자의 FIRST 회차 메타(사업장/부서/기준 만료일/통지일/지정 일수). 미해당이면 null.
     *
     * <p>{@code selectDesignateTargetMeta} 를 PROMO_STAGE='FIRST' 로 미러한다. siteCd 는 세션 고정
     * 사업장 스코프(대상자가 그 사업장 소속이어야 함) — 클라 불신뢰. FIRST 마스터가 여러 회차면
     * 가장 임박한 만료일 1건(ORDER BY BASE_AVAIL_TO_DATE ASC, PROMO_ID ASC LIMIT 1).
     */
    FirstRoundMetaVO selectFirstRoundMeta(@Param("cmpnyCd") String cmpnyCd,
                                          @Param("siteCd") String siteCd,
                                          @Param("userCd") String userCd);

    /**
     * 오늘 같은 대상에게 이미 독촉을 발송했는지 카운트(DEDUP_KEY 정확 일치). 1 이상이면 스킵.
     *
     * <p>UX 용 사전 점검이며 <b>최종 권위는 UNIQUE(CMPNY_CD, DEDUP_KEY)</b> 다(동시 클릭 TOCTOU 방어는
     * INSERT 시 DuplicateKeyException 흡수로 처리). LIKE 금지 — 정확 일치만.
     */
    int countRemindToday(@Param("cmpnyCd") String cmpnyCd,
                         @Param("dedupKey") String dedupKey);
}
