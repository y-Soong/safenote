package com.prafta.web.tbm.tbmai02.service;

import com.prafta.web.tbm.tbmai02.application.param.TbmAi02Param;
import com.prafta.web.tbm.tbmai02.dto.response.TbmAiUnconfirmedResponse;
import com.prafta.web.tbm.tbmai02.dto.response.TbmGenerateResponse;

/**
 * TBM AI 교육안 생성 서비스.
 */
public interface TbmAi02Service {

    /**
     * 교육안 생성: 세션(sessionCd)에 묶인 교육자료의 CONFIRMED 확정 서술 + 관리자 교육내용 →
     * HCX 라인프로토콜 4섹션 생성 → 파싱·리치HTML 렌더. DB 미기록(초안 반환) — FE가 세션
     * CONTENT_BODY 에디터에 채운 뒤 기존 세션 저장으로 영속한다.
     *
     * <p>사전 차단: 세션에 AI 분석 지정(AI_ANALYZE_YN='Y')됐지만 미확정인 항목이 하나라도 있으면
     *    TBM_409_060(어떤 항목이 미확정인지 상세 메시지)로 생성을 거부한다.
     */
    TbmGenerateResponse generate(TbmAi02Param param);

    /**
     * 미확정 AI 분석 항목 조회(FE 사전 차단용, 조회 전용).
     *
     * <p>generate 와 동일한 사전 게이트(파라미터/권한/회사소유/사업장)를 적용하되,
     *    세션 상태 게이트(GENERATABLE_STATUS)와 LLM 게이트는 검사하지 않는다(게이트 OFF 에서도 목록은 노출).
     *    LLM 미호출·감사 미기록.
     */
    TbmAiUnconfirmedResponse unconfirmedItems(TbmAi02Param param);
}
