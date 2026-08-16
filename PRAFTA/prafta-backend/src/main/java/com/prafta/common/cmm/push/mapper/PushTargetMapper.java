package com.prafta.common.cmm.push.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.push.result.TbmPushTargetRow;

/**
 * PUSH 생산자(트리거/스케줄러)의 수신 대상 산출 전용 공용 Mapper (PRAFTA-APP-021).
 *
 * <p>특정 도메인 모듈에 종속되지 않는 "PUSH 대상" 조회만 모은다.
 * outbox 채번/INSERT 는 {@code LeaveDashboardMapper} 를 재사용하므로 본 매퍼는 미보유.
 */
@Mapper
public interface PushTargetMapper {

    /**
     * TBM 세션의 <b>실제 입실(enter)한 참석자</b> 목록 (W3, PRAFTA-APP-021-3b + SUBCON-T5 F6).
     *
     * <p>대상 = TB_TBM_ATTENDANCE 의 해당 세션 행 중 ENTRY_AT IS NOT NULL(입실 완료), DEL_YN='N'.
     * REGULAR/DAILY 무관 전부 포함한다("참석 예정 대상자" 개념 없음 — 입실자만, §8-R 2).
     *
     * <p><b>T5 F6</b>: 참석자 회사 조건을 제거해 <b>타사(지정 체인) 참석자에게도</b> 교육 시작/종료
     * 푸시가 가도록 한다. 대신 세션이 {@code hostCmpnyCd}(개설사) 소유임을 SQL 안에서 검증한다
     * (소유 검증 없이 스코프만 넓히면 타사 세션 참석자 명단이 새어나간다).
     * 반환 행은 (참석자 회사코드, USER_CD) 쌍이다 — USER_CD 는 회사별 채번이라 회사코드 동반 필수.
     *
     * @param hostCmpnyCd 세션 개설사 회사코드(소유 검증)
     * @param sessionCd   TBM 세션 코드
     */
    List<TbmPushTargetRow> selectTbmEnteredTargets(@Param("hostCmpnyCd") String hostCmpnyCd,
                                                   @Param("sessionCd") String sessionCd);

    /**
     * 특정 노드 <b>및 그 조상 노드</b>의 정/부 관리자 USER_CD 목록 (셀프가입 승인 대기 통보 M6).
     *
     * <p>★{@code AttdCloseMapper.countNodeAdmin} 의 조상 재귀 CTE 를 <b>역방향</b>으로 뒤집은
     * 쿼리다. 즉 "이 사람이 이 노드를 관리하는가"(판정)를 "이 노드를 관리하는 사람은 누구인가"(열거)로
     * 바꾼 것이며, <b>통과 조건이 되는 술어는 완전히 동일</b>하다. 그래서
     * "수신자 집합 ⊆ 조회 권한자 집합" 불변식이 구조적으로 성립한다.
     * 두 쿼리 중 한쪽의 술어만 바꾸면 "알림은 왔는데 열어보니 403" 이 생긴다 — 반드시 함께 고칠 것.
     *
     * <p>기존 {@code LeaveRefusalMapper.selectSiteRefusalAdmins} 는 <b>대상자 노드와 정확히 일치하는
     * 노드</b>의 관리자만 뽑아 상위(조상) 부서를 포함하지 않으므로 본 용도에 쓸 수 없다.
     *
     * <p>재직·활성({@code USE_YN='Y' AND ACCOUNT_STATUS='01'})만 반환하므로, 승인 대기 상태인
     * 신청자 본인이나 과거 관리자였던 탈퇴 계정은 구조적으로 배제된다.
     *
     * @param cmpnyCd 회사 코드
     * @param siteCd  사업장 코드
     * @param nodeCd  기준 노드(신청자 소속 부서). null/공백이면 빈 목록
     */
    List<String> selectNodeAdminChainUserCds(@Param("cmpnyCd") String cmpnyCd,
                                             @Param("siteCd") String siteCd,
                                             @Param("nodeCd") String nodeCd);

    /**
     * 해당 사업장 접근 권한을 가진 특정 역할(master/hr 등)의 USER_CD 목록.
     *
     * <p>셀프가입 승인 대기 통보의 <b>폴백 전용</b>이다 — 신청자 부서와 조상 전체에 정/부 관리자가
     * 0명일 때만 쓴다. 관리자가 1명이라도 있으면 호출하지 않는다(전사 발송 방지).
     *
     * <p>사업장 원장({@code TB_USER_SITE_AUTH}) 조인은
     * {@code LeaveRefusalMapper.selectSiteRefusalAdmins} 의 역할 기반 절과 동일 형태다.
     * 이 조인 덕분에 폴백 수신자도 {@code assertSiteAccess} 를 통과하는 사람만 남는다
     * (수신자 ⊆ 조회 권한자 불변식 유지).
     *
     * @param cmpnyCd    회사 코드
     * @param siteCd     사업장 코드
     * @param authCdList 대상 권한코드 목록(비면 빈 목록)
     */
    List<String> selectSiteRoleAdminUserCds(@Param("cmpnyCd") String cmpnyCd,
                                            @Param("siteCd") String siteCd,
                                            @Param("authCdList") List<String> authCdList);
}
