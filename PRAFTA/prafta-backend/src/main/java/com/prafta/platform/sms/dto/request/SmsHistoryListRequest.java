package com.prafta.platform.sms.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Platform_05(SMS 발송 관리) 발송 이력 목록 조회 요청 — <b>요청 바디 바인딩</b>({@code @RequestBody}).
 *
 * <p>★★<b>조회인데 POST 인 이유 — GET/쿼리스트링으로 되돌리지 말 것.</b>
 * 이 DTO 의 {@code mblNo} 는 <b>휴대폰 평문</b>이다. 쿼리스트링으로 받으면 서버가 로그를 남기지
 * 않아도 <b>nginx/ALB access log · CloudFront 로그 · 브라우저 히스토리·리퍼러</b>에 평문이 복제된다.
 * 이들은 애플리케이션 로그와 보존주기·접근통제가 달라 사후 회수가 어렵다(정책 §11.1 최소 수집).
 * 바디로 받으면 그 경로가 전부 사라진다. 상세 사유는 {@code PlatformSmsController} 참조.
 *
 * <p>회사코드/운영자는 담지 않는다 — 전부 JWT 클레임에서만 도출한다.
 * 애초에 {@code TB_SMS_AUTH_CODE} 에는 {@code CMPNY_CD} 가 없어 테넌트 술어를 걸 수단이 없고,
 * 그래서 이 조회는 <b>플랫폼 운영자 전용</b>({@code /platformApi} 게이트 뒤)에서만 성립한다.
 *
 * <p>★인증번호({@code AUTH_CD}) 검색 조건은 <b>절대 추가하지 않는다.</b> 인증번호를 조건으로 받는
 * 순간 "번호로 행 찾기 → 인증번호 대입" 의 역방향 오라클이 만들어진다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SmsHistoryListRequest {

    /** 발송기간 시작일 (선택, yyyy-MM-dd). 미입력이면 서버가 오늘-6일로 채운다. */
    private String startDate;

    /** 발송기간 종료일 (선택, yyyy-MM-dd). 미입력이면 서버가 오늘로 채운다. */
    private String endDate;

    /** 인증 목적 필터 (선택) — SELF_JOIN / PLATFORM_LOCATION / MOBILE_CHANGE, 미지정=전체 */
    private String purposeCd;

    /** 발송 상태 필터 (선택) — PENDING / SENT / FAILED / SKIPPED, 미지정=전체 */
    private String sendStatus;

    /**
     * 휴대폰 번호 검색 (선택, 평문 입력).
     *
     * <p>★서버가 {@code HmacSigner} 로 변환해 {@code MBL_NO_HMAC} <b>정확 일치</b>로만 조회한다.
     * 부분일치/LIKE/스캔은 제공하지 않는다(암호문은 LIKE 가 불가능하고, 부분일치를 흉내내려면
     * 전 행 복호가 필요해져 PII 대량 처리가 된다).
     * <p>★이 값은 평문 PII 다 — 로그·응답·감사 detail 어디에도 남기지 않는다.
     */
    private String mblNo;

    /** 페이지 번호 (1-base, 기본 1) */
    private Integer page;

    /** 페이지 크기 (기본 20, 상한 100) */
    private Integer pageSize;
}
