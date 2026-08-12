package com.prafta.web.user.user09.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * 소정-09: 셀프가입 신청 목록 응답 (User_09).
 *
 * <p>PII 최소화 — 휴대폰은 <b>마스킹 문자열만</b> 내려간다(암호문/평문 금지, 정책 §11.1).
 * 이메일·생년월일은 승인 판단에 필요하지 않아 아예 내려주지 않는다.
 */
@Value
@Builder
public class SelfJoinListResponse {

    /** 신청 목록 (신청일 내림차순) */
    List<Row> selfJoinList;

    @Value
    @Builder
    public static class Row {

        /** 사용자 코드 (승인/거부 요청 키) */
        String userCd;

        /** 로그인 아이디 */
        String userId;

        /** 이름 */
        String userNm;

        /** 사업장 코드 */
        String siteCd;

        /** 사업장명 */
        String siteNm;

        /** 부서 코드 */
        String nodeCd;

        /** 부서명 */
        String nodeNm;

        /** 마스킹 휴대폰 (010-****-1234) */
        String mblNo;

        /** 계정상태 [SYS013] '06' 승인대기 / '07' 가입거부 */
        String accountStatus;

        /** 신청 일시 (yyyy-MM-dd HH:mm) */
        String applyDtime;
    }
}
