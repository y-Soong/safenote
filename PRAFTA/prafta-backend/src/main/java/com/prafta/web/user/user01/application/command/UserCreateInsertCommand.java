package com.prafta.web.user.user01.application.command;

/**
 * TB_USER 단건 INSERT 커맨드 (PRAFTA-036).
 *
 * <p>호출 시점:
 * <ul>
 *   <li>관리자 단건 생성 ({@code POST /webApi/user01/insert-user-info})</li>
 *   <li>엑셀 업로드 행 단위 처리 (PRAFTA-036-3, 동일 로직 재사용)</li>
 * </ul>
 *
 * <p>입력 PII(휴대폰/이메일/생년월일)는 서비스 계층에서 AES-GCM 으로 암호화된 값을,
 * HMAC 인덱스는 서명된 값을 받아 그대로 INSERT 한다.
 * USER_PW 는 휴대폰 11자리(하이픈 제외)를 BCrypt 해시한 값(D3).
 * ACCOUNT_STATUS 는 항상 '04 인증대기' 고정(D2).
 */
public record UserCreateInsertCommand(
    String cmpnyCd
    , String userCd
    , String userId
    , String userNm
    , String userPw
    , String siteCd
    , String nodeCd
    , String authCd
    , String rankCd
    , String mblNoEnc
    , String mblNoHmac
    , String mblNoLast4
    , String emailEnc
    , String emailHmac
    , String emailDomain
    , String birthDtEnc
    , String hireDate
    , String employmentType
    , String contractEndDate
    , String gender
    , String gvUserCd
) {
}
