package com.prafta.web.subcon.subcon03.service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.subcon.subcon03.mapper.Subcon03Mapper;
import com.prafta.web.subcon.subcon03.result.CloseGateResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 근태 마감 게이팅 판정(PRAFTA-SUBCON-T3-03, plan §5-4).
 *
 * <p><b>마감 술어를 새로 정의하지 않는다</b>(정책서 attd/§13 재사용 원칙) — attd07
 * {@link AttdCloseService#isClosedForNode} 를 그대로 호출한다. 신규 마감 SQL 금지.
 *
 * <p>판정 규칙(D2 개정 — qa M3): 월 단위로 아래 순서로 본다.
 * <ol>
 *   <li>전체 센티넬('*')이 마감이면 그 달은 마감 완료(대표행 마감 운영 — 노드 루프 생략).</li>
 *   <li>아니면 전 부서노드가 각각 마감 커버되어야 하고(부서별 마감 운영),
 *       <b>추가로</b> 그 달에 "부서노드 마감으로 덮이지 않는 근태/OT"(NODE_CD NULL·공백·고아)가
 *       0건이어야 한다. 이런 행이 있으면 '*' 마감을 요구한다.</li>
 * </ol>
 *
 * <p>과거 구현은 '*' 를 무조건 필수 원소로 넣어, 부서별로만 마감하는 사업장은 승인이 <b>영구 차단</b>됐다.
 * 반대로 '*' 를 빼면 일용직(부서 개념 없음 — 실제 NODE_CD NULL 근태 존재)이 무마감 상태로 반출된다.
 * 위 규칙은 두 운영 방식을 모두 지원하면서 무부서 근태의 마감 누락도 막는다.
 *
 * <p>데이터 0건 월도 검사 대상이다(0건 = "확정"이 아니다 — 마감 행위가 있어야 확정).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareCloseGateService {

    private final AttdCloseService attdCloseService;
    private final Subcon03Mapper subcon03Mapper;

    /** 사업장 전체 마감 센티넬(attd07 TB_ATTD_CLOSE.NODE_CD 기본값). */
    private static final String WHOLE_SITE_NODE = "*";

    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * 요청 기간의 월별 마감 커버리지를 판정한다.
     *
     * @param prvCmpnyCd   제공측 회사코드
     * @param targetSiteCd 제공측 대상 사업장코드(서버가 체인으로 해석한 값)
     * @param periodStr    기간 시작(YYYYMMDD)
     * @param periodEnd    기간 종료(YYYYMMDD)
     */
    public CloseGateResult evaluate(String prvCmpnyCd, String targetSiteCd, String periodStr, String periodEnd) {

        List<String> nodeCds = subcon03Mapper.selectSiteNodeCdList(prvCmpnyCd, targetSiteCd);

        List<String> unclosedYms = new ArrayList<>();

        for (YearMonth ym : monthsOf(periodStr, periodEnd)) {
            String closeYm = ym.format(YM_FMT);

            if (!isMonthClosed(prvCmpnyCd, targetSiteCd, nodeCds, closeYm)) {
                unclosedYms.add(ym.toString()); // YYYY-MM (화면 안내 표기)
            }
        }

        boolean closedAll = unclosedYms.isEmpty();
        log.info("공유 마감 게이팅 판정 - prvCmpnyCd={}, siteCd={}, 기간={}~{}, 노드 {}개, 마감완료={}, 미마감월={}",
                prvCmpnyCd, targetSiteCd, periodStr, periodEnd, nodeCds.size(), closedAll, unclosedYms);

        return new CloseGateResult(closedAll, unclosedYms);
    }

    /**
     * 한 달의 마감 완료 판정.
     *
     * <p>① '*'(사업장 전체) 마감이면 즉시 완료 — 노드 루프를 건너뛴다(N+1 short-circuit).
     * ② 아니면 전 부서노드 마감 + 무부서 근태(NODE_CD NULL·공백·고아) 0건이어야 완료.
     */
    private boolean isMonthClosed(String cmpnyCd, String siteCd, List<String> nodeCds, String closeYm) {

        // ① 대표행('*') 마감 — 전 노드 + 무부서 근태를 한 번에 덮는다.
        if (attdCloseService.isClosedForNode(cmpnyCd, siteCd, WHOLE_SITE_NODE, closeYm)) {
            return true;
        }

        // ② 부서별 마감 운영 — 전 노드가 자기/상위(INC_SUB) 마감으로 덮여야 한다.
        for (String nodeCd : nodeCds) {
            if (!attdCloseService.isClosedForNode(cmpnyCd, siteCd, nodeCd, closeYm)) {
                return false;
            }
        }

        // ②-2 부서 마감이 닿지 못하는 근태(일용직 등)가 있으면 '*' 마감이 필요하다.
        int uncovered = subcon03Mapper.countNodeUncoveredAttdRows(cmpnyCd, siteCd, closeYm);
        if (uncovered > 0) {
            log.info("마감 게이팅 - 무부서 근태로 전체마감('*') 필요 - siteCd={}, closeYm={}, 미커버 {}건",
                    siteCd, closeYm, uncovered);
            return false;
        }

        return true;
    }

    /**
     * [PS-03] 행 단위 마감 커버리지 판정(D-2) — 마감 커버리지 필터(Subcon03ServiceImpl#computeCoverage)
     * 와 승인 사전정보 예고가 공용으로 쓰는 술어. 신규 마감 SQL 을 만들지 않고 {@link AttdCloseService}
     * 를 그대로 재사용한다(attd §13 단일 출처 원칙).
     *
     * <p>유효 노드(대상 사업장 전체 노드 목록 {@code validNodeCds} 에 존재)면 그 노드의 마감 여부
     * (자기/상위(INC_SUB) 마감 포함 — {@link AttdCloseService#isClosedForNode} 내장 규칙)를 본다.
     * NULL/공백/고아(전체 노드 목록에 없는) 노드는 전체 센티넬('*') 마감 여부를 본다 — 게이트의
     * {@code countNodeUncoveredAttdRows}("고아는 '*' 전용") 의미와 정확히 동치다.
     *
     * <p>(월×유효노드) 판정 결과는 호출자가 넘긴 {@code gateCache} 에 메모이즈한다(행 수천 건 ×
     * 판정 SQL N+1 방지). 캐시는 <b>요청 처리(승인 1회 또는 예고 1회) 스코프의 로컬 Map</b>이어야
     * 한다 — 이 서비스 빈에 필드로 캐시하지 않는다(스레드 안전 — 다른 회사/기간의 판정이 섞이면 안 됨).
     *
     * @param validNodeCds 대상 사업장의 전체 노드 코드 집합({@code Subcon03Mapper#selectSiteNodeList} 결과)
     * @param gateCache    (closeYm|유효노드) → 마감여부 메모이즈 캐시(호출자가 매 계산마다 새로 생성해 전달)
     */
    public boolean isRowCovered(String cmpnyCd, String siteCd, String nodeCd, String closeYm,
            Set<String> validNodeCds, Map<String, Boolean> gateCache) {

        String effectiveNode = (nodeCd == null || nodeCd.isBlank() || !validNodeCds.contains(nodeCd))
                ? WHOLE_SITE_NODE
                : nodeCd;

        String cacheKey = closeYm + "|" + effectiveNode;
        Boolean cached = gateCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        boolean closed = attdCloseService.isClosedForNode(cmpnyCd, siteCd, effectiveNode, closeYm);
        gateCache.put(cacheKey, closed);
        return closed;
    }

    /** 기간이 걸치는 월 집합(YYYYMM 순회 — 서버에서 "오늘"을 만들지 않고 저장된 기간 값만 사용). */
    private List<YearMonth> monthsOf(String periodStr, String periodEnd) {
        YearMonth start = YearMonth.parse(periodStr.substring(0, 6), YM_FMT);
        YearMonth end = YearMonth.parse(periodEnd.substring(0, 6), YM_FMT);

        List<YearMonth> months = new ArrayList<>();
        for (YearMonth cur = start; !cur.isAfter(end); cur = cur.plusMonths(1)) {
            months.add(cur);
        }
        return months;
    }
}
