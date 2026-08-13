package com.prafta.common.cmm.stdwork.service;

import java.util.List;

import com.prafta.common.cmm.stdwork.command.StdWorkHoursSaveCommand;
import com.prafta.common.cmm.stdwork.vo.StdWorkHoursSaveResult;
import com.prafta.common.cmm.stdwork.vo.StdWorkHoursSummaryVO;
import com.prafta.common.cmm.stdwork.vo.StdWorkHoursVO;
import com.prafta.common.cmm.stdwork.vo.StdWorkReasonRuleVO;

/**
 * 소정-02: 근로자별 소정근로시간(계약량) 공용 서비스.
 *
 * <p>지시서 {@code 작업지시서_근로자별-소정근로시간-관리-도입.md} §0단계 / plan §4 소정-02.
 *
 * <p><b>역할</b> — 근로자별 "주 소정근로 분"의 시점별 해석과 등록/변경 검증을 단일 출처로 제공한다.
 * 연차 비례부여 분모(2단계), 정산 버킷 분모(3단계), 단시간 파생 판정, OT 게이트(소정-07)가
 * 전부 이 서비스를 경유한다.
 *
 * <p><b>★0단계 경계</b>: 본 서비스는 <b>데이터 축</b>만 담당한다. 연차 판정·부여 로직에
 * 아직 연결하지 않는다(지시서 §0단계 마지막 항목).
 *
 * <p><b>폴백 체인</b> (지시서 확정 — 미입력 계정 = 통상근로자 간주)
 * <ol>
 *   <li>TB_USER_STD_WORK_HOURS 의 기준일 유효 행</li>
 *   <li>없으면 TB_CMPNY_STD_WORK_POLICY 의 <b>소속 사업장 오버라이드</b>(SITE 스코프)</li>
 *   <li>없으면 같은 테이블의 회사 통상 기준값(COMPANY 스코프)</li>
 *   <li>그것도 없으면 코드 상수 {@link #DEFAULT_WEEK_STD_MINUTES}(2400분 = 주 40시간)</li>
 * </ol>
 *
 * <p><b>★사업장 오버라이드 (작업지시서_통상근로시간-회사-사업장별-설정)</b> — 통상근로시간이
 * 주 40시간이 아닌 사업장(예: 교대제 공장 주 35시간)에서 단시간 판정 분모가 회사 기본값으로
 * 고정되면 그 사업장의 <b>통상근로자가 단시간근로자로 오분류</b>된다. 판정에 쓰는 사업장은
 * 언제나 <b>대상 근로자의 소속 사업장</b>({@code TB_USER.SITE_CD})이며, 오버라이드 행이 없는
 * 사업장은 회사 기본값으로 폴백되어 종전과 결과가 100% 동일하다(무회귀 보장).
 *
 * <p><b>일용직 제외 (★소비처 주의)</b>
 * <ul>
 *   <li>쓰기 경로({@link #register}/{@link #correct}/{@link #validateForWarning}): 계정이 없거나
 *       탈퇴·사용중지(USE_YN='N')·일용직이면 차단한다(fail-closed).</li>
 *   <li>조회 경로: <b>대상 여부와 무관하게 폴백 값을 반환한다.</b> 즉 일용직 계정도
 *       {@link #resolveWeekStdMinutes} 로 2400분(또는 회사 기준값)을 받는다 — 소정근로 개념이
 *       없는 계정에 의미 없는 값이므로, 연차 부여·정산·단시간 판정 소비처는
 *       {@code StdWorkHoursSummaryVO.eligible}(또는 {@link #isEligible})로 먼저 걸러야 한다.
 *       조회를 예외로 막지 않는 이유는 배치 루프(부여 엔진 등)가 일부 계정 때문에 통째로
 *       실패하는 것을 피하기 위함이다.</li>
 * </ul>
 *
 * <p><b>단축 종료 후 복귀 (★H-1 확정 규칙)</b> — 유한 기간(단축 사유) 행을 등록하면서 직전
 * 열린 행을 마감한 경우, 단축 종료 다음 날부터 이력 공백이 생겨 폴백(통상 간주)으로 승격되는
 * 것을 막기 위해 <b>복귀 행을 자동 생성</b>한다. 상세 규칙은 {@link #register} 참조.
 */
public interface StdWorkHoursService {

    /** 회사 기준값도 없을 때 쓰는 시스템 폴백 (2400분 = 주 40시간, 지시서 B-1). */
    int DEFAULT_WEEK_STD_MINUTES = 2400;

    /**
     * 통상근로자 주 소정근로시간의 <b>법정 상한</b> (2400분 = 주 40시간).
     *
     * <p>근로기준법 제50조 — 1주 40시간·1일 8시간 초과 금지. 소정근로시간은 그 범위 <i>안에서</i>
     * 정하는 시간이므로 회사·사업장 기준값이 이 값을 넘을 수 없다. "주 44시간 사업장"은
     * 소정 40h + 연장 4h 이며, 연장분은 고정연장근무(근무타입)로 잡는다.
     *
     * <p>★근로자 <b>개인</b> 이력({@code register}/{@code correct})의 상한과는 별개다. 개인 이력은
     * 오입력 방어선(주 168시간)만 두고 값 자체는 제한하지 않는다 — 감시·단속적 근로 승인 등
     * 특례 데이터를 서비스가 임의로 막지 않기 위함.
     */
    int LEGAL_MAX_WEEK_MINUTES = 2400;

    /** 주 15시간(900분) — 미만이면 경고(초단시간 경계, plan §8 Q4: 차단 아님). */
    int MIN_WARN_WEEK_MINUTES = 900;

    /** 육아기 단축 권장 하한 주 15시간(900분). */
    int CHILDCARE_MIN_WEEK_MINUTES = 900;

    /** 육아기 단축 권장 상한 주 35시간(2100분). */
    int CHILDCARE_MAX_WEEK_MINUTES = 2100;

    // ===== 조회 =====

    /**
     * 기준일에 유효한 소정근로 이력 행.
     *
     * @param baseYmd 기준일 (YYYYMMDD)
     * @return 유효 행. 이력 미입력 계정이면 null (폴백 값은 {@link #resolveSummary} 로 확인)
     */
    StdWorkHoursVO findEffectiveRow(String cmpnyCd, String userCd, String baseYmd);

    /** 오늘(DB NOW 기준) 유효한 소정근로 이력 행. 미입력 계정이면 null. */
    StdWorkHoursVO findCurrentRow(String cmpnyCd, String userCd);

    /**
     * 기준일 기준 본인 주 소정근로 분 (폴백 포함 — 항상 양수 반환).
     *
     * <p>후속 작업이 가장 많이 쓰는 최소 진입점.
     */
    int resolveWeekStdMinutes(String cmpnyCd, String userCd, String baseYmd);

    /** 오늘 기준 본인 주 소정근로 분 (폴백 포함). */
    int resolveCurrentWeekStdMinutes(String cmpnyCd, String userCd);

    /**
     * 회사 통상근로자 주 소정근로 분 (COMPANY 스코프만 — 행 부재 시 {@link #DEFAULT_WEEK_STD_MINUTES}).
     *
     * <p>사업장이 특정되지 않는 자리(신규 회사 프로비저닝 등)에서만 쓴다. 대상 근로자의
     * 사업장을 알 수 있으면 {@link #resolveSiteWeekStdMinutes} 를 써야 단시간 오분류가 없다.
     */
    int resolveCmpnyWeekStdMinutes(String cmpnyCd);

    /**
     * 사업장 기준 통상근로자 주 소정근로 분 — <b>사업장 오버라이드 → 회사 기본값 → 2400분</b>.
     *
     * <p>단시간 판정 분모·연차 비례부여 분모·"풀타임" 입력 기본값의 단일 진입점이다.
     *
     * @param siteCd 대상 사업장. null/빈 값이면 회사 기본값만 본다
     *               (= {@link #resolveCmpnyWeekStdMinutes} 와 동일 결과)
     */
    int resolveSiteWeekStdMinutes(String cmpnyCd, String siteCd);

    /**
     * 특정 스코프에 <b>직접 지정된</b> 기준값 (상속 폴백 없음).
     *
     * <p>화면이 "회사 기본값 사용 / 직접 지정"을 구분해 그리기 위한 조회다.
     *
     * @param siteCd null/빈 값이면 회사(COMPANY) 스코프, 값이 있으면 그 사업장(SITE) 스코프
     * @return 직접 지정된 분. 지정이 없으면 null (= 상위 스코프 상속)
     */
    Integer findPolicyWeekStdMinutes(String cmpnyCd, String siteCd);

    /**
     * 기준일 기준 소정근로 요약 — 해석값·회사 기준값·출처·단시간 파생 판정을 함께 반환.
     *
     * <p>화면(UI-C)이 "미입력(통상 기준 40h 간주)" 배지를 그릴 수 있도록 출처를 싣는다.
     *
     * <p><b>★TODO(성능, 2단계 착수 시) — 벌크 조회 API 필요</b>: 본 메서드는 호출당 쿼리 3회
     * (회사 기준값 / 유효 이력 행 / 고용형태)를 쓴다. 단건 화면·게이트에는 문제가 없으나,
     * 2단계 부여 엔진처럼 <b>회사 전 사용자 루프</b>에서 호출하면 3N 왕복이 된다.
     * 소비 시점에 {@code resolveSummaries(cmpnyCd, List&lt;userCd&gt;, baseYmd)} 형태의
     * 벌크 조회(회사 기준값 1회 + IN 절 일괄 조회)를 추가할 것.
     */
    StdWorkHoursSummaryVO resolveSummary(String cmpnyCd, String userCd, String baseYmd);

    /**
     * 단시간근로자 파생 판정 (본인 주 소정 &lt; 회사 통상 기준).
     *
     * <p>지시서 B-2: EMPLOYMENT_TYPE 에 값을 추가하지 않는 <b>파생값</b>이다.
     * 이력 미입력 계정은 통상 기준으로 폴백되므로 항상 false.
     *
     * <p>★일용직 계정도 폴백으로 false 를 받는다(관리 대상 자체가 아님). 대상 여부 판정이
     * 필요하면 {@link #isEligible} 또는 {@code StdWorkHoursSummaryVO.eligible} 을 함께 본다.
     */
    boolean isPartTime(String cmpnyCd, String userCd, String baseYmd);

    /** 소정근로 이력 전체 (적용 시작일 내림차순). */
    List<StdWorkHoursVO> findHistory(String cmpnyCd, String userCd);

    /**
     * 소정근로시간 관리 대상 계정인지 여부 (계정이 없거나 일용직이면 false).
     *
     * <p>목록 화면/EP 가 일용직을 명시적으로 걸러야 할 때 쓴다. 쓰기 경로는 동일 판정을
     * 내부에서 강제하므로 별도 호출이 필요 없다.
     */
    boolean isEligible(String cmpnyCd, String userCd);

    /**
     * SYS083 사유코드의 정책 규칙 1건 (차감 규칙 / 부여 규칙).
     *
     * <p>2단계 차감·부여 분기의 데이터 진입점 — 사유코드 상수 하드코딩 금지(plan §1.4).
     *
     * @return 규칙. 미등록/미사용 코드면 null
     */
    StdWorkReasonRuleVO findReasonRule(String reasonCd);

    /** SYS083 사용중 사유코드 전체 (화면 셀렉트용). */
    List<StdWorkReasonRuleVO> findReasonRules();

    // ===== 등록 / 변경 =====

    /**
     * 소정근로 이력 등록 (변경 = 직전 열린 행 자동 마감 + 신규 행 INSERT).
     *
     * <p>검증(차단): 필수값 / 날짜 형식 / 종료일 &gt;= 시작일 / 값 범위 / 사유코드 존재(SYS083) /
     * 단축 사유 종료일 필수 / 일용직·사용중지 차단 / 계정 존재 / 동일 시작일 중복 / 기간 겹침.
     * <p>검증(경고, 저장 허용): 주 15시간 미만 / 육아기 주 15~35시간 밖.
     *
     * <p><b>★단축 종료 후 복귀 행 자동 생성 (H-1 확정 규칙)</b>
     * <ul>
     *   <li>조건: 신규 행이 <b>유한 기간</b>(종료일 있음)이고, <b>직전 열린 행이 실제로 마감된</b> 경우.</li>
     *   <li>복귀 행: 적용 시작일 = 신규 행 종료일 + 1일, 종료일 = NULL(열린 행).
     *       주 소정근로분 / 사유코드 / 사유 상세는 <b>마감된 직전 행에서 그대로 승계</b>한다.</li>
     *   <li>직전 열린 행이 없어 마감이 발생하지 않았으면 복귀 행도 만들지 않는다 —
     *       원래 이력이 없던 계정은 단축 종료 후 폴백(통상 간주)이 의미상 정합.</li>
     *   <li>복귀 구간이 기존 다른 행과 겹치면(예: 종료일 이후에 이미 등록된 미래 행) 생성하지
     *       않고 경고 1건을 반환한다.</li>
     *   <li>무기한 행(종료일 NULL) 등록은 복귀 행 없음.</li>
     * </ul>
     *
     * @return 저장 결과 + 복귀 행 정보 + 경고 문구 목록
     */
    StdWorkHoursSaveResult register(StdWorkHoursSaveCommand command);

    /**
     * 소정근로 이력 정정 (동일 적용 시작일 행의 값 수정).
     *
     * <p>이력 원칙상 "값 변경"은 {@link #register} 로 새 행을 쌓는 것이 정상 경로이며,
     * 본 메서드는 <b>오입력 정정</b> 전용이다(적용 시작일은 변경 불가).
     *
     * <p><b>★복귀 행 동기화 (확정 규칙)</b> — 유한 기간 행의 종료일을 정정하면 그 뒤에
     * 붙어 있는 복귀 행과 공백/겹침이 생긴다. 이를 막기 위해 다음을 함께 처리한다.
     * <ul>
     *   <li>인접 판정: 적용 시작일이 <b>정정 전 종료일 + 1일</b> 인 후속 행이 있으면 복귀 행으로
     *       간주한다(자동/수동 구분 컬럼이 없으므로 인접성으로 판정).</li>
     *   <li>이동: 그 행의 적용 시작일을 <b>새 종료일 + 1일</b> 로 옮긴다. PK 에 적용 시작일이
     *       포함되므로 DELETE + INSERT 로 처리하며, 나머지 컬럼은 전량 승계한다.</li>
     *   <li>차단: 이동 결과가 또 다른 행과 겹치면(뒤로 미루는데 그 뒤에 행이 있는 경우)
     *       조용히 뭉개지 않고 정정 자체를 겹침 오류로 차단한다.</li>
     *   <li>인접 행이 없으면 종전대로 종료일만 변경한다.</li>
     * </ul>
     * 겹침 검증은 <b>이동 후 최종 배치</b>가 무겹침이어야 통과한다.
     */
    StdWorkHoursSaveResult correct(StdWorkHoursSaveCommand command);

    /**
     * 저장 없이 검증만 수행해 경고 문구를 산출한다 (화면 실시간 안내용).
     *
     * <p>차단 사유는 예외로 던지고, 경고만 목록으로 반환한다.
     */
    List<String> validateForWarning(StdWorkHoursSaveCommand command);

    // ===== 기준값(TB_CMPNY_STD_WORK_POLICY) 등록 / 변경 =====

    /**
     * 통상근로자 주 소정근로 기준값 저장 (회사 기본값 / 사업장 오버라이드 공용).
     *
     * <p><b>검증(차단)</b> — 0 초과, {@link #LEGAL_MAX_WEEK_MINUTES}(주 40시간) 이하.
     * 상한 초과는 근로기준법 제50조 근거로 차단한다.
     * <p><b>경고(저장 허용)</b> — 주 15시간 미만은 오입력 가능성이 높아 로그 경고만 남긴다
     * (회사 기준값이 초단시간인 경우는 현실적으로 없다).
     *
     * <p><b>★미지정 = 행 없음</b> — {@code weekStdMinutes} 가 null 이면 해당 스코프 행을
     * <b>삭제</b>한다. 회사 스코프면 코드 폴백 2400분으로, 사업장 스코프면 회사 기본값으로
     * 되돌아간다. 즉 "0 으로 저장"이 아니라 "상속"이다.
     *
     * @param siteCd         null/빈 값이면 회사(COMPANY) 스코프, 값이 있으면 그 사업장(SITE) 스코프
     * @param weekStdMinutes 주 소정근로 분. null 이면 해당 스코프 지정 해제(상속)
     * @param actorNo        작업자 USER_CD (감사 컬럼)
     */
    void saveWeekStdMinutesPolicy(String cmpnyCd, String siteCd, Integer weekStdMinutes, String actorNo);
}
