package com.prafta.common.cmm.leave.feature.vo;

import lombok.Builder;
import lombok.Getter;

/**
 * 소정-06: 회사 단위 연차 기능 노출 판정 결과.
 *
 * <p>지시서 §연차 부여 on/off 토글 — "자동 부여 off <b>+</b> 부여 이력 0 인 회사는 연차 카드·신청
 * 진입점 숨김(일용직 게이트 패턴)".
 *
 * <p><b>판정식</b>: {@code visible = !(statutoryAutoGrantYn='N' AND grantHistoryExists=false)}
 * <ul>
 *   <li>토글 off + 이력 0 → 숨김. 연차를 한 번도 운영한 적 없는 5인 미만 사업장.</li>
 *   <li>토글 off + 이력 1건 이상 → <b>노출 유지</b>. 이미 부여된 연차의 잔여 확인·사용·이력 조회가
 *       가능해야 한다(off 는 신규 자동 부여 중지일 뿐 몰수가 아니다).</li>
 *   <li>토글 on → 이력과 무관하게 노출(기존 동작).</li>
 * </ul>
 *
 * <p>판정 단위는 <b>회사</b>다(지시서 표기 그대로). 개인별 부여 이력으로 좁히지 않는다 —
 * 신입 사원처럼 아직 부여가 없는 개인의 화면까지 사라지는 부작용을 피하기 위함이다.
 */
@Getter
@Builder
public class LeaveFeatureVisibilityVO {

    /** 회사 코드 (JWT 도출) */
    private final String cmpnyCd;

    /** 법정 연차 자동 부여 사용 여부 (활성 정책 부재/NULL 이면 true = 기존 동작) */
    private final boolean statutoryAutoGrantEnabled;

    /** 회사에 연차 부여 이력(DEL_YN='N')이 1건이라도 존재하는지 */
    private final boolean grantHistoryExists;

    /** 연차 기능(카드·신청 진입점) 노출 여부 — 위 판정식 결과 */
    private final boolean leaveFeatureVisible;
}
