package com.prafta.web.attd.attd07.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.attd07.application.command.AttdCloseCommand;
import com.prafta.web.attd.attd07.result.AttdCloseHistResult;
import com.prafta.web.attd.attd07.result.AttdCloseRowResult;
import com.prafta.web.attd.attd07.result.NodeApprovalInfoResult;

/**
 * 근태 마감 전용 Mapper (prafta-019-C / prafta-028 부서 단위 확장).
 *
 * <p>정책서: attd/§13(근태 마감), 재기획서 §3.3(자동마감 금지)·§9.4(마감기준일).
 * 마감 단위 = 회사+사업장+<b>부서(NODE_CD)</b>+마감월(YYYYMM). 전체 사업장 마감은 NODE_CD='*'.
 * 하위부서 포함(INC_SUB_YN='Y') 마감은 노드 트리 하위까지 커버한다.
 */
@Mapper
public interface AttdCloseMapper {

    /** 마감 상태 행(정확 스코프) 조회. 없으면 null. */
    AttdCloseRowResult selectCloseRow(@Param("cmpnyCd") String cmpnyCd,
                                      @Param("siteCd") String siteCd,
                                      @Param("nodeCd") String nodeCd,
                                      @Param("closeYm") String closeYm);

    /** 미결(대기) 요청 건수 — 초과근무(03/04) 제외. 스코프(부서+하위포함) 내로 한정. */
    int countPendingReq(@Param("cmpnyCd") String cmpnyCd,
                        @Param("siteCd") String siteCd,
                        @Param("nodeCd") String nodeCd,
                        @Param("incSubNodeYn") String incSubNodeYn,
                        @Param("closeYm") String closeYm);

    /** 미승인 초과근무 건수 — REQ_TYPE IN ('03','04'). 스코프 내로 한정. */
    int countUnapprovedOt(@Param("cmpnyCd") String cmpnyCd,
                          @Param("siteCd") String siteCd,
                          @Param("nodeCd") String nodeCd,
                          @Param("incSubNodeYn") String incSubNodeYn,
                          @Param("closeYm") String closeYm);

    /** GPS 미확인 건수 — IS_MOCKED='Y'. 스코프 내로 한정. */
    int countGpsUnconfirmed(@Param("cmpnyCd") String cmpnyCd,
                            @Param("siteCd") String siteCd,
                            @Param("nodeCd") String nodeCd,
                            @Param("incSubNodeYn") String incSubNodeYn,
                            @Param("closeYm") String closeYm);

    /** 정확 스코프(회사+사업장+부서+월) 마감(CLOSED) 행 수 — 재마감 방지. */
    int countClosed(@Param("cmpnyCd") String cmpnyCd,
                    @Param("siteCd") String siteCd,
                    @Param("nodeCd") String nodeCd,
                    @Param("closeYm") String closeYm);

    /**
     * 커버리지 마감 수 — 주어진 부서(nodeCd)가 마감으로 "덮이는지" 판정.
     * 덮임 조건: 같은 월에 CLOSED 인 마감행 중 (NODE_CD='*') / (NODE_CD=nodeCd) /
     * (INC_SUB_YN='Y' 인 상위 노드) 가 하나라도 존재.
     * nodeCd='*' 로 주면 전체('*') 마감 존재 여부만 본다.
     */
    int countCovering(@Param("cmpnyCd") String cmpnyCd,
                      @Param("siteCd") String siteCd,
                      @Param("nodeCd") String nodeCd,
                      @Param("closeYm") String closeYm);

    /**
     * 노드 관리자 여부 — userCd 가 nodeCd 또는 그 상위 노드의 정/부 관리자
     * (MAIN_ADMIN_CD / SUB_ADMIN_CD) 인지. 1 이상이면 관리 권한 있음.
     */
    int countNodeAdmin(@Param("cmpnyCd") String cmpnyCd,
                       @Param("siteCd") String siteCd,
                       @Param("nodeCd") String nodeCd,
                       @Param("userCd") String userCd);

    /** 사용자(userCd)의 소속부서(NODE_CD) 조회. 없으면 null. */
    String selectUserNodeCd(@Param("cmpnyCd") String cmpnyCd,
                            @Param("siteCd") String siteCd,
                            @Param("userCd") String userCd);

    /**
     * 노드 1행의 근태 승인 정책 조회 (com-013-06-FU r28).
     *
     * <p>해당 노드의 {@code SELF_ATTD_APPRV_YN}, {@code PARENT_NODE_CD},
     * {@code MAIN_ADMIN_CD}, {@code SUB_ADMIN_CD} 를 반환한다. 노드가 없으면 null.
     * 자기처리/상위 1단계 결재 강제 판정에만 사용한다(재귀 없음).
     */
    NodeApprovalInfoResult selectNodeApprovalInfo(@Param("cmpnyCd") String cmpnyCd,
                                                  @Param("siteCd") String siteCd,
                                                  @Param("nodeCd") String nodeCd);

    /** 마감 upsert (CLOSED 전이). */
    int upsertClose(AttdCloseCommand command);

    /** 마감 해제 (OPEN 전이). 마감 상태일 때만 갱신되어 영향 행수로 검증 가능. */
    int updateUnclose(AttdCloseCommand command);

    /** 마감 이력 ID 채번 (SEQ_KEY = 'ATTD_CLOSE_HIST'). */
    String selectNextCloseHistId(@Param("cmpnyCd") String cmpnyCd);

    /** 마감/해제 이력 INSERT. */
    int insertCloseHist(@Param("histId") String histId,
                        @Param("cmpnyCd") String cmpnyCd,
                        @Param("siteCd") String siteCd,
                        @Param("nodeCd") String nodeCd,
                        @Param("incSubNodeYn") String incSubNodeYn,
                        @Param("closeYm") String closeYm,
                        @Param("actionType") String actionType,
                        @Param("userCd") String userCd,
                        @Param("actionDesc") String actionDesc);

    /** 마감/해제 이력 목록 조회 (스코프 부서 기준, 최신순). */
    List<AttdCloseHistResult> selectCloseHist(@Param("cmpnyCd") String cmpnyCd,
                                              @Param("siteCd") String siteCd,
                                              @Param("nodeCd") String nodeCd,
                                              @Param("closeYm") String closeYm);
}
