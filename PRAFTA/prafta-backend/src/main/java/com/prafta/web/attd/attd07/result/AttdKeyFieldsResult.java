package com.prafta.web.attd.attd07.result;

/**
 * 관리자 직접 "수정"(attdId 보유) 기존 행 대조 게이트용 — 근태(TB_USER_ATTD_MGMT) 키 필드 최소 스냅샷.
 *
 * <p>{@code Attd07Mapper.selectAttdKeyFieldsById} 결과. {@code updateUserAttdInfos} 매퍼는
 *   upsert(ON DUPLICATE KEY UPDATE)라 편집 시 WORK_YMD/WORK_SEQ/SITE_CD/USER_CD 를
 *   갱신하지 않는다(불변 전제). 본문이 이 필드들에 기존 행과 다른 값을 보내면 조용한 병합이 일어나고
 *   마감·겹침 검사가 본문 값 기준으로 돌아 실제 저장 행과 어긋나므로, 저장 전에 본 스냅샷과 대조해
 *   불일치를 차단한다(승인 경로 body/REQ 대조와 동일 원칙).
 *   NODE_CD 는 대조 대상이 아니라 조회하지 않는다 — 행의 NODE_CD 는 생성 시점 스냅샷이고 본문
 *   nodeCd 는 별도 게이트가 현재 소속 권위 노드와 대조하므로, 둘 다 요구하면 부서 이동자의 과거
 *   근태 편집이 불가능해진다(데드락, qa 2026-08-10 판정).
 *
 * <p><b>★ record 컴포넌트 순서 = SELECT 컬럼 순서</b>(MyBatis record 생성자 매핑은 컬럼 순서를 따른다).
 *   {@code Attd07Mapper.xml} 의 SELECT 순서(workYmd, workSeq, siteCd, userCd)와 반드시
 *   함께 유지할 것 — 불일치 시 무증상 오매핑이 발생한다.
 */
public record AttdKeyFieldsResult(
      String workYmd
    , String workSeq
    , String siteCd
    , String userCd
) {
}
