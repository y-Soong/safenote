package com.prafta.common.cmm.leave.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 연차 결재 PUSH(차례 도래 / 무결재 사용 통보) 전용 Mapper (PRAFTA-COM-004).
 *
 * <p>web 신청·승인(LeaveFlowServiceImpl)과 app 신청(AppLeaveFlowServiceImpl)이 공용으로
 * 사용한다. 앱/웹 분리 원칙(app 이 web 을 호출하지 않음)에 따라 공용 영역인
 * {@code com.prafta.common.cmm.leave} 에 둔다.
 *
 * <p>outbox INSERT / NOTI_ID 채번은 신규 SQL 난립 방지를 위해
 * {@link LeaveDashboardMapper#insertNotiOutbox} / {@link LeaveDashboardMapper#selectNextNotiId}
 * 를 재사용한다(본 매퍼는 미보유).
 *
 * <p>모든 조회는 CMPNY_CD 스코프로 격리한다.
 */
@Mapper
public interface LeaveApprovalNotiMapper {

    /**
     * 시나리오 B 수신자: 신청자 소속 노드(NODE_CD)의 main/sub 관리자 USER_CD 목록.
     *
     * <p>{@code LeaveRefusalMapper.selectSiteRefusalAdmins} 의 (2)번 노드 서브쿼리만 분리한 것이다.
     * (1)번 역할 UNION(master/hr)은 의도적으로 제외한다(C2: 노드 관리자에게만 통보).
     *
     * <p>대상 = 대상 근로자의 NODE_CD(tb_user) 가 매칭되는 tb_site_node 의
     * MAIN_ADMIN_CD ∪ SUB_ADMIN_CD (해당 회사/사업장). USER_CD 기준 DISTINCT, 활성 계정
     * (USE_YN='Y', ACCOUNT_STATUS='01')만. 빈 관리자 칸은 NULLIF(TRIM())로 제외.
     *
     * <p>신청자가 어떤 노드에도 속하지 않거나 관리자 칸이 비면 빈 리스트(no-op 허용).
     *
     * @param cmpnyCd 회사 코드 (CMPNY_CD 스코프)
     * @param siteCd  사업장 코드 (SITE_CD 스코프)
     * @param userCd  신청자(대상 근로자) 코드 (→ NODE_CD 조인)
     */
    List<String> selectNodeAdmins(@Param("cmpnyCd") String cmpnyCd,
                                  @Param("siteCd") String siteCd,
                                  @Param("userCd") String userCd);

    /**
     * 본문 합성용 신청자 실명(평문 USER_NM) 조회. 활성 계정만, 스코프 밖/미존재면 null.
     *
     * <p>USER_NM 은 평문 varchar 컬럼이다(AES-GCM 암호화 대상 아님). 복호화 호출 금지.
     *
     * @param cmpnyCd 회사 코드 (CMPNY_CD 스코프)
     * @param userCd  대상 사용자 코드
     * @return 평문 사용자명(없으면 null)
     */
    String selectUserNm(@Param("cmpnyCd") String cmpnyCd,
                        @Param("userCd") String userCd);
}
