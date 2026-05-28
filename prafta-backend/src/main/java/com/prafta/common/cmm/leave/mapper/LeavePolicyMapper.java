package com.prafta.common.cmm.leave.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.leave.vo.AffectedEmployeeBaseVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyHistoryVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;

/**
 * TB_LEAVE_POLICY / TB_LEAVE_USAGE_POLICY / TB_LEAVE_POLICY_HISTORY 전용 Mapper.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5
 */
@Mapper
public interface LeavePolicyMapper {

    /**
     * 활성 정책 단건 조회 (락 없음, 조회/미리보기용).
     * 활성 정책이 없으면 {@code null}.
     */
    LeavePolicyVO selectActivePolicy(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 활성 정책 단건 조회 + 회사 행 락 (SELECT ... FOR UPDATE).
     *
     * <p>정책 생성/변경 트랜잭션 진입부에서 호출. 직렬화 보장.
     * 활성 정책이 없으면 {@code null} (락은 행이 없으면 잡지 않음).
     */
    LeavePolicyVO selectActivePolicyForUpdate(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 기존 활성 정책을 비활성화 (USE_YN='N').
     * {@code policySeq} 기준으로 단건 UPDATE.
     *
     * @return UPDATE된 행 수
     */
    int deactivatePolicy(@Param("cmpnyCd") String cmpnyCd,
                         @Param("policySeq") Long policySeq,
                         @Param("updateBy") String updateBy);

    /**
     * 신규 정책 INSERT. AUTO_INCREMENT POLICY_SEQ를 회수한다.
     *
     * <p>MyBatis가 {@code useGeneratedKeys=true, keyProperty="policySeq"}로 POLICY_SEQ를
     * VO에 다시 채워준다.
     */
    int insertPolicy(LeavePolicyVO vo);

    /**
     * TB_LEAVE_USAGE_POLICY 1:1 INSERT.
     * POLICY_SEQ는 직전 INSERT된 정책의 PK를 사용.
     */
    int insertUsagePolicy(LeavePolicyVO vo);

    /**
     * 정책 변경 이력 INSERT (PREV/NEW snapshot + impact summary JSON 포함).
     * HIST_ID는 호출 측에서 채번하여 전달한다.
     */
    int insertPolicyHistory(LeavePolicyHistoryVO vo);

    /**
     * HIST_ID 채번 (FNC_CMM_SEQ_NEXTVAL 호출).
     * SEQ_KEY = 'LEAVE_POLICY_HIST'.
     */
    String selectNextHistId(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 정책 변경 이력 페이징 조회.
     *
     * <p>응답에 PREV_SNAPSHOT / NEW_SNAPSHOT / IMPACT_SUMMARY JSON 컬럼을 raw 문자열로 포함.
     */
    List<LeavePolicyHistoryVO> selectPolicyHistory(@Param("cmpnyCd") String cmpnyCd,
                                                   @Param("offset") int offset,
                                                   @Param("limit") int limit);

    /**
     * 정책 변경 이력 전체 행 수 (페이징 메타 계산용).
     */
    long countPolicyHistory(@Param("cmpnyCd") String cmpnyCd);

    /**
     * IMPACT_SUMMARY 계산 보조: 본 회사 활성 사용자 수 집계.
     *
     * <p>활성 사용자 정의: USE_YN='Y' AND WITHDRAWAL_DATE IS NULL AND ACCOUNT_STATUS='01'.
     * (정확한 활성 사용자 기준은 PRAFTA에서 별도 명시되어 있지 않으므로 안전한 보수적 기준 적용)
     */
    int countActiveUsers(@Param("cmpnyCd") String cmpnyCd);

    /**
     * MAX(POLICY_SEQ) 조회 (테스트/디버그용, 현재는 미사용).
     */
    Long selectMaxPolicySeq(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 정책 변경 영향 분석(화면 8)용 활성 직원 목록 조회.
     *
     * <p>정책서: {@code .claude/requests/ref/prafta-017/CLAUDE_CODE_INSTRUCTIONS.md} §9.8 (활성 직원 전체 조회)
     *
     * <p>활성 사용자 정의는 {@link #countActiveUsers}와 동일:
     * {@code USE_YN='Y' AND WITHDRAWAL_DATE IS NULL AND ACCOUNT_STATUS='01'}.
     * 부서명은 TB_SITE_NODE(CMPNY_CD,SITE_CD,NODE_CD) LEFT JOIN, 기존 부여/사용은
     * TB_USER_LEAVE_GRANT 활성 행(DEL_YN='N' AND STATUS='ACTIVE') 합계를 LEFT JOIN 집계한다.
     * 대량 사업장 대비 {@code LIMIT} 가드를 부착한다(최대 {@code limit}건).
     *
     * @param cmpnyCd 회사 코드 (CMPNY_CD 격리)
     * @param limit   조회 상한 (대량 가드, 예: 500)
     */
    List<AffectedEmployeeBaseVO> selectActiveUsersForImpact(@Param("cmpnyCd") String cmpnyCd,
                                                            @Param("limit") int limit);
}
