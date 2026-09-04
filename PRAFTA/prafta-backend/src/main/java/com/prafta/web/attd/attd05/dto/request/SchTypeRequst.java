package com.prafta.web.attd.attd05.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SchTypeRequst {
	private String siteCd;
	private String userCd;
	private String workYmd;
	private String workPlanCd;
	/**
	 * prafta-com-016-C-2: 연차 셀일 때 적용할 휴가 종류 코드(SYS_ANNUAL=연차 / SYS_MONTHLY=월차).
	 * 근무타입(SCH) 셀이면 null/미사용. 서버가 화이트리스트(SYS_ANNUAL|SYS_MONTHLY)로 검증한다.
	 * (레거시/엑셀 back-compat — UI 직접 종류선택은 C-4 에서 제거됨)
	 */
	private String leaveCd;
	/**
	 * prafta-com-016-C-4: 종류 미지정 "법정 휴가" 자동 적용 셀 여부.
	 * true 면 서버가 후보(연차/월차) 중 소멸 임박 통합순으로 1일을 자동 차감한다(leaveCd 무시).
	 */
	private boolean autoLegalLeave;
	/**
	 * BW-04(qa §5-8): 관리자 직접 차감(근무계획 저장/엑셀) 경로는 휴게시간 무시 요청을 받지 않는다.
	 * 값이 'Y' 로 실려 오면 서버가 ATTD_400_218 로 거부한다(근로자 본인 앱/웹 신청 경로 한정). 정상 요청은 미전송.
	 */
	private String brkWaiveYn;
}
