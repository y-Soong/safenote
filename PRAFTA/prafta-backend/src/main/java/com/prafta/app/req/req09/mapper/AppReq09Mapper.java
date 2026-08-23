package com.prafta.app.req.req09.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * prafta-app-009: 근태 요청 결재 분기/라인 전용 Mapper.
 *
 * <p>결재라인 INSERT/조회는 공용 {@code ApprovalLineMapper}, 노드 관리자/신청자명/outbox 는 연차
 * 공용 매퍼를 재사용한다. 본 매퍼는 근태 'Y'/'N' 분기 판정에 필요한 신규 조회 + 모듈 독립을 위한
 * 결재자 스코프/자기승인/요청상태 갱신 SQL 만 보유한다(연차 매퍼 직접 의존을 피해 모듈 결합 최소화).
 *
 * <p>모든 조회/갱신은 CMPNY_CD(+SITE_CD) 스코프로 격리한다. 식별값은 JWT 도출 Param 만 사용(IDOR).
 */
@Mapper
public interface AppReq09Mapper {

    /**
     * D2/D3/D4 분기용 — 신청자 소속 노드(tb_user.NODE_CD)의 SELF_ATTD_APPRV_YN 단순 조회.
     *
     * <p>⚠️ 연차 {@code selectUserNodeSelfApproveYn}('본인이 노드 관리자 + 자체승인 ON' 일 때만 'Y')와
     * 의미가 다르다. 본 조회는 "신청자가 속한 노드가 자체근태승인 부서인가" 만 판정한다(관리자 여부 무관).
     * 노드 미존재/미배정이면 null → 호출부에서 'N' 취급(결재라인 다단계).
     *
     * @param cmpnyCd 회사 코드
     * @param siteCd  사업장 코드
     * @param userCd  신청자 사용자 코드(→ NODE_CD 조인)
     * @return 'Y' / 'N' / null(노드 미존재)
     */
    String selectAttdSelfApprvYn(@Param("cmpnyCd") String cmpnyCd,
                                 @Param("siteCd") String siteCd,
                                 @Param("userCd") String userCd);

    /**
     * D4 분기용 — 신청자가 자기 소속 노드의 정/부 관리자(MAIN_ADMIN_CD / SUB_ADMIN_CD)인지 여부.
     * 'Y' 부서이면서 본인이 그 노드 관리자면 즉시 자동승인(D4).
     *
     * @return 1 이상이면 노드 관리자, 0 이면 일반 근로자.
     */
    int selectIsNodeAdmin(@Param("cmpnyCd") String cmpnyCd,
                          @Param("siteCd") String siteCd,
                          @Param("userCd") String userCd);

    /**
     * D8 결재자 스코프 가드 — 목록 중 유효(동일 회사/사업장·재직·활성·system 아님) 사용자 수.
     * 연차 {@code AppLeaveFlowMapper.countValidApprovers} 와 동일 SQL(모듈 독립 위해 복제).
     * 호출부가 중복제거 수와 비교하여 cross-tenant 결재자 주입을 차단한다.
     */
    int countValidApprovers(@Param("cmpnyCd") String cmpnyCd,
                            @Param("siteCd") String siteCd,
                            @Param("userCds") List<String> userCds);

    /**
     * D7 자기승인 자격 — 신청자 소속 노드 SELF_ATTD_APPRV_YN='Y' 이면서 본인이 그 노드 정/부 관리자일 때
     * 'Y'. 연차 {@code AppLeaveFlowMapper.selectUserNodeSelfApproveYn} 와 동일 의미·SQL(모듈 독립 복제).
     * 자격 미달이면 행 없음(null=fail-closed).
     */
    String selectUserNodeSelfApproveYn(@Param("cmpnyCd") String cmpnyCd,
                                       @Param("userCd") String userCd);

    /**
     * 'Y'+노드관리자 즉시 자동승인(D4) / 'N' 전 단계 본인 자동승인 시 REQ_STATUS 갱신.
     * tb_user_attd_req.REQ_STATUS 와 처리자/처리시각만 갱신한다.
     */
    int updateReqStatus(@Param("cmpnyCd") String cmpnyCd,
                        @Param("reqId") String reqId,
                        @Param("reqStatus") String reqStatus,
                        @Param("processUserCd") String processUserCd,
                        @Param("processComment") String processComment);

    /**
     * 근태결재선통합 P1-2(§0-5 3번째 폴백): approverUserCds/presetId 둘 다 없을 때 신청자 소속 노드의
     * 기본 결재자(정 관리자 MAIN_ADMIN_CD 우선, 없으면 부 관리자 SUB_ADMIN_CD).
     *
     * <p>공용 {@code ApprovalLineMapper.selectDefaultApproverOfNode} 와 동일 형식(MAIN→SUB) —
     * 모듈 독립을 위해 복제한다(D7/D8 스코프 가드 재사용 매퍼들과 동일 관례). 신청자 소속 노드는
     * 여기서 TB_USER 조인으로 직접 해석한다(applyApprovalFlow 가 별도 nodeCd 파라미터를 받지 않음).
     */
    String selectDefaultApproverForUser(@Param("cmpnyCd") String cmpnyCd,
                                        @Param("siteCd") String siteCd,
                                        @Param("userCd") String userCd);
}
