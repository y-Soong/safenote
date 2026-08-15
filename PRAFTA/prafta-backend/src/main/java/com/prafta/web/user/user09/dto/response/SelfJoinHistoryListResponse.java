package com.prafta.web.user.user09.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * 소정-09: 셀프가입 처리 이력 목록 응답 (User_09 처리 이력 탭).
 *
 * <p>PII 최소화 — 휴대폰은 <b>마스킹 문자열만</b> 내려간다(암호문/평문 금지, 정책 §11.1).
 * 노출 항목은 대기 목록({@link SelfJoinListResponse})과 <b>동일 집합</b>으로 고정한다.
 * 이력이라는 이유로 이메일·생년월일·권한·고용형태를 추가하지 않는다.
 *
 * <p>사업장/부서는 <b>코드 없이 명칭만</b> 내려준다 — 이력 행에는 후속 액션(승인/거부 팝업)이 없어
 * 코드의 소비처가 없다.
 */
@Value
@Builder
public class SelfJoinHistoryListResponse {

    /** 처리 이력 목록 (처리일시 내림차순, 현재 페이지 분) */
    List<Row> historyList;

    /** 조건 전체 건수 (페이저 계산용) */
    int totalCount;

    @Value
    @Builder
    public static class Row {

        /** 감사 로그 ID — 행의 유일 식별자(프론트 key) */
        String auditId;

        /** 처리 일시 (yyyy-MM-dd HH:mm) */
        String processDtime;

        /** 처리 결과 — 'APPROVE' 승인 / 'REJECT' 거부 */
        String actionType;

        /** 대상 사용자 코드 */
        String userCd;

        /** 대상 로그인 아이디 */
        String userId;

        /** 대상 이름 */
        String userNm;

        /** 사업장명 (대상 계정의 현재 소속) */
        String siteNm;

        /** 부서명 (대상 계정의 현재 소속) */
        String nodeNm;

        /** 마스킹 휴대폰 (010-****-1234) */
        String mblNo;

        /** 신청 일시 (yyyy-MM-dd HH:mm) */
        String applyDtime;

        /**
         * 승인 당시 입사일 (yyyy-MM-dd).
         *
         * <p>거부 건은 입사일이 확정되지 않으므로 통상 null 이다(화면은 '-' 로 표기).
         */
        String hireDate;

        /** 승인 당시 직급명. 미지정이거나 거부 건이면 null */
        String rankNm;

        /** 처리자 이름 (이름이 없으면 사용자 코드) */
        String processorNm;

        /** 거부 사유. 승인 건은 null */
        String rejectReason;
    }
}
