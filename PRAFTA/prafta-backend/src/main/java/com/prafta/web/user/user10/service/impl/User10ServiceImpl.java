package com.prafta.web.user.user10.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.siteauth.service.SiteAccessService;
import com.prafta.common.cmm.stdwork.StdWorkReasonCd;
import com.prafta.common.cmm.stdwork.command.StdWorkHoursSaveCommand;
import com.prafta.common.cmm.stdwork.service.StdWorkHoursService;
import com.prafta.common.cmm.stdwork.vo.StdWorkHoursSaveResult;
import com.prafta.common.cmm.stdwork.vo.StdWorkHoursSummaryVO;
import com.prafta.common.cmm.stdwork.vo.StdWorkHoursVO;
import com.prafta.common.cmm.stdwork.vo.StdWorkReasonRuleVO;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.user.UserErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.user.user10.application.param.StdWorkSaveParam;
import com.prafta.web.user.user10.application.param.StdWorkUserListParam;
import com.prafta.web.user.user10.application.query.StdWorkUserListQuery;
import com.prafta.web.user.user10.dto.response.StdWorkHistoryResponse;
import com.prafta.web.user.user10.dto.response.StdWorkReasonOptionsResponse;
import com.prafta.web.user.user10.dto.response.StdWorkSaveResponse;
import com.prafta.web.user.user10.dto.response.StdWorkUserListResponse;
import com.prafta.web.user.user10.mapper.User10Mapper;
import com.prafta.web.user.user10.result.StdWorkUserRowResult;
import com.prafta.web.user.user10.result.StdWorkUserScopeResult;
import com.prafta.web.user.user10.service.User10Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 소정-10: 소정근로시간 관리·이력 서비스 구현 (User_10).
 *
 * <p>모든 EP 가 <b>사업장 인가 + 부서 게이트</b>를 거친다. 쓰기/단건 조회는 대상 근로자의
 * 사업장·부서를 DB 에서 재조회해 게이트 입력으로 쓴다(바디 값 불신 — IDOR 차단).
 *
 * <p><b>로깅 규약(security M-3)</b> — 소정근로 <b>사유코드를 로그에 남기지 않는다.</b>
 * {@code userCd + 사유}(임신기·육아기·가족돌봄) 조합은 건강·가족관계 정보다(정책 §11.1).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class User10ServiceImpl implements User10Service {

    private final User10Mapper user10Mapper;
    /** 사업장 접근 인가(공용 cmm 빈). */
    private final SiteAccessService siteAccessService;
    /** 부서 스코프 게이트 — 기존 빈 재사용(판정 로직 복제 금지). */
    private final AttdCloseService attdCloseService;
    /** 소정근로 이력 단일 출처(조회·등록·정정·검증). */
    private final StdWorkHoursService stdWorkHoursService;

    // ====================================================================
    // 조회
    // ====================================================================

    @Override
    public StdWorkUserListResponse selectStdWorkUserList(StdWorkUserListParam param) {

        siteAccessService.assertSiteAccess(
                param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());
        assertCanManageNode(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd(), param.nodeCd());

        // 단시간 판정 분모는 조회 대상 사업장의 기준값이다(사업장 오버라이드 → 회사 기본값 → 2400).
        //   목록은 항상 사업장 1곳으로 한정되므로(StdWorkUserListParam 이 siteCd 를 필수로 강제)
        //   행마다 다른 사업장이 섞이지 않는다.
        int cmpnyWeekStdMinutes = stdWorkHoursService.resolveSiteWeekStdMinutes(param.gvCmpnyCd(), param.siteCd());

        List<StdWorkUserRowResult> rows = user10Mapper.selectStdWorkUserList(StdWorkUserListQuery.from(param));

        List<StdWorkUserListResponse.Row> list = new ArrayList<>();
        if (rows != null) {
            for (StdWorkUserRowResult row : rows) {
                boolean fromHistory = row.weekStdMinutes() != null && row.weekStdMinutes() > 0;
                // 미입력 계정은 회사 기준값으로 폴백 해석되므로 단시간이 될 수 없다(지시서 B-2).
                boolean partTime = fromHistory && row.weekStdMinutes() < cmpnyWeekStdMinutes;

                list.add(StdWorkUserListResponse.Row.builder()
                        .userCd(row.userCd())
                        .userId(row.userId())
                        .userNm(row.userNm())
                        .nodeCd(row.nodeCd())
                        .nodeNm(row.nodeNm())
                        .employmentType(row.employmentType())
                        .hireDate(row.hireDate())
                        .weekStdMinutes(row.weekStdMinutes())
                        .reasonCd(row.reasonCd())
                        .reasonNm(row.reasonNm())
                        .applyStrDate(row.applyStrDate())
                        .applyEndDate(row.applyEndDate())
                        .partTime(partTime)
                        .fromHistory(fromHistory)
                        .build());
            }
        }

        log.info("User_10 소정근로 관리 목록 조회 - siteCd={}, nodeCd={}, {}건",
                param.siteCd(), param.nodeCd(), list.size());

        return StdWorkUserListResponse.builder()
                .cmpnyWeekStdMinutes(cmpnyWeekStdMinutes)
                .stdWorkUserList(list)
                .build();
    }

    @Override
    public StdWorkHistoryResponse selectStdWorkHistory(String gvCmpnyCd, String gvAuthCd, String gvUserCd,
                                                       String gvSiteCd, String targetUserCd) {

        StdWorkUserScopeResult target = loadTargetWithGate(gvCmpnyCd, gvAuthCd, gvUserCd, gvSiteCd, targetUserCd);

        StdWorkHoursSummaryVO summary = stdWorkHoursService.resolveSummary(gvCmpnyCd, target.userCd(), null);
        List<StdWorkHoursVO> history = stdWorkHoursService.findHistory(gvCmpnyCd, target.userCd());

        log.info("User_10 소정근로 이력 조회 - 요청자={}, 대상userCd={}, 이력={}건",
                gvUserCd, target.userCd(), history.size());

        return StdWorkHistoryResponse.builder()
                .userCd(target.userCd())
                .userNm(target.userNm())
                .employmentType(target.employmentType())
                .baseYmd(summary.getBaseYmd())
                .weekStdMinutes(summary.getWeekStdMinutes())
                .cmpnyWeekStdMinutes(summary.getCmpnyWeekStdMinutes())
                .source(summary.getSource() == null ? null : summary.getSource().name())
                .fromHistory(summary.isFromHistory())
                .partTime(summary.isPartTime())
                .eligible(summary.isEligible())
                .dailyWorker(summary.isDailyWorker())
                .historyList(history)
                .build();
    }

    /**
     * 등록 팝업 옵션.
     *
     * <p>{@code siteCd} 는 "풀타임 기본값"으로 채워 넣을 통상 기준값을 고르기 위한 <b>대상 사업장</b>이다.
     * 미지정이면 회사 기본값을 쓴다(종전 동작). 지정 시에는 다른 사업장의 기준값이 새어 나가지
     * 않도록 사업장 인가를 건다.
     */
    @Override
    public StdWorkReasonOptionsResponse getReasonOptions(String cmpnyCd, String gvAuthCd, String gvUserCd,
                                                         String gvSiteCd, String siteCd) {

        if (cmpnyCd == null || cmpnyCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (siteCd != null && !siteCd.isBlank()) {
            siteAccessService.assertSiteAccess(cmpnyCd, gvUserCd, gvAuthCd, gvSiteCd, siteCd);
        }

        List<StdWorkReasonOptionsResponse.ReasonOption> options = new ArrayList<>();
        for (StdWorkReasonRuleVO rule : stdWorkHoursService.findReasonRules()) {
            if (rule == null || rule.getReasonCd() == null) {
                continue;
            }
            options.add(StdWorkReasonOptionsResponse.ReasonOption.builder()
                    .reasonCd(rule.getReasonCd())
                    .reasonNm(rule.getReasonNm())
                    .reduced(StdWorkReasonCd.isReduced(rule.getReasonCd()))
                    .build());
        }

        return StdWorkReasonOptionsResponse.builder()
                .cmpnyWeekStdMinutes(stdWorkHoursService.resolveSiteWeekStdMinutes(cmpnyCd, siteCd))
                .minWarnWeekMinutes(StdWorkHoursService.MIN_WARN_WEEK_MINUTES)
                .childcareMinWeekMinutes(StdWorkHoursService.CHILDCARE_MIN_WEEK_MINUTES)
                .childcareMaxWeekMinutes(StdWorkHoursService.CHILDCARE_MAX_WEEK_MINUTES)
                .reasonOptions(options)
                .build();
    }

    // ====================================================================
    // 등록 / 정정
    // ====================================================================

    /**
     * ★트랜잭션 경계 — 본 메서드에 {@code @Transactional} 을 두지 않는다.
     *
     * <p>실제 쓰기는 {@code StdWorkHoursService.register}(@Transactional) 한 호출이 전부이며,
     * 그 안에서 "직전 행 마감 + 신규 행 INSERT + 복귀 행 자동 생성"이 <b>이미 원자적으로</b>
     * 처리된다. 여기서 트랜잭션을 한 겹 더 열면 인가 조회까지 같은 트랜잭션에 묶여 이득 없이
     * 락 구간만 길어진다.
     */
    @Override
    public StdWorkSaveResponse registerStdWorkHours(StdWorkSaveParam param) {

        StdWorkUserScopeResult target = loadTargetWithGate(
                param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), param.gvSiteCd(), param.userCd());

        StdWorkHoursSaveResult result = stdWorkHoursService.register(toCommand(param, target.userCd()));

        // ★사유코드는 로그에 남기지 않는다(security M-3).
        log.info("User_10 소정근로 이력 등록 - 요청자={}, 대상userCd={}, 적용={}~{}, 주소정={}분, 경고={}건",
                param.gvUserCd(), target.userCd(), param.applyStrDate(),
                param.applyEndDate() == null ? "무기한" : param.applyEndDate(),
                param.weekStdMinutes(),
                result.getWarnings() == null ? 0 : result.getWarnings().size());

        return StdWorkSaveResponse.of(result);
    }

    /** 정정도 {@code correct} 한 호출이 원자 단위다(복귀 행 이동 포함) — 상단 주석 참조. */
    @Override
    public StdWorkSaveResponse correctStdWorkHours(StdWorkSaveParam param) {

        StdWorkUserScopeResult target = loadTargetWithGate(
                param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), param.gvSiteCd(), param.userCd());

        StdWorkHoursSaveResult result = stdWorkHoursService.correct(toCommand(param, target.userCd()));

        log.info("User_10 소정근로 이력 정정 - 요청자={}, 대상userCd={}, 적용시작={}, 종료={}, 주소정={}분, 경고={}건",
                param.gvUserCd(), target.userCd(), param.applyStrDate(),
                param.applyEndDate() == null ? "무기한" : param.applyEndDate(),
                param.weekStdMinutes(),
                result.getWarnings() == null ? 0 : result.getWarnings().size());

        return StdWorkSaveResponse.of(result);
    }

    // ====================================================================
    // 내부
    // ====================================================================

    /**
     * 대상 근로자 로드 + 사업장 인가 + 부서 게이트 (타인 스코프 EP 공통 진입).
     *
     * <p>★이 화면의 모든 EP 는 {@code userCd} 를 입력으로 받는다. 게이트 없이 두면 일반 사원이
     * 전 직원의 계약 근로시간·이력을 열람·변경할 수 있다
     * (feedback_web_new_query_screen_needs_node_gate — 3회 재발 실증).
     */
    private StdWorkUserScopeResult loadTargetWithGate(String gvCmpnyCd, String gvAuthCd, String gvUserCd,
                                                      String gvSiteCd, String targetUserCd) {

        if (targetUserCd == null || targetUserCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        StdWorkUserScopeResult target = user10Mapper.selectUserScope(gvCmpnyCd, targetUserCd);
        if (target == null) {
            throw new ApiException(UserErrorCode.USER_404_004);
        }

        siteAccessService.assertSiteAccess(gvCmpnyCd, gvUserCd, gvAuthCd, gvSiteCd, target.siteCd());
        assertCanManageNode(gvAuthCd, gvUserCd, gvCmpnyCd, target.siteCd(), target.nodeCd());

        return target;
    }

    /** 부서 스코프 게이트 — master/hr 전사 통과, 그 외는 해당·상위 부서 정·부 관리자만(safe 제외). */
    private void assertCanManageNode(String gvAuthCd, String gvUserCd, String cmpnyCd, String siteCd, String nodeCd) {

        if (!attdCloseService.canManageNodeExcludeSafe(gvAuthCd, gvUserCd, cmpnyCd, siteCd, nodeCd)) {
            log.warn("User_10 접근 권한 없음 - userCd={}, authCd={}, siteCd={}, nodeCd={}",
                    gvUserCd, gvAuthCd, siteCd, nodeCd);
            throw new ApiException(UserErrorCode.USER_403_002);
        }
    }

    /** 파라미터 → 공용 커맨드. 회사/작업자는 토큰 도출값만 싣는다(클라 바디 신뢰 금지). */
    private StdWorkHoursSaveCommand toCommand(StdWorkSaveParam param, String targetUserCd) {

        return StdWorkHoursSaveCommand.builder()
                .cmpnyCd(param.gvCmpnyCd())
                .userCd(targetUserCd)
                .applyStrDate(param.applyStrDate())
                .applyEndDate(param.applyEndDate())
                .weekStdMinutes(param.weekStdMinutes())
                .reasonCd(param.reasonCd())
                .reasonDetail(param.reasonDetail())
                .actorNo(param.gvUserCd())
                .build();
    }
}
