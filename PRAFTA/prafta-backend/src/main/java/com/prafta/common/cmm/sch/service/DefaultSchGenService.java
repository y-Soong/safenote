package com.prafta.common.cmm.sch.service;

/**
 * 기본근무 자동 스케줄 생성 공용 서비스 (PRAFTA-COM-008-E-3).
 *
 * <p>기본 근무타입(tb_user.DEFAULT_SCH_CD)이 설정된 교대 비소속자에게
 * 지정 범위(오늘 또는 1/1 ~ 당해 12/31)의 평일(월~금) work_plan 을 멱등 생성한다.
 * <ul>
 *   <li>덮어쓰기 금지(빈 날만), 교대팀 소속 구간 제외(E-7), 마감월 제외(prafta-028).</li>
 *   <li>GEN_SOURCE='DEFAULT_SCH'. 결정성(난수 금지).</li>
 * </ul>
 *
 * <p>트리거: (1) 매년 1/1 배치(DefaultSchGenScheduler), (2) 설정/변경 즉시(User_01·로그인 게이트 — 패스 B),
 * (3) 폴백 단일일(ensureWorkPlanDay — A/연차 등록 시 호출).
 */
public interface DefaultSchGenService {

    /**
     * 단일 사용자에게 fromYmd~toYmd 평일 기본근무를 멱등 생성한다(교대 비소속·미마감월·빈 날만).
     *
     * @param cmpnyCd 회사 코드
     * @param siteCd  사업장 코드
     * @param userCd  대상 사용자 코드
     * @param schCd   기본 근무타입(SCH_CD). null/blank 면 무동작(생성 안 함).
     * @param fromYmd 시작일(YYYYMMDD, 포함)
     * @param toYmd   종료일(YYYYMMDD, 포함)
     * @return 신규 생성한 일수
     */
    int generateForUser(String cmpnyCd, String siteCd, String userCd,
                        String schCd, String fromYmd, String toYmd);

    /**
     * 폴백용 단일일 생성(A/연차 등록 폴백). 교대 비소속·미마감월·빈 날·평일 무관(연차일은 평일/주말 모두 가능).
     * ⚠️ 폴백은 평일 제한을 적용하지 않는다(촉진/연차 대상일이 주말일 수도 있으므로 그 자리에 스케줄을 깐다).
     * 사용자의 DEFAULT_SCH_CD 를 조회해 사용한다(미설정/교대소속이면 무동작).
     *
     * @return 생성했으면 1, 스킵(이미 존재/교대/마감/미설정)이면 0
     */
    int ensureWorkPlanDay(String cmpnyCd, String userCd, String workYmd);

    /**
     * 기본근무 변경 시 명일(today+1)~당해 12/31 범위에서 자동생성분만 새 SCH_CD 로 갱신 후,
     * 같은 범위의 빈 평일에 신규 생성도 수행한다(D3: 당일 미변경).
     * 마감월은 제외(prafta-028). 수동/연차/교대/촉진 보존.
     *
     * @param cmpnyCd   회사 코드
     * @param siteCd    사업장 코드
     * @param userCd    대상 사용자 코드
     * @param newSchCd  새 기본 근무타입(SCH_CD)
     * @return 갱신/신규 합산 영향 일수
     */
    int applyDefaultSchChange(String cmpnyCd, String siteCd, String userCd, String newSchCd);
}
