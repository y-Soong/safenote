package com.prafta.web.subcon.subcon01.service;

import java.util.List;

import com.prafta.web.subcon.subcon01.result.TerminationImpactItem;

/**
 * 관계 해지 시 산하 연동 자동 종결 확장점(마스터 §1-3 해지=독립화 — plan §8 계약).
 *
 * <p>T2(사업장 링크 독립화) / T3(진행중 공유요청 자동취소) / T5(TBM 지정 회수)가
 * 본 인터페이스의 구현체 빈을 등록한다. T1 단독 시점에는 구현체가 없어 no-op 이 자동 성립한다.
 */
public interface RelationTerminationHandler {

    /**
     * 해지 확인 팝업용 영향 요약(예: "사업장 링크 N건 독립화 예정"). 부작용 금지(조회 전용).
     *
     * @param relationId 관계ID
     * @param cmpnyCdA   관계 요청측 회사코드(REQ_CMPNY_CD)
     * @param cmpnyCdB   관계 상대측 회사코드(TGT_CMPNY_CD)
     */
    List<TerminationImpactItem> summarize(long relationId, String cmpnyCdA, String cmpnyCdB);

    /**
     * 해지 확정 직후 동일 트랜잭션 내 호출(마스터 §1-3: 독립화/자동취소/지정회수).
     * 예외 발생 시 해지 전체가 롤백된다(반쪽 해지 방지 — plan §8).
     *
     * @param actionUserCd 해지 행위자 사용자코드(JWT 도출)
     */
    void onTerminated(long relationId, String cmpnyCdA, String cmpnyCdB, String actionUserCd);
}
