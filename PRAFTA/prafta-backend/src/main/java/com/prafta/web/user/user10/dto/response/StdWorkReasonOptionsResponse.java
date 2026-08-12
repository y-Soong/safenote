package com.prafta.web.user.user10.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * 소정-10: 소정근로시간 등록/정정 팝업의 셀렉트 옵션 응답 (User_10).
 *
 * <p>계정 생성 경로({@code /user01/std-work-options})와 달리 <b>SYS083 전 사유</b>를 내려준다 —
 * 단축 사유(육아기·임신기·가족돌봄)는 <b>기간과 함께</b> 등록하는 이 화면이 담당하기 때문이다.
 * 사유 목록/명칭을 화면에 하드코딩하지 않기 위한 조회다(행정해석 변동은 코드표 시드로 흡수).
 */
@Value
@Builder
public class StdWorkReasonOptionsResponse {

    /** 회사 통상근로자 주 소정근로 분 (풀타임 라벨/비교 분모) */
    int cmpnyWeekStdMinutes;

    /** 주 15시간(900분) 미만 경고 임계 — 화면 실시간 안내용(서버 판정과 동일 상수) */
    int minWarnWeekMinutes;

    /** 육아기 단축 권장 하한 주 분 */
    int childcareMinWeekMinutes;

    /** 육아기 단축 권장 상한 주 분 */
    int childcareMaxWeekMinutes;

    List<ReasonOption> reasonOptions;

    @Value
    @Builder
    public static class ReasonOption {

        /** 사유코드 [SYS083] */
        String reasonCd;

        /** 사유 명칭 */
        String reasonNm;

        /**
         * 단축 사유 여부 — true 면 화면이 <b>적용 종료일을 필수</b>로 강제한다.
         *
         * <p>판정은 서버가 한다(코드 나열 하드코딩 금지). 서버 저장 검증도 동일 규칙이라
         * 화면이 놓쳐도 {@code STDWORK_400_006} 으로 차단된다.
         */
        boolean reduced;
    }
}
