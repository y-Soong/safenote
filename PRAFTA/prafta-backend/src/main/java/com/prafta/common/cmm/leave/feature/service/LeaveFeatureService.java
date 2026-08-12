package com.prafta.common.cmm.leave.feature.service;

import com.prafta.common.cmm.leave.feature.vo.LeaveFeatureVisibilityVO;
import com.prafta.common.cmm.stdwork.vo.StdWorkHoursSummaryVO;

/**
 * 소정-06: 연차 기능 노출 판정 + 소정근로(단시간 파생) 조회 서비스.
 *
 * <p>지시서 {@code 작업지시서_근로자별-소정근로시간-관리-도입.md} 완료 기준
 * "연차 기능 노출 판정" / "단시간 여부 파생 조회 API (판정·부여 로직 연결은 하지 않음)".
 *
 * <p><b>★0단계 경계</b>: 본 서비스는 <b>읽기 전용 노출/조회</b>만 담당한다. 소정근로 값을
 * 연차 부여량 산정·차감 판정에 연결하지 않는다(2단계 착수 시점 작업).
 *
 * <p>웹/앱 공용이므로 {@code com.prafta.common} 하위에 둔다(자동 프리픽스 → {@code /prafta/comApi}).
 */
public interface LeaveFeatureService {

    /**
     * 회사 단위 연차 기능 노출 판정.
     *
     * <p>판정식은 {@link LeaveFeatureVisibilityVO} javadoc 참조
     * (토글 off <b>그리고</b> 부여 이력 0 일 때만 숨김).
     *
     * @param cmpnyCd 회사 코드 (JWT 도출 — 요청 파라미터로 받지 않는다)
     */
    LeaveFeatureVisibilityVO resolveVisibility(String cmpnyCd);

    /**
     * 본인 소정근로 요약 조회 (단시간 파생 판정 포함).
     *
     * <p>{@code StdWorkHoursService.resolveSummary} 를 그대로 노출하는 얇은 위임이다.
     * 이력 미입력 계정은 회사 통상 기준값(기본 2400분)으로 폴백되며, 그 사실이
     * {@code source} 로 함께 실린다.
     *
     * @param cmpnyCd 회사 코드 (JWT 도출)
     * @param userCd  사용자 코드 (JWT 도출 — 타인 조회 불가, IDOR 차단)
     * @param baseYmd 기준일 (YYYYMMDD). null/공백/형식오류면 오늘로 대체
     */
    StdWorkHoursSummaryVO resolveMyStdWorkSummary(String cmpnyCd, String userCd, String baseYmd);
}
