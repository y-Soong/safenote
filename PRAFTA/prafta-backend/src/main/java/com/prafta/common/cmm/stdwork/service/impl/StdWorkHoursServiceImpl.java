package com.prafta.common.cmm.stdwork.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.common.cmm.stdwork.StdWorkReasonCd;
import com.prafta.common.cmm.stdwork.command.StdWorkHoursSaveCommand;
import com.prafta.common.cmm.stdwork.mapper.StdWorkHoursMapper;
import com.prafta.common.cmm.stdwork.service.StdWorkHoursService;
import com.prafta.common.cmm.stdwork.vo.StdWorkHoursSaveResult;
import com.prafta.common.cmm.stdwork.vo.StdWorkHoursSummaryVO;
import com.prafta.common.cmm.stdwork.vo.StdWorkHoursVO;
import com.prafta.common.cmm.stdwork.vo.StdWorkPolicyVO;
import com.prafta.common.cmm.stdwork.vo.StdWorkReasonRuleVO;
import com.prafta.common.cmm.stdwork.vo.StdWorkUserScopeVO;
import com.prafta.common.error.stdwork.StdWorkErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.common.util.DateTimeUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 소정-02: 근로자별 소정근로시간 공용 서비스 구현.
 *
 * <p>plan §4 소정-02. 0단계는 <b>데이터 축</b>만 담당하며 연차 판정·부여 로직에 연결하지 않는다.
 *
 * <p><b>일용직 제외 방식</b>
 * <ul>
 *   <li>등록/정정/검증(쓰기 경로): 고용형태를 조회해 계정 부재·사용중지면 404,
 *       DAILY 면 403 으로 차단한다(fail-closed).</li>
 *   <li>조회 경로: <b>예외를 던지지 않고</b> 폴백 값을 그대로 반환한다(배치 루프가 일부
 *       계정 때문에 통째로 실패하는 것을 피하기 위함). 대신
 *       {@code StdWorkHoursSummaryVO.eligible / dailyWorker} 에 대상 여부를 실어 소비처가
 *       오독하지 않게 한다. 목록 화면/EP 는 {@link #isEligible} 로 사전 필터링한다.</li>
 * </ul>
 *
 * <p><b>단축 종료 후 복귀 행 자동 생성(H-1)</b> — {@link #register} 참조. 단축 사유 행이
 * 끝난 다음 날부터 이력이 비면 폴백(통상 간주)으로 승격되어 단시간 판정·비례부여 분모가
 * 틀어지므로, 마감된 직전 행의 값을 승계한 열린 행을 같은 트랜잭션에서 만든다.
 *
 * <p><b>★로깅 규약 (security M-3)</b> — 로그에 <b>사유코드(REASON_CD)를 남기지 않는다.</b>
 * {@code userCd + 사유}(임신기·육아기·가족돌봄 단축) 조합은 건강·가족관계 정보에 해당하고
 * (정책 §11.1), 운영이 파일 로깅 상태라 로그 열람자 전원에게 노출된다. 어떤 사유였는지는
 * 이력 테이블(TB_USER_STD_WORK_HOURS)과 INSERT_NO/UPDATE_NO 로 추적한다. 분기 진단이
 * 필요하면 사유코드 대신 "단축 사유 인접" 같은 <b>정적 문구</b>로 표현할 것.
 *
 * <p><b>동시성 메모(현재 미조치)</b> — 겹침 검증(countOverlap)과 INSERT/UPDATE 사이에 행 락이
 * 없어, 같은 근로자의 이력을 두 관리자가 동시에 저장하면 이론적으로 겹치는 구간이 들어갈 수
 * 있다. 0단계는 호출부가 아직 없고 실사용도 관리자 단독 작성 전제라 조치하지 않았다.
 * 화면(소정-10)·엑셀 일괄 등록(소정-03)이 붙는 시점에
 * {@code SELECT ... FOR UPDATE}(사용자 단위 직렬화) 또는 애드바이저리 락 도입을 검토할 것.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StdWorkHoursServiceImpl implements StdWorkHoursService {

    /** 주 소정근로 분의 물리적 상한 (168시간 = 10080분). 정책이 아니라 오입력 방어선. */
    private static final int MAX_WEEK_MINUTES = 10080;

    private final StdWorkHoursMapper stdWorkHoursMapper;

    // ===================== 조회 =====================

    @Override
    public StdWorkHoursVO findEffectiveRow(String cmpnyCd, String userCd, String baseYmd) {

        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(userCd)) {
            throw new ApiException(StdWorkErrorCode.STDWORK_400_001);
        }
        String ymd = resolveBaseYmd(baseYmd);
        return stdWorkHoursMapper.selectEffectiveRow(cmpnyCd, userCd, ymd);
    }

    @Override
    public StdWorkHoursVO findCurrentRow(String cmpnyCd, String userCd) {
        return findEffectiveRow(cmpnyCd, userCd, stdWorkHoursMapper.selectTodayYmd());
    }

    @Override
    public int resolveWeekStdMinutes(String cmpnyCd, String userCd, String baseYmd) {
        return resolveSummary(cmpnyCd, userCd, baseYmd).getWeekStdMinutes();
    }

    @Override
    public int resolveCurrentWeekStdMinutes(String cmpnyCd, String userCd) {
        return resolveWeekStdMinutes(cmpnyCd, userCd, stdWorkHoursMapper.selectTodayYmd());
    }

    @Override
    public int resolveCmpnyWeekStdMinutes(String cmpnyCd) {
        return resolveSiteWeekStdMinutes(cmpnyCd, null);
    }

    @Override
    public int resolveSiteWeekStdMinutes(String cmpnyCd, String siteCd) {

        if (!StringUtils.hasText(cmpnyCd)) {
            throw new ApiException(StdWorkErrorCode.STDWORK_400_001);
        }

        StdWorkPolicyVO policy = stdWorkHoursMapper.selectEffectivePolicy(cmpnyCd, trimToNull(siteCd));
        return resolvePolicyMinutes(policy);
    }

    @Override
    public Integer findPolicyWeekStdMinutes(String cmpnyCd, String siteCd) {

        if (!StringUtils.hasText(cmpnyCd)) {
            throw new ApiException(StdWorkErrorCode.STDWORK_400_001);
        }

        String site = trimToNull(siteCd);
        StdWorkPolicyVO policy = (site == null)
                ? stdWorkHoursMapper.selectPolicy(cmpnyCd, StdWorkPolicyVO.SCOPE_TYPE_COMPANY, StdWorkPolicyVO.SCOPE_CD_COMPANY)
                : stdWorkHoursMapper.selectPolicy(cmpnyCd, StdWorkPolicyVO.SCOPE_TYPE_SITE, site);

        if (policy == null || policy.getWeekStdMinutes() == null || policy.getWeekStdMinutes() <= 0) {
            return null;
        }
        return policy.getWeekStdMinutes();
    }

    @Override
    public StdWorkHoursSummaryVO resolveSummary(String cmpnyCd, String userCd, String baseYmd) {

        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(userCd)) {
            throw new ApiException(StdWorkErrorCode.STDWORK_400_001);
        }

        String ymd = resolveBaseYmd(baseYmd);

        // 대상 여부(일용직·탈퇴·미존재)와 소속 사업장을 한 행으로 읽는다.
        // 조회 경로에서는 대상이 아니어도 예외를 던지지 않는다(배치 루프 전체 실패 방지).
        StdWorkUserScopeVO userScope = stdWorkHoursMapper.selectUserScope(cmpnyCd, userCd);
        String employmentType = (userScope == null) ? null : userScope.getEmploymentType();
        String userSiteCd = (userScope == null) ? null : trimToNull(userScope.getSiteCd());
        boolean dailyWorker = AuthRoleUtils.isDailyWorker(employmentType);

        // 기준값(비교 분모)은 대상 근로자의 소속 사업장 기준으로 1회만 조회한다.
        // ★사업장 오버라이드가 있으면 그 값이 분모다 — 회사 기본값으로 고정하면 통상 40시간이
        //   아닌 사업장의 통상근로자가 단시간으로 오분류된다(지시서 사고 시나리오).
        StdWorkPolicyVO policy = stdWorkHoursMapper.selectEffectivePolicy(cmpnyCd, userSiteCd);
        int baseMinutes = resolvePolicyMinutes(policy);

        StdWorkHoursVO row = stdWorkHoursMapper.selectEffectiveRow(cmpnyCd, userCd, ymd);

        // 폴백 체인: 이력 행 → 사업장 오버라이드 → 회사 기준값 → 코드 상수 2400.
        int weekStdMinutes;
        StdWorkHoursSummaryVO.StdWorkSource source;
        if (row != null && row.getWeekStdMinutes() != null && row.getWeekStdMinutes() > 0) {
            weekStdMinutes = row.getWeekStdMinutes();
            source = StdWorkHoursSummaryVO.StdWorkSource.USER_HISTORY;
        } else {
            weekStdMinutes = baseMinutes;
            source = resolvePolicySource(policy);
            row = null;
        }

        return StdWorkHoursSummaryVO.builder()
                .cmpnyCd(cmpnyCd)
                .userCd(userCd)
                .siteCd(userSiteCd)
                .baseYmd(ymd)
                .weekStdMinutes(weekStdMinutes)
                .cmpnyWeekStdMinutes(baseMinutes)
                .source(source)
                .partTime(weekStdMinutes < baseMinutes)
                .effectiveRow(row)
                .eligible(employmentType != null && !dailyWorker)
                .dailyWorker(dailyWorker)
                .build();
    }

    /** 기준값 행 → 적용 분. 행 부재/비정상 값이면 코드 상수 2400 으로 폴백(plan §1.2). */
    private int resolvePolicyMinutes(StdWorkPolicyVO policy) {

        if (policy == null || policy.getWeekStdMinutes() == null || policy.getWeekStdMinutes() <= 0) {
            // 행 부재 = 통상 주 40시간. 전 회사 백필 시드를 두지 않는 설계의 근거 지점(plan §1.2).
            return DEFAULT_WEEK_STD_MINUTES;
        }
        return policy.getWeekStdMinutes();
    }

    /** 기준값 폴백 출처 — 화면 배지가 "사업장 지정 / 회사 기본 / 시스템 기본"을 구분한다. */
    private StdWorkHoursSummaryVO.StdWorkSource resolvePolicySource(StdWorkPolicyVO policy) {

        if (policy == null || policy.getWeekStdMinutes() == null || policy.getWeekStdMinutes() <= 0) {
            return StdWorkHoursSummaryVO.StdWorkSource.SYSTEM_DEFAULT;
        }
        return policy.isSiteScope()
                ? StdWorkHoursSummaryVO.StdWorkSource.SITE_POLICY
                : StdWorkHoursSummaryVO.StdWorkSource.COMPANY_POLICY;
    }

    @Override
    public boolean isPartTime(String cmpnyCd, String userCd, String baseYmd) {
        return resolveSummary(cmpnyCd, userCd, baseYmd).isPartTime();
    }

    @Override
    public List<StdWorkHoursVO> findHistory(String cmpnyCd, String userCd) {

        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(userCd)) {
            throw new ApiException(StdWorkErrorCode.STDWORK_400_001);
        }

        List<StdWorkHoursVO> rows = stdWorkHoursMapper.selectHistory(cmpnyCd, userCd);
        return rows == null ? Collections.emptyList() : rows;
    }

    @Override
    public StdWorkReasonRuleVO findReasonRule(String reasonCd) {

        if (!StringUtils.hasText(reasonCd)) {
            return null;
        }
        return stdWorkHoursMapper.selectReasonRule(reasonCd);
    }

    @Override
    public List<StdWorkReasonRuleVO> findReasonRules() {

        List<StdWorkReasonRuleVO> rules = stdWorkHoursMapper.selectReasonRules();
        return rules == null ? Collections.emptyList() : rules;
    }

    @Override
    public boolean isEligible(String cmpnyCd, String userCd) {

        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(userCd)) {
            throw new ApiException(StdWorkErrorCode.STDWORK_400_001);
        }

        StdWorkUserScopeVO userScope = stdWorkHoursMapper.selectUserScope(cmpnyCd, userCd);
        return userScope != null && !AuthRoleUtils.isDailyWorker(userScope.getEmploymentType());
    }

    // ===================== 등록 / 변경 =====================

    @Override
    @Transactional
    public StdWorkHoursSaveResult register(StdWorkHoursSaveCommand command) {

        StdWorkHoursSaveCommand cmd = normalize(command);
        List<String> warnings = validateCommon(cmd);

        // 등록은 같은 적용 시작일 행이 이미 있으면 중복이다(정정은 correct 경로).
        if (stdWorkHoursMapper.selectRowByApplyStrDate(cmd.getCmpnyCd(), cmd.getUserCd(), cmd.getApplyStrDate()) != null) {
            throw new ApiException(StdWorkErrorCode.STDWORK_409_001);
        }
        assertNoOverlap(cmd.getCmpnyCd(), cmd.getUserCd(), cmd.getApplyStrDate(), cmd.getApplyEndDate(), null, null);

        // 마감 전에 직전 열린 행을 먼저 읽어 둔다 — 복귀 행이 승계할 값의 출처(H-1).
        StdWorkHoursVO prevOpenRow = stdWorkHoursMapper.selectOpenRowBefore(
                cmd.getCmpnyCd(), cmd.getUserCd(), cmd.getApplyStrDate());

        // 이력 원칙: 직전 열린 행을 신규 적용일 전일로 마감한 뒤 신규 행을 쌓는다.
        String closeYmd = DateTimeUtils.plusDays(cmd.getApplyStrDate(), -1);
        int closed = stdWorkHoursMapper.closeOpenRowBefore(
                cmd.getCmpnyCd(), cmd.getUserCd(), cmd.getApplyStrDate(), closeYmd, cmd.getActorNo());

        stdWorkHoursMapper.insertRow(cmd);

        log.info("[stdWork] 소정근로시간 이력 등록: cmpnyCd={}, userCd={}, 적용={}~{}, 주소정={}분, 직전행마감={}건, 경고={}건",
                cmd.getCmpnyCd(), cmd.getUserCd(), cmd.getApplyStrDate(),
                cmd.getApplyEndDate() == null ? "무기한" : cmd.getApplyEndDate(),
                cmd.getWeekStdMinutes(), closed, warnings.size());

        // 단축 종료 후 복귀 행 자동 생성(H-1). 경고가 추가될 수 있어 warnings 를 그대로 넘긴다.
        StdWorkHoursVO restoreRow = createRestoreRowIfNeeded(cmd, prevOpenRow, closed, warnings);

        return StdWorkHoursSaveResult.builder()
                .applyStrDate(cmd.getApplyStrDate())
                .closedPrevEndDate(closed > 0 ? closeYmd : null)
                .restoreStrDate(restoreRow == null ? null : restoreRow.getApplyStrDate())
                .restoreWeekStdMinutes(restoreRow == null ? null : restoreRow.getWeekStdMinutes())
                .restoreReasonCd(restoreRow == null ? null : restoreRow.getReasonCd())
                .warnings(warnings)
                .build();
    }

    /**
     * 단축 종료 후 복귀 행 자동 생성 (★H-1 확정 규칙).
     *
     * <p>유한 기간(단축 사유) 행을 등록하면 직전 열린 행이 마감되는데, 단축 종료 다음 날부터
     * 이력이 비면 {@code resolveSummary} 가 회사 기준값/2400 으로 폴백해 <b>단시간근로자가
     * 통상근로자로 자동 승격</b>된다(2단계 비례부여 분모·단시간 파생 판정이 전부 틀어짐).
     * 이를 막기 위해 마감된 직전 행의 계약값을 그대로 승계한 열린 행을 같은 트랜잭션에서 만든다.
     *
     * <p>승계 대상: {@code WEEK_STD_MINUTES}, {@code REASON_CD}, {@code REASON_DETAIL}.
     * 복귀 행의 적용 시작일 = 신규 행 종료일 + 1일, 종료일 = NULL(열린 행).
     *
     * <p>{@code INSERT_NO} 는 <b>등록 작업자</b>로 둔다(그 사람의 조작에서 파생된 행이므로).
     * 자동 생성 사실은 로그와 본 주석으로만 남기고 {@code REASON_DETAIL} 에 표식을 덧붙이지
     * 않는다 — 사용자가 입력한 사유 상세를 시스템 문구로 오염시키면 이후 정정·이동 시
     * 표식이 중복 누적되고, 화면 표기도 왜곡되기 때문이다.
     *
     * <p>생성하지 않는 경우
     * <ol>
     *   <li>신규 행이 무기한(종료일 NULL) — 뒤에 공백이 생기지 않는다.</li>
     *   <li>직전 열린 행이 없어 마감이 발생하지 않음 — 원래 이력이 없던 계정은 단축 종료 후
     *       폴백(통상 간주)이 의미상 정합하므로 그대로 둔다.</li>
     *   <li>복귀 구간이 기존 다른 행과 겹침(종료일 이후에 이미 등록된 미래 행 존재) — skip 하고
     *       경고 1건을 추가한다.</li>
     *   <li>승계할 사유가 단축 사유 — 단축 사유는 유한 기간이 필수라 열린 복귀 행으로 쓸 수 없다
     *       (직전 "열린" 행이므로 정상 데이터에서는 발생하지 않는 방어 분기).</li>
     * </ol>
     *
     * @return 생성된 복귀 행(생성하지 않았으면 null)
     */
    private StdWorkHoursVO createRestoreRowIfNeeded(StdWorkHoursSaveCommand cmd,
                                                    StdWorkHoursVO prevOpenRow,
                                                    int closed,
                                                    List<String> warnings) {

        // 1) 무기한 행 등록이면 복귀 개념 자체가 없다.
        if (cmd.getApplyEndDate() == null) {
            return null;
        }

        // 2) 마감된 직전 행이 없으면 승계할 계약값이 없다 → 종전대로 폴백에 맡긴다.
        if (closed <= 0 || prevOpenRow == null
                || prevOpenRow.getWeekStdMinutes() == null || prevOpenRow.getWeekStdMinutes() <= 0) {
            return null;
        }

        // 3) 방어: 승계 사유가 단축 사유면 열린 행으로 만들 수 없다(종료일 필수 규칙과 충돌).
        if (StdWorkReasonCd.isReduced(prevOpenRow.getReasonCd())) {
            log.info("[stdWork] 복귀 행 자동 생성 skip(승계 사유가 단축 사유): cmpnyCd={}, userCd={}",
                    cmd.getCmpnyCd(), cmd.getUserCd());
            return null;
        }

        String restoreStrDate = DateTimeUtils.plusDays(cmd.getApplyEndDate(), 1);
        if (restoreStrDate == null) {
            return null;
        }

        // 4) 복귀 구간이 기존 행과 겹치면 생성하지 않는다(미래 행이 이미 등록된 경우).
        int overlap = stdWorkHoursMapper.countOverlap(
                cmd.getCmpnyCd(), cmd.getUserCd(), restoreStrDate, null, null, null);
        if (overlap > 0) {
            log.info("[stdWork] 복귀 행 자동 생성 skip(기존 이력과 겹침): cmpnyCd={}, userCd={}, 복귀시작일={}, 겹침={}건",
                    cmd.getCmpnyCd(), cmd.getUserCd(), restoreStrDate, overlap);
            warnings.add("단축 종료 후 적용될 소정근로 이력이 이미 존재해 복귀 이력을 자동 생성하지 않았습니다. 이력을 확인해 주세요.");
            return null;
        }

        StdWorkHoursSaveCommand restoreCmd = StdWorkHoursSaveCommand.builder()
                .cmpnyCd(cmd.getCmpnyCd())
                .userCd(cmd.getUserCd())
                .applyStrDate(restoreStrDate)
                .applyEndDate(null)
                .weekStdMinutes(prevOpenRow.getWeekStdMinutes())
                .reasonCd(prevOpenRow.getReasonCd())
                .reasonDetail(prevOpenRow.getReasonDetail())
                .actorNo(cmd.getActorNo())
                .build();
        stdWorkHoursMapper.insertRow(restoreCmd);

        log.info("[stdWork] 단축 종료 후 복귀 이력 자동 생성: cmpnyCd={}, userCd={}, 복귀시작일={}, 주소정={}분",
                cmd.getCmpnyCd(), cmd.getUserCd(), restoreStrDate,
                prevOpenRow.getWeekStdMinutes());

        StdWorkHoursVO restoreRow = new StdWorkHoursVO();
        restoreRow.setCmpnyCd(cmd.getCmpnyCd());
        restoreRow.setUserCd(cmd.getUserCd());
        restoreRow.setApplyStrDate(restoreStrDate);
        restoreRow.setWeekStdMinutes(prevOpenRow.getWeekStdMinutes());
        restoreRow.setReasonCd(prevOpenRow.getReasonCd());
        restoreRow.setReasonDetail(prevOpenRow.getReasonDetail());
        return restoreRow;
    }

    @Override
    @Transactional
    public StdWorkHoursSaveResult correct(StdWorkHoursSaveCommand command) {

        StdWorkHoursSaveCommand cmd = normalize(command);
        List<String> warnings = validateCommon(cmd);

        StdWorkHoursVO current = stdWorkHoursMapper.selectRowByApplyStrDate(
                cmd.getCmpnyCd(), cmd.getUserCd(), cmd.getApplyStrDate());
        if (current == null) {
            throw new ApiException(StdWorkErrorCode.STDWORK_404_002);
        }

        // ★N-3: 열린 행에 종료일을 처음 부여하면 그 다음 날부터 이력이 비어 폴백(통상 간주)으로
        //        승격된다. 등록(register) 경로의 복귀 행 자동 생성이 적용되지 않는 구간이라
        //        차단 대신 경고로 알린다.
        if (current.getApplyEndDate() == null && cmd.getApplyEndDate() != null) {
            warnings.add("종료일 이후에 적용될 소정근로 이력이 없어 통상 기준(주 40시간)으로 간주됩니다. 이어질 소정근로시간을 등록해 주세요.");
        }

        // 종료일이 바뀌면 뒤에 붙어 있는 복귀 행도 같이 옮겨야 한다(확정 규칙).
        StdWorkHoursVO adjacentRow = findAdjacentRestoreRow(cmd, current);
        String movedStrDate = (adjacentRow == null) ? null : DateTimeUtils.plusDays(cmd.getApplyEndDate(), 1);

        // ★N-1: 이동 결과가 역전 구간(시작일 > 종료일)이 되면 그 행은 어떤 조회에도 잡히지 않는
        //        좀비 행이 된다(유효행 조회·겹침 검증 모두 통과 못 함). PK 만 점유해 같은 시작일
        //        재등록을 막고 이력 타임라인에 뒤집힌 구간을 노출하므로 쓰기 전에 차단한다.
        //        countOverlap 은 자기모순 윈도우(시작일 > 종료일)에 0 을 반환해 잡아내지 못한다.
        if (adjacentRow != null
                && adjacentRow.getApplyEndDate() != null
                && movedStrDate != null
                && movedStrDate.compareTo(adjacentRow.getApplyEndDate()) > 0) {
            log.info("[stdWork] 정정 차단(후속 이력 소멸): cmpnyCd={}, userCd={}, 정정행시작={}, 복귀행 {}~{} -> 이동시작일 {}",
                    cmd.getCmpnyCd(), cmd.getUserCd(), cmd.getApplyStrDate(),
                    adjacentRow.getApplyStrDate(), adjacentRow.getApplyEndDate(), movedStrDate);
            throw new ApiException(StdWorkErrorCode.STDWORK_409_004);
        }

        // 겹침 검증은 "이동 후 최종 배치" 기준이어야 한다.
        // 1) 정정 행: 자기 자신 + 함께 이동될 복귀 행을 제외하고 본다.
        //    (복귀 행은 새 종료일 다음 날로 옮겨지므로 정정 행과 겹칠 수 없다)
        assertNoOverlap(cmd.getCmpnyCd(), cmd.getUserCd(), cmd.getApplyStrDate(), cmd.getApplyEndDate(),
                cmd.getApplyStrDate(), adjacentRow == null ? null : adjacentRow.getApplyStrDate());

        // 2) 이동될 복귀 행: 옮겨갈 위치에서 다른 행과 겹치면 정정 자체를 차단한다
        //    (뒤로 미루는데 그 뒤에 또 행이 있는 경우 — 조용히 데이터를 뭉개지 않는다).
        if (adjacentRow != null) {
            assertNoOverlap(cmd.getCmpnyCd(), cmd.getUserCd(), movedStrDate, adjacentRow.getApplyEndDate(),
                    adjacentRow.getApplyStrDate(), cmd.getApplyStrDate());
        }

        stdWorkHoursMapper.updateRow(cmd);

        // 복귀 행 이동: PK 에 적용 시작일이 포함되어 UPDATE 불가 → DELETE + INSERT(값 전량 승계).
        if (adjacentRow != null) {
            stdWorkHoursMapper.deleteRow(cmd.getCmpnyCd(), cmd.getUserCd(), adjacentRow.getApplyStrDate());
            String fromStrDate = adjacentRow.getApplyStrDate();
            adjacentRow.setApplyStrDate(movedStrDate);
            stdWorkHoursMapper.insertMovedRow(adjacentRow, cmd.getActorNo());

            log.info("[stdWork] 정정에 따른 복귀 이력 이동: cmpnyCd={}, userCd={}, 정정행시작={}, 복귀행 {} -> {}",
                    cmd.getCmpnyCd(), cmd.getUserCd(), cmd.getApplyStrDate(), fromStrDate, movedStrDate);

            log.info("[stdWork] 소정근로시간 이력 정정: cmpnyCd={}, userCd={}, 적용시작={}, 종료일 {} -> {}, 주소정={}분, 경고={}건",
                    cmd.getCmpnyCd(), cmd.getUserCd(), cmd.getApplyStrDate(),
                    current.getApplyEndDate(), cmd.getApplyEndDate(),
                    cmd.getWeekStdMinutes(), warnings.size());

            return StdWorkHoursSaveResult.builder()
                    .applyStrDate(cmd.getApplyStrDate())
                    .closedPrevEndDate(null)
                    .movedRestoreStrDate(movedStrDate)
                    .movedRestoreFromStrDate(fromStrDate)
                    .warnings(warnings)
                    .build();
        }

        log.info("[stdWork] 소정근로시간 이력 정정: cmpnyCd={}, userCd={}, 적용시작={}, 종료일 {} -> {}, 주소정={}분, 경고={}건",
                cmd.getCmpnyCd(), cmd.getUserCd(), cmd.getApplyStrDate(),
                current.getApplyEndDate(), cmd.getApplyEndDate(),
                cmd.getWeekStdMinutes(), warnings.size());

        return StdWorkHoursSaveResult.builder()
                .applyStrDate(cmd.getApplyStrDate())
                .closedPrevEndDate(null)
                .warnings(warnings)
                .build();
    }

    /**
     * 정정 대상 행에 인접한 복귀 행을 찾는다 (★확정 규칙).
     *
     * <p>자동 생성 여부를 구분하는 컬럼이 없으므로 <b>인접성</b>으로 판정한다:
     * 적용 시작일이 <b>정정 전 종료일 + 1일</b> 인 후속 행이 있으면 그 행을 복귀 행으로 본다.
     *
     * <p><b>★N-2 단축 사유 행 보호 (fail-closed)</b> — 인접 행이 단축 사유
     * (육아기·임신기·가족돌봄)면 이동시키지 않고 <b>정정 자체를 차단</b>한다
     * ({@link StdWorkErrorCode#STDWORK_409_003}).
     * 인접성만으로 판정하면 "마감된 통상 행의 종료일 정정"이 뒤따르는 단축 행을 끌고 가서
     * <b>법정 단축 개시일이 바뀌는</b> 사고가 난다(소정-07 OT 게이트 대상 기간과 2단계 비례부여
     * 구간이 함께 틀어짐). skip(이동만 안 함)으로 두면 종료일을 앞당길 때 조용한 공백이 남으므로
     * 차단이 안전하다. {@code register()} 의 "승계 사유가 단축이면 만들지 않음" 방어와 대칭.
     *
     * <p>이동 대상이 아닌 경우 (null 반환)
     * <ol>
     *   <li>정정 전 종료일이 없음(무기한 행) — 뒤에 붙은 행이라는 개념이 성립하지 않는다.
     *       이 경우 종료일이 새로 생기면 호출부가 N-3 경고를 남긴다.</li>
     *   <li>정정 후 종료일이 없음(무기한으로 변경) — 옮겨갈 자리가 없다. 이 경우 인접 행은
     *       정정 행의 무한 구간과 겹치므로 겹침 검증에서 차단된다.</li>
     *   <li>종료일이 그대로임 — 옮길 필요가 없다.</li>
     *   <li>인접 위치에 행이 없음 — 종전대로 종료일만 변경한다.</li>
     * </ol>
     */
    private StdWorkHoursVO findAdjacentRestoreRow(StdWorkHoursSaveCommand cmd, StdWorkHoursVO current) {

        String oldEndDate = current.getApplyEndDate();
        String newEndDate = cmd.getApplyEndDate();

        if (oldEndDate == null || newEndDate == null || oldEndDate.equals(newEndDate)) {
            return null;
        }

        String adjacentStrDate = DateTimeUtils.plusDays(oldEndDate, 1);
        if (adjacentStrDate == null) {
            return null;
        }

        StdWorkHoursVO adjacentRow =
                stdWorkHoursMapper.selectRowByApplyStrDate(cmd.getCmpnyCd(), cmd.getUserCd(), adjacentStrDate);
        if (adjacentRow == null) {
            return null;
        }

        // ★N-2: 단축 사유 행은 법정 기간이므로 정정에 딸려 움직이면 안 된다.
        if (StdWorkReasonCd.isReduced(adjacentRow.getReasonCd())) {
            log.info("[stdWork] 정정 차단(후속 단축 이력 인접): cmpnyCd={}, userCd={}, 정정행시작={}, 인접행={}",
                    cmd.getCmpnyCd(), cmd.getUserCd(), cmd.getApplyStrDate(),
                    adjacentRow.getApplyStrDate());
            throw new ApiException(StdWorkErrorCode.STDWORK_409_003);
        }
        return adjacentRow;
    }

    @Override
    public List<String> validateForWarning(StdWorkHoursSaveCommand command) {
        return validateCommon(normalize(command));
    }

    // ===================== 기준값(TB_CMPNY_STD_WORK_POLICY) 등록 / 변경 =====================

    @Override
    @Transactional
    public void saveWeekStdMinutesPolicy(String cmpnyCd, String siteCd, Integer weekStdMinutes, String actorNo) {

        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(actorNo)) {
            throw new ApiException(StdWorkErrorCode.STDWORK_400_001);
        }

        String site = trimToNull(siteCd);
        String scopeType = (site == null) ? StdWorkPolicyVO.SCOPE_TYPE_COMPANY : StdWorkPolicyVO.SCOPE_TYPE_SITE;
        String scopeCd = (site == null) ? StdWorkPolicyVO.SCOPE_CD_COMPANY : site;

        // 미지정 = 행 삭제(상속). "0 으로 저장"이 아니라 상위 스코프로 되돌리는 동작이다.
        if (weekStdMinutes == null) {
            int deleted = stdWorkHoursMapper.deletePolicy(cmpnyCd, scopeType, scopeCd);
            if (deleted > 0) {
                log.info("[stdWork] 통상근로시간 기준값 지정 해제(상속) - cmpnyCd={}, scope={}/{}, 삭제={}건",
                        cmpnyCd, scopeType, scopeCd, deleted);
            }
            return;
        }

        // 차단: 0 이하 / 법정 상한(주 40시간) 초과.
        if (weekStdMinutes <= 0) {
            throw new ApiException(StdWorkErrorCode.STDWORK_400_004);
        }
        if (weekStdMinutes > LEGAL_MAX_WEEK_MINUTES) {
            log.info("[stdWork] 통상근로시간 기준값 상한 초과 차단 - cmpnyCd={}, scope={}/{}, 입력={}분",
                    cmpnyCd, scopeType, scopeCd, weekStdMinutes);
            throw new ApiException(StdWorkErrorCode.STDWORK_400_007);
        }

        // 경고(저장 허용): 회사·사업장 통상 기준값이 초단시간 경계 미만이면 오입력 가능성이 높다.
        if (weekStdMinutes < MIN_WARN_WEEK_MINUTES) {
            log.warn("[stdWork] 통상근로시간 기준값이 주 15시간 미만입니다(오입력 확인 필요) - cmpnyCd={}, scope={}/{}, 입력={}분",
                    cmpnyCd, scopeType, scopeCd, weekStdMinutes);
        }

        stdWorkHoursMapper.upsertPolicy(cmpnyCd, scopeType, scopeCd, weekStdMinutes, actorNo);

        log.info("[stdWork] 통상근로시간 기준값 저장 - cmpnyCd={}, scope={}/{}, 주소정={}분",
                cmpnyCd, scopeType, scopeCd, weekStdMinutes);
    }

    // ===================== 내부 =====================

    /** 빈 문자열/공백만 있는 값을 null 로 정규화한다(사업장 미지정 = 회사 스코프). */
    private String trimToNull(String value) {

        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /** 기준일 미지정 시 DB NOW 기준 오늘로 대체한다(JVM 시계 스큐 방지). */
    private String resolveBaseYmd(String baseYmd) {

        if (StringUtils.hasText(baseYmd)) {
            if (DateTimeUtils.parseYyyymmdd(baseYmd) == null) {
                throw new ApiException(StdWorkErrorCode.STDWORK_400_002);
            }
            return baseYmd;
        }
        return stdWorkHoursMapper.selectTodayYmd();
    }

    /** 빈 문자열 종료일을 null(무기한)로 정규화한다. */
    private StdWorkHoursSaveCommand normalize(StdWorkHoursSaveCommand command) {

        if (command == null) {
            throw new ApiException(StdWorkErrorCode.STDWORK_400_001);
        }
        if (command.getApplyEndDate() != null && !StringUtils.hasText(command.getApplyEndDate())) {
            return StdWorkHoursSaveCommand.builder()
                    .cmpnyCd(command.getCmpnyCd())
                    .userCd(command.getUserCd())
                    .applyStrDate(command.getApplyStrDate())
                    .applyEndDate(null)
                    .weekStdMinutes(command.getWeekStdMinutes())
                    .reasonCd(command.getReasonCd())
                    .reasonDetail(command.getReasonDetail())
                    .actorNo(command.getActorNo())
                    .build();
        }
        return command;
    }

    /**
     * 등록/정정 공통 검증.
     *
     * <p>차단 사유는 {@link ApiException} 으로 던지고, 경고는 문구 목록으로 반환한다
     * (plan §8 Q4: 15시간 미만·육아기 범위 밖은 저장 허용).
     *
     * @return 경고 문구 목록 (없으면 빈 목록)
     */
    private List<String> validateCommon(StdWorkHoursSaveCommand cmd) {

        // 1) 필수값.
        if (!StringUtils.hasText(cmd.getCmpnyCd())
                || !StringUtils.hasText(cmd.getUserCd())
                || !StringUtils.hasText(cmd.getApplyStrDate())
                || !StringUtils.hasText(cmd.getReasonCd())
                || !StringUtils.hasText(cmd.getActorNo())
                || cmd.getWeekStdMinutes() == null) {
            throw new ApiException(StdWorkErrorCode.STDWORK_400_001);
        }

        // 2) 날짜 형식·전후 관계.
        if (DateTimeUtils.parseYyyymmdd(cmd.getApplyStrDate()) == null) {
            throw new ApiException(StdWorkErrorCode.STDWORK_400_002);
        }
        String endDate = cmd.getApplyEndDate();
        if (endDate != null) {
            if (DateTimeUtils.parseYyyymmdd(endDate) == null) {
                throw new ApiException(StdWorkErrorCode.STDWORK_400_002);
            }
            if (endDate.compareTo(cmd.getApplyStrDate()) < 0) {
                throw new ApiException(StdWorkErrorCode.STDWORK_400_003);
            }
        }

        // 3) 값 범위 (정책이 아니라 오입력 방어선).
        int weekMinutes = cmd.getWeekStdMinutes();
        if (weekMinutes <= 0 || weekMinutes > MAX_WEEK_MINUTES) {
            throw new ApiException(StdWorkErrorCode.STDWORK_400_004);
        }

        // 4) 사유코드는 SYS083 에 등록된 사용중 코드만 허용.
        StdWorkReasonRuleVO rule = stdWorkHoursMapper.selectReasonRule(cmd.getReasonCd());
        if (rule == null) {
            throw new ApiException(StdWorkErrorCode.STDWORK_400_005);
        }

        // 5) 단축 사유는 반드시 유한 기간 — "단축 전 모수 = 직전 행" 구조의 전제(plan §1.1).
        if (StdWorkReasonCd.isReduced(cmd.getReasonCd()) && endDate == null) {
            throw new ApiException(StdWorkErrorCode.STDWORK_400_006);
        }

        // 6) 계정 존재(USE_YN='Y') + 일용직 제외 게이트.
        //    탈퇴·사용중지 계정은 매퍼의 USE_YN 술어로 걸러져 null 이 되고 404 로 차단된다.
        //    고용형태 미지정 계정은 빈 문자열이 오므로(매퍼 COALESCE) 404 가 아니라 통과한다.
        StdWorkUserScopeVO userScope = stdWorkHoursMapper.selectUserScope(cmd.getCmpnyCd(), cmd.getUserCd());
        if (userScope == null) {
            throw new ApiException(StdWorkErrorCode.STDWORK_404_001);
        }
        if (AuthRoleUtils.isDailyWorker(userScope.getEmploymentType())) {
            log.info("[stdWork] 일용직 소정근로시간 등록 차단: cmpnyCd={}, userCd={}", cmd.getCmpnyCd(), cmd.getUserCd());
            throw new ApiException(StdWorkErrorCode.STDWORK_403_001);
        }

        // 7) 경고(저장 허용).
        List<String> warnings = new ArrayList<>();
        if (weekMinutes < MIN_WARN_WEEK_MINUTES) {
            warnings.add("주 소정근로시간이 15시간 미만입니다. 초단시간근로자는 연차·주휴 적용 대상에서 제외될 수 있으니 계약 내용을 확인해 주세요.");
        }
        if (StdWorkReasonCd.isChildcare(cmd.getReasonCd())
                && (weekMinutes < CHILDCARE_MIN_WEEK_MINUTES || weekMinutes > CHILDCARE_MAX_WEEK_MINUTES)) {
            warnings.add("육아기 근로시간 단축은 주 15시간 이상 35시간 이하가 원칙입니다. 입력값이 범위를 벗어났습니다.");
        }
        return warnings;
    }

    /**
     * 기간 겹침 검증. 직전 열린 행(자동 마감 대상)은 매퍼에서 이미 제외된다.
     *
     * @param excludeStrDate  자기 자신 행 제외용 적용 시작일 (등록 시 null)
     * @param excludeStrDate2 함께 이동될 복귀 행 제외용 적용 시작일 (해당 없으면 null)
     */
    private void assertNoOverlap(String cmpnyCd, String userCd, String applyStrDate, String applyEndDate,
                                 String excludeStrDate, String excludeStrDate2) {

        int overlap = stdWorkHoursMapper.countOverlap(
                cmpnyCd, userCd, applyStrDate, applyEndDate, excludeStrDate, excludeStrDate2);
        if (overlap > 0) {
            log.info("[stdWork] 소정근로시간 기간 겹침 차단: cmpnyCd={}, userCd={}, 적용={}~{}, 겹침={}건",
                    cmpnyCd, userCd, applyStrDate,
                    applyEndDate == null ? "무기한" : applyEndDate, overlap);
            throw new ApiException(StdWorkErrorCode.STDWORK_409_002);
        }
    }
}
