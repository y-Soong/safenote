package com.prafta.web.user.user10.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * 소정-10: 소정근로시간 관리 대상 목록 응답 (User_10).
 *
 * <p>PII 를 담지 않는다(이름·부서까지만 — 휴대폰/이메일/생년월일 미노출).
 */
@Value
@Builder
public class StdWorkUserListResponse {

    /** 회사 통상근로자 주 소정근로 분 (비교 분모 · "풀타임" 라벨 원천). 화면 하드코딩 금지. */
    int cmpnyWeekStdMinutes;

    /**
     * 목록 (오늘 기준 유효 이력 조인 결과).
     *
     * <p>기준일은 목록 SQL 안에서 DB NOW() 로 만든다 — 응답에 별도로 싣지 않는다
     * (표기용 값 하나 때문에 조회를 늘리지 않는다).
     */
    List<Row> stdWorkUserList;

    @Value
    @Builder
    public static class Row {

        String userCd;

        String userId;

        String userNm;

        String nodeCd;

        String nodeNm;

        /** 고용형태 [SYS041] */
        String employmentType;

        /** 입사일 (YYYYMMDD, 미입력이면 null) */
        String hireDate;

        /**
         * 오늘 기준 유효 이력의 주 소정근로 분.
         *
         * <p><b>null = 이력 미입력</b> — 화면은 "미입력(통상 기준 간주)" 배지를 표시한다.
         * 서버가 폴백값(회사 기준/2400)으로 채우지 않는 것은 의도다.
         */
        Integer weekStdMinutes;

        /** 유효 이력의 사유코드 [SYS083] (미입력이면 null) */
        String reasonCd;

        /** 유효 이력의 사유 명칭 (미입력이면 null) */
        String reasonNm;

        /** 유효 이력 적용 시작일 (미입력이면 null) */
        String applyStrDate;

        /** 유효 이력 적용 종료일 (무기한/미입력이면 null) */
        String applyEndDate;

        /**
         * 단시간근로자 파생 여부 (본인 주 소정 &lt; 회사 통상 기준).
         *
         * <p>이력 미입력 계정은 통상 기준으로 폴백되므로 항상 false(지시서 B-2 파생 판정).
         */
        boolean partTime;

        /** 이력이 실제 입력된 계정인지 (false = 폴백 해석) */
        boolean fromHistory;
    }
}
