package com.prafta.common.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 근무타입(SCH_CD) 시간/휴게 변경·사용중지 하드 차단(com-016-A 공통 가드 ③, {@code ATTD_400_162}/{@code 163})
 * 관련 <b>사용자 노출 문구</b> 생성기.
 *
 * <p>가드가 이미 갖고 있던 차단 대상 근무일(blockedYmds/futurePlans)을 로그에만 남기고 응답에는
 * 담지 않아, 관리자가 원인 날짜·건수를 알 수 없었던 문제(F-12-1)를 해소한다.
 *
 * <p><b>문구 규약({@code AttdOverlapMessages}, 커밋 74a89a25 선례 승계)</b>
 * <ul>
 *   <li><b>날짜 + 건수만</b> 담는다. <b>USER_CD·사용자명 등 PII 는 절대 넣지 않는다.</b></li>
 *   <li>날짜가 많을 때는 앞 {@value #DISPLAY_LIMIT}건만 나열하고 "외 N일"로 나머지를 요약한다.</li>
 * </ul>
 *
 * <p>클래스명은 {@code ScheduleLockMessages}(연차 잠금 <b>사유</b> 문구, F-7 신설)와 혼동되지 않도록
 * 근무타입(Sch) 잠금(Lock) <b>일자 목록</b> 전용 이름으로 분리했다.
 */
public final class AttdSchLockMessages {

	/** 문구에 나열할 최대 일자 수. 초과분은 "외 N일"로 요약한다. */
	private static final int DISPLAY_LIMIT = 3;

	private AttdSchLockMessages() {
		// 유틸리티 클래스 - 인스턴스 생성 금지
	}

	/**
	 * 근무타입 시간/휴게 변경 차단({@code ATTD_400_162}) 동적 메시지.
	 *
	 * @param blockedYmds 시간/휴게 변경으로 영향받는 근무일(yyyyMMdd) 목록(중복 가능)
	 */
	public static String timeChangeBlockedMessage(List<String> blockedYmds) {
		return blockedMessage(blockedYmds, "연차·초과근무가 등록되어 있어", "시간을 변경할 수 없습니다.");
	}

	/**
	 * 근무타입 사용중지 차단({@code ATTD_400_163}) 동적 메시지.
	 *
	 * @param futureYmds 사용중지 시점 이후 배정된 근무계획 일자(yyyyMMdd) 목록(중복 가능)
	 */
	public static String deactivateBlockedMessage(List<String> futureYmds) {
		return blockedMessage(futureYmds, "근무계획이 배정되어 있어", "사용중지할 수 없습니다.");
	}

	private static String blockedMessage(List<String> ymds, String reasonPhrase, String actionPhrase) {
		List<String> distinctSorted = distinctSorted(ymds);
		if(distinctSorted.isEmpty()) {
			// 호출자는 목록이 비어 있지 않을 때만 부르지만(가드 로직상 isEmpty 체크 후 호출),
			// 방어적으로 정적 문구 수준의 기본값을 돌려준다.
			return "해당 일정으로 인해 " + actionPhrase;
		}

		String firstDay = monthDayText(distinctSorted.get(0));
		int total = distinctSorted.size();

		StringBuilder listText = new StringBuilder();
		int shown = Math.min(DISPLAY_LIMIT, total);
		for(int i = 0; i < shown; i++) {
			if(i > 0) {
				listText.append(", ");
			}
			listText.append(slashText(distinctSorted.get(i)));
		}
		if(total > shown) {
			listText.append(" 외 ").append(total - shown).append("일");
		}

		return firstDay + " 등 " + total + "일에 " + reasonPhrase + " " + actionPhrase
				+ " (" + listText + ")";
	}

	/** 중복 제거 + 오름차순 정렬(yyyyMMdd 문자열은 사전순 = 날짜순). null/형식 오류 항목은 제외. */
	private static List<String> distinctSorted(List<String> ymds) {
		Set<String> distinct = new LinkedHashSet<>();
		if(ymds != null) {
			for(String ymd : ymds) {
				if(ymd != null && ymd.length() == 8) {
					distinct.add(ymd);
				}
			}
		}
		List<String> sorted = new ArrayList<>(distinct);
		sorted.sort(String::compareTo);
		return sorted;
	}

	/** "9월 17일". {@link AttdOverlapMessages#monthDayText} 와 동일한 날짜 포맷을 재사용(형식 통일). */
	private static String monthDayText(String yyyymmdd) {
		String text = AttdOverlapMessages.monthDayText(yyyymmdd);
		return text == null ? yyyymmdd : text;
	}

	/** "9/17". */
	private static String slashText(String yyyymmdd) {
		try {
			int mm = Integer.parseInt(yyyymmdd.substring(4, 6));
			int dd = Integer.parseInt(yyyymmdd.substring(6, 8));
			return mm + "/" + dd;
		} catch(RuntimeException e) {
			return yyyymmdd;
		}
	}
}
