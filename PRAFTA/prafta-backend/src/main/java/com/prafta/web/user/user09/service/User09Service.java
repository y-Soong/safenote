package com.prafta.web.user.user09.service;

import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.web.user.user09.application.param.SelfJoinApproveParam;
import com.prafta.web.user.user09.application.param.SelfJoinListParam;
import com.prafta.web.user.user09.application.param.SelfJoinRejectParam;
import com.prafta.web.user.user09.dto.response.SelfJoinListResponse;

/**
 * 소정-09: 셀프가입 승인/거부 서비스 (User_09).
 *
 * <p>지시서 {@code 작업지시서_근로자별-소정근로시간-관리-도입.md} §0단계
 * "셀프가입 승인/거부 화면 신설(08-11 확정)" / plan §2·§3 UI-B·§4 소정-04·09.
 *
 * <p><b>권한 (plan §8 Q1 확정)</b> — master/hr <b>또는 가입 대상 부서(및 상위)의 정·부 관리자</b>.
 * {@code AttdCloseService.canManageNodeExcludeSafe} 로 강제한다. safe(안전관리자)를 전사 통과에서
 * 제외하는 이유는 셀프가입 승인이 <b>인사 행위</b>(계정 활성화 + 입사일·고용형태·소정근로 확정)이기
 * 때문이다 — 안전관리 역할에 인사 승인 권한을 주지 않는다(attd13 연차 변경 동의 관리 전례).
 *
 * <p>★목록 EP 에도 반드시 부서 게이트를 건다. 사업장 인가만으로는 자기 사업장 fast path 때문에
 * 일반 사원이 전 직원의 이름·휴대폰을 열람할 수 있다(feedback_web_new_query_screen_needs_node_gate,
 * 3회 재발 실증).
 */
public interface User09Service {

    /** 셀프가입 신청 목록 ('06' 승인대기 / '07' 가입거부). 휴대폰은 마스킹되어 나간다. */
    SelfJoinListResponse selectSelfJoinList(SelfJoinListParam param);

    /**
     * 셀프가입 승인 — <b>단일 트랜잭션</b>: TB_USER 보강(입사일/고용형태/직급) + 상태 '06'→'01'
     * + 소정근로 이력 INSERT.
     *
     * <p>소정근로 이력 등록이 실패하면 계정 활성화까지 함께 롤백된다 — "소정근로 미기록 활성 계정"이
     * 조용히 생기지 않게 하기 위함(소정-03 계정 생성 경로와 동일 원칙).
     */
    void approveSelfJoin(SelfJoinApproveParam param, AuditContext auditContext);

    /**
     * 셀프가입 거부 — 상태 '06'→'07' + USE_YN='N'(행 보존).
     *
     * <p>거부 사유는 감사 로그로만 남긴다. 동일 아이디/휴대폰 재가입 시 이 행이 재활용되므로
     * (LoginServiceImpl 재가입 판정) tb_user 에 사유를 적으면 과거 이력이 덮어써진다.
     */
    void rejectSelfJoin(SelfJoinRejectParam param, AuditContext auditContext);
}
