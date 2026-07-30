package com.prafta.web.attd.attd16.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.siteauth.service.SiteAccessService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.attd.attd16.application.param.LeaveUsageCalendarParam;
import com.prafta.web.attd.attd16.application.query.LeaveUsageCalendarQuery;
import com.prafta.web.attd.attd16.dto.response.LeaveUsageCalendarResponse;
import com.prafta.web.attd.attd16.mapper.Attd16Mapper;
import com.prafta.web.attd.attd16.result.LeaveUsageCalendarRowResult;
import com.prafta.web.attd.attd16.service.Attd16Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ATTD16-T1 - 연차 사용 현황 캘린더 서비스 구현(읽기 전용).
 *
 * <p>처리 순서(plan §3):
 * <ol>
 *   <li>사업장 접근 인가 — {@code SiteAccessService.assertSiteAccess}(TB_USER_SITE_AUTH 원장 기반,
 *       master/hr 전사 허용 내장). attd07~15 전례 동일 시그니처.</li>
 *   <li>역할·부서 스코프 게이트 — {@code AttdCloseService.canManageNode}(SEC-A16-01).
 *       사업장 인가만으로는 자기 사업장 fast path 때문에 일반 사원도 전 직원 연차를 열람할 수 있어,
 *       PII 노출 조회 화면 전례(Attd_11/Attd_15)와 동일하게 서버에서 강제한다
 *       (메뉴/버튼 권한은 서버에서 검사되지 않으므로 LNB 미노출은 방어가 아니다).</li>
 *   <li>월 범위 검증/산출은 Param 에서 완료(서버 재계산) — 여기서는 쿼리 변환만.</li>
 *   <li>매퍼 단일 SELECT — 기간형 연차의 일자 전개까지 SQL 에서 마치고 그대로 응답한다
 *       (집계/가공은 프론트 그룹핑 담당 — plan §4.2/§4.3).</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Attd16ServiceImpl implements Attd16Service {

    private final Attd16Mapper attd16Mapper;
    /** 사업장 접근 인가(공용 cmm 빈) — User_03 원장(TB_USER_SITE_AUTH) 기반(신규 등식가드 금지). */
    private final SiteAccessService siteAccessService;
    /** 역할·부서 스코프 게이트(canManageNode) — Attd_11/Attd_15 와 동일 빈 재사용(로직 미복제). */
    private final AttdCloseService attdCloseService;

    @Override
    public LeaveUsageCalendarResponse getLeaveUsageCalendar(LeaveUsageCalendarParam param) {

        log.info("Attd_16 연차 사용 현황 조회 진입 - searchYm={}, siteCd={}, nodeCd={}, incSub={}",
                param.searchYm(), param.siteCd(), param.nodeCd(), param.incSubNodeYn());

        // 사업장 접근 인가(원장 기반).
        siteAccessService.assertSiteAccess(
                param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());

        // 역할·부서 스코프 게이트 — master/hr/safe 전사 통과, 그 외는 본인이 관리하는 부서(하위 포함)만.
        //   비전사 역할은 부서 미지정 조회가 차단된다(Attd_11/Attd_15 와 동일 UX).
        if (!attdCloseService.canManageNode(
                param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd(), param.nodeCd())) {
            log.warn("Attd_16 조회 권한 없음 - userCd={}, authCd={}, siteCd={}, nodeCd={}",
                    param.gvUserCd(), param.gvAuthCd(), param.siteCd(), param.nodeCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        LeaveUsageCalendarQuery query = LeaveUsageCalendarQuery.from(param);

        List<LeaveUsageCalendarRowResult> resultList = attd16Mapper.selectLeaveUsageCalendarList(query);

        log.info("Attd_16 연차 사용 현황 조회 종료 - 전개 행 {}건", resultList.size());

        return LeaveUsageCalendarResponse.builder()
                .leaveUsageCalendarResultList(resultList)
                .build();
    }
}
