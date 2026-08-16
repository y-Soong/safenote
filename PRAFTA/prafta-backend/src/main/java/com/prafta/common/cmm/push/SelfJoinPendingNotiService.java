package com.prafta.common.cmm.push;

/**
 * 셀프가입 승인 대기 통보(M6) PUSH 생산자(outbox PENDING 적재).
 *
 * <p>셀프가입 접수(신규 INSERT · 거부 행 재활용 UPDATE 양쪽)가 확정 커밋된 직후 호출된다.
 * afterCommit + REQUIRES_NEW 격리라 푸시 적재 실패가 <b>가입 접수를 롤백시키지 않는다</b>.
 *
 * <p><b>수신 대상</b> = 신청자 소속 노드 + 그 조상 노드의 정/부 관리자(재직·활성만).
 * 조회 게이트({@code AttdCloseService.canManageNodeExcludeSafe})와 <b>같은 술어</b>를 써서
 * "수신자 집합 ⊆ 조회 권한자 집합" 불변식을 구조적으로 보장한다 — "알림은 왔는데 열어보니
 * 권한 없음"이 발생하지 않는다. master/hr 은 전사 발송 금지 원칙상 수신자에 넣지 않는다
 * (권한은 있는데 알림이 없는 반대 방향 불일치는 안전한 불일치라 허용).
 *
 * <p><b>단, 조상 체인 전체에 정/부 관리자가 0명이면</b> 실제 승인 가능자가 master/hr 뿐이므로
 * 그때만 해당 사업장 master/hr 로 폴백한다(관리자가 1명이라도 있으면 폴백하지 않는다).
 *
 * <p><b>발송 빈도 = 수신자 1명당 하루 1건</b>({@code dedupKey} 에 수신자 + 날짜 + 사업장).
 * <b>알려진 한계</b> — 그날 두 번째 이후의 신청은 dedupKey 가 같아 UNIQUE 로 흡수되므로
 * <b>추가 알림이 가지 않는다.</b> 또한 적재 시점 이후 본문을 갱신할 수 없어 메시지에 건수를
 * 넣지 않는다. 정확한 대기 건수는 화면(승인 대기 탭)과 관리자 런처 배지가 실시간으로 보여준다.
 */
public interface SelfJoinPendingNotiService {

    /**
     * 셀프가입 접수 통보. 접수 트랜잭션 커밋 이후 적재한다.
     *
     * @param cmpnyCd         회사 코드
     * @param siteCd          신청 사업장 코드(dedupKey · 라우팅 키)
     * @param nodeCd          신청자 소속 부서 코드(수신자 산출 기준). 비면 수신자 0명
     * @param applicantUserCd 신청자 사용자 코드(적재 INSERT_NO 용). 페이로드·본문에는 넣지 않는다
     */
    void notifyJoinRequested(String cmpnyCd, String siteCd, String nodeCd, String applicantUserCd);

    /** 접수 통보 outbox 적재(REQUIRES_NEW). afterCommit 콜백 전용 — 직접 호출 금지. */
    void runJoinRequestedOutbox(String cmpnyCd, String siteCd, String nodeCd, String applicantUserCd);
}
