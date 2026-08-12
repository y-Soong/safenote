package com.prafta.common.cmm.leave.service;

import com.prafta.common.cmm.leave.command.LeavePolicyCommand;
import com.prafta.common.cmm.leave.vo.AnalyzeImpactVO;
import com.prafta.common.cmm.leave.vo.ImpactSummaryVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyHistoryVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;
import com.prafta.common.cmm.leave.vo.PagedResult;

/**
 * 회사 법정 연차 부여 정책(TB_LEAVE_POLICY) + 사용 단위 정책(TB_LEAVE_USAGE_POLICY) 관리 서비스.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.2~§8.5.8
 *
 * <p>설계 원칙:
 * <ul>
 *   <li>회사당 활성 정책 1건만 존재. {@code @Transactional + SELECT ... FOR UPDATE}로 동시성 직렬화.</li>
 *   <li>Cross-axis 검증(§8.5.3 매트릭스)은 본 서비스 내부에서 강제. 위반 시 ApiException.</li>
 *   <li>{@code APPLY_FROM_DATE}는 오늘 이상(과거 소급 금지 - §8.5.8).</li>
 *   <li>권한 가드는 서비스 진입부에서 {@code AuthRoleUtils.isManager(authCd)} 검증 (§8.5.7).</li>
 *   <li>모든 변경은 TB_LEAVE_POLICY_HISTORY에 PREV/NEW snapshot + impact summary JSON 보존.</li>
 *   <li>본 서비스는 채널 비종속(channel-agnostic). baim07 Controller가 1차 호출처이지만,
 *       추후 회사 생성 프로시저/배치 등에서도 동일하게 호출 가능해야 한다.</li>
 * </ul>
 */
public interface LeavePolicyService {

    /**
     * 회사의 현재 활성 정책 조회 (조회 전용, 락 없음).
     *
     * @return 활성 정책 없으면 {@code null}
     */
    LeavePolicyVO findActivePolicy(String cmpnyCd);

    /**
     * 소정-05: 회사의 <b>법정 연차 자동 부여</b> 사용 여부 (5인 미만 사업장 토글).
     *
     * <p>지시서 {@code 작업지시서_근로자별-소정근로시간-관리-도입.md} §연차 부여 on/off 토글.
     *
     * <p>false 인 회사에서 중지되는 것은 <b>신규 법정 자동 부여</b> 뿐이다.
     * <ul>
     *   <li>중지: 부여 엔진(정기부여 배치·Attd_09 정책 기준 부여)·사용촉진 도래 판정·가불(선차감) 부여</li>
     *   <li>유지: 관리자 수동(약정) 부여, 이미 부여된 연차의 잔여·사용(몰수 아님)</li>
     * </ul>
     *
     * <p><b>★기본값 가드</b>: 활성 정책이 없거나 값이 NULL/공백이면 <b>true</b>(기존 동작)를 반환한다.
     * 컬럼에 'N' 이 명시된 회사만 게이트에 걸린다.
     *
     * @return 자동 부여 사용 여부 (기본 true)
     */
    boolean isStatutoryAutoGrantEnabled(String cmpnyCd);

    /**
     * 소정-05: 이미 조회해 둔 정책 VO 로 판정 (조회 중복 방지용 오버로딩).
     *
     * @param policy 활성 정책 VO. {@code null} 이면 true(기존 동작)
     */
    boolean isStatutoryAutoGrantEnabled(LeavePolicyVO policy);

    /**
     * 정책 생성.
     *
     * <p>처리 순서:
     * <ol>
     *   <li>권한 검증 (AUTH_MASTER OR AUTH_HR_MANAGER)</li>
     *   <li>{@code SELECT ... FOR UPDATE}로 회사 행 락</li>
     *   <li>Cross-axis 검증 (§8.5.3) + APPLY_FROM_DATE 검증 (오늘 이상)</li>
     *   <li>기존 활성 정책 있으면 USE_YN='N' UPDATE</li>
     *   <li>신규 정책 INSERT (USE_YN='Y') → POLICY_SEQ 회수</li>
     *   <li>TB_LEAVE_USAGE_POLICY 1:1 INSERT</li>
     *   <li>TB_LEAVE_POLICY_HISTORY INSERT (PREV/NEW snapshot + IMPACT_SUMMARY)</li>
     * </ol>
     *
     * @return 생성된 정책의 POLICY_SEQ
     */
    Long createPolicy(String cmpnyCd, LeavePolicyCommand command, String authCd, String userCd);

    /**
     * 정책 변경. {@code policySeq}는 변경 대상의 식별자(서버 측에서 활성 정책과의 일치 여부를 검증).
     *
     * <p>처리 순서는 {@link #createPolicy}와 동일하며, 5단계의 "신규 INSERT"가 정책 변경 의미를 가진다.
     *
     * @return 새로 만들어진 정책의 POLICY_SEQ
     */
    Long updatePolicy(String cmpnyCd, Long policySeq, LeavePolicyCommand command, String authCd, String userCd);

    /**
     * 정책 변경 이력 페이징 조회.
     *
     * <p>정책서 §8.5.7: 정책 변경 권한자(AUTH_MASTER OR AUTH_HR_MANAGER)만 허용한다.
     * 이력에는 변경자 실명(평문) 등 민감정보가 포함되므로 진입부에서 권한을 강제한다.
     *
     * @param cmpnyCd 회사 코드 (CMPNY_CD 격리, JWT 도출)
     * @param authCd  요청자 권한 코드 (JWT 도출, ensureManager 판정)
     * @param page    1-based 페이지 번호
     * @param size    페이지 크기 (1~100)
     */
    PagedResult<LeavePolicyHistoryVO> findHistory(String cmpnyCd, String authCd, int page, int size);

    /**
     * 영향 분석만 수행 (저장 X, 화면 미리보기용).
     *
     * <p>현 활성 정책과 {@code command}의 axis 차이를 계산하여 IMPACT_SUMMARY JSON 형태의 결과를 반환.
     *
     * <p>본 메서드는 변경을 저장하지 않으므로 권한 가드 또한 Read 권한 수준으로 완화 가능하나,
     * 정책서 §8.5.7에 따라 정책 변경 권한자(AUTH_MASTER OR AUTH_HR_MANAGER)로 제한한다.
     */
    ImpactSummaryVO previewImpact(String cmpnyCd, LeavePolicyCommand command, String authCd);

    /**
     * 정책 변경 영향 분석 (화면 8, 읽기 전용 시뮬레이션 — 저장 X).
     *
     * <p>정책서: {@code .claude/requests/ref/prafta-017/CLAUDE_CODE_INSTRUCTIONS.md} §9.5 / §9.6 / §9.8.
     *
     * <p>처리:
     * <ol>
     *   <li>권한 검증 (AUTH_MASTER OR AUTH_HR_MANAGER, §8.5.7)</li>
     *   <li>{@code command}의 axis 매트릭스 재검증 (저장 아님 — APPLY_FROM_DATE 과거 검증 제외)</li>
     *   <li>현재 활성 정책 조회. 변경 사항이 전혀 없으면 거부(§9.10-4)</li>
     *   <li>axis별 diff 산출 (Baim_07 UI 순서)</li>
     *   <li>활성 직원 1년치 부여 근사 시뮬레이션 → 영향받는 직원/요약 산출</li>
     * </ol>
     *
     * <p>본 시뮬레이션은 정밀 부여엔진(법정 연차 자동 부여 배치, §10) 미구현 상태의 <b>근사치</b>다.
     *
     * @return 요약/diff/영향 직원 묶음
     */
    AnalyzeImpactVO analyzeImpact(String cmpnyCd, LeavePolicyCommand command, String authCd);
}
