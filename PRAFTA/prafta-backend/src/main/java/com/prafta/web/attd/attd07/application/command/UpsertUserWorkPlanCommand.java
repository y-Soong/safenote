package com.prafta.web.attd.attd07.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * Command for {@code Attd07Mapper.upsertUserWorkPlan} (PRAFTA-APP-007).
 *
 * <p>스케줄 수정 요청(REQ_TYPE='10') 승인 시 tb_user_work_plan 의 단일 행
 * (CMPNY_CD, SITE_CD, USER_CD, WORK_YMD) 의 WORK_PLAN_CD 를 요청의 목표 스케줄 코드로
 * upsert 한다. INSERT ... ON DUPLICATE KEY UPDATE 패턴은 Attd_05 의
 * {@code saveUserWorkPlans} 와 동형이다.
 *
 * <p>tb_user_work_plan 에는 WORK_SEQ 컬럼이 없으므로 사용자-일자 스케줄은 단일 칸이다.
 * 따라서 "2구간 부분 유지/제거" 연산은 존재하지 않으며, 그 날 스케줄 코드 한 칸을 통째
 * 교체한다(plan §0-5/§0-6, D1/D2).
 *
 * <p>모든 값은 서버 권위 값으로 구성한다: 키 필드(cmpnyCd/siteCd/userCd/workYmd)는 REQ row
 * 기준, workPlanCd 는 REQ row 의 SCH_CD, 감사 컬럼은 JWT 의 gvUserCd.
 */
public record UpsertUserWorkPlanCommand(
      String gvCmpnyCd
    , String siteCd
    , String userCd
    , String workYmd
    , String workPlanCd
    , String gvUserCd
) {

    /**
     * 서버 권위 값으로 upsert command 를 생성한다.
     *
     * @param gvCmpnyCd  회사 코드 (JWT)
     * @param siteCd     사업장 코드 (REQ row 권위)
     * @param userCd     대상 사용자 코드 (REQ row 권위)
     * @param workYmd    근무 일자 YYYYMMDD (REQ row 권위)
     * @param workPlanCd 목표 스케줄 코드 = REQ row 의 SCH_CD (서버 권위, 클라 미신뢰)
     * @param gvUserCd   처리자 사용자 코드 (JWT)
     */
    public static UpsertUserWorkPlanCommand of(
            String gvCmpnyCd, String siteCd, String userCd,
            String workYmd, String workPlanCd, String gvUserCd) {

        if (gvCmpnyCd == null || gvCmpnyCd.isEmpty()
                || siteCd == null || siteCd.isEmpty()
                || userCd == null || userCd.isEmpty()
                || workYmd == null || workYmd.isEmpty()
                || workPlanCd == null || workPlanCd.isEmpty()) {
            // 키 또는 목표 스케줄 코드 누락 — 데이터 부재/변조로 간주(fail-closed).
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new UpsertUserWorkPlanCommand(
              gvCmpnyCd
            , siteCd
            , userCd
            , workYmd
            , workPlanCd
            , gvUserCd
        );
    }
}
