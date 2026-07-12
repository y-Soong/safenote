package com.prafta.platform.common.command;

/**
 * 플랫폼 영역 TB_USER 단건 INSERT 커맨드.
 *
 * <p>두 경로에서 공용으로 사용한다.
 * <ul>
 *   <li>최초 플랫폼 운영자 부트스트랩({@code PlatformOperatorBootstrapRunner}) — ACCOUNT_STATUS='01'(즉시 활성),
 *       SITE_CD/NODE_CD = null.</li>
 *   <li>신규 고객사 master 계정 프로비저닝({@code CompanyProvisionServiceImpl}) — ACCOUNT_STATUS='04'(인증대기),
 *       SITE_CD/NODE_CD = 신규 사업장/노드.</li>
 * </ul>
 *
 * <p>PII(휴대폰)는 호출 계층에서 AES-GCM 암호화/HMAC/last4 로 가공한 값을 받아 그대로 INSERT 한다
 * (User01ServiceImpl.insertUserOne 와 동일 규약). USER_PW 는 BCrypt 해시값.
 */
public record PlatformUserInsertCommand(
    String cmpnyCd
    , String userCd
    , String userId
    , String userNm
    , String userPw
    , String siteCd
    , String nodeCd
    , String authCd
    , String accountStatus
    , String mblNoEnc
    , String mblNoHmac
    , String mblNoLast4
    , String insertNo
) {
}
