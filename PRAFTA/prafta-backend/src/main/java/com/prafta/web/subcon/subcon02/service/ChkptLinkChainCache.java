package com.prafta.web.subcon.subcon02.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.prafta.web.subcon.subcon02.mapper.ChkptLinkMapper;
import com.prafta.web.subcon.subcon02.result.LinkDstRaw;

/**
 * 점검 구성 전파용 사업장 링크 체인 캐시(qa M-3).
 *
 * <p>점검연동 ACTIVE 링크(사업장 단위 위상)는 <b>저장 1회 동안 불변</b>이다. 그런데 전파는 행(문항/점검대상)마다
 * 호출되므로, 캐시가 없으면 같은 사업장의 링크 목록을 행 수만큼 재조회한다(N+1).
 * 호출 단위(하나의 저장 트랜잭션)마다 인스턴스를 하나 만들어 재사용한다 — <b>Bean 이 아니다</b>(요청 간 공유 금지).
 */
public final class ChkptLinkChainCache {

    private final ChkptLinkMapper chkptLinkMapper;
    private final Map<String, List<LinkDstRaw>> linksBySite = new HashMap<>();

    ChkptLinkChainCache(ChkptLinkMapper chkptLinkMapper) {
        this.chkptLinkMapper = chkptLinkMapper;
    }

    /** 해당 SRC 사업장의 점검연동 ACTIVE 링크 목록(최초 1회만 조회 후 재사용). */
    List<LinkDstRaw> activeChkptLinks(String srcCmpnyCd, String srcSiteCd) {
        return linksBySite.computeIfAbsent(
                srcCmpnyCd + "|" + srcSiteCd,
                key -> chkptLinkMapper.selectActiveChkptLinksBySrcSite(srcCmpnyCd, srcSiteCd));
    }
}
