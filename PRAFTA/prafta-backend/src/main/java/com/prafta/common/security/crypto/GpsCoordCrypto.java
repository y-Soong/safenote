package com.prafta.common.security.crypto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * GPS 좌표(위도/경도) 전용 AES-GCM 암복호 헬퍼 (GPS좌표-암호화-전환).
 *
 * <p>decimal(10,7) 좌표 컬럼(TB_USER_ATTD_GPS.LAT/LON, TB_TBM_ATTENDANCE.ENTRY_GPS_LAT/LON,
 * TB_TBM_SESSION.MANAGER_GPS_LAT/LON)을 애플리케이션 레벨 암호화로 전환하기 위한 단일 정규화 규칙을
 * 제공한다. 기존 PII 와 동일한 {@link AesGcmCrypto} 빈/키(PRAFTA_AES_DATA_KEY)를 재사용한다.
 *
 * <p>정규화 규칙(쓰기·백필 공통, plan §1.1):
 *   {@code BigDecimal → setScale(7, HALF_UP) → toPlainString() → encrypt()}.
 *   scale 7 고정은 기존 decimal(10,7) 컬럼의 반올림(HALF_UP) 동작을 미러하며,
 *   toPlainString() 으로 지수 표기를 금지한다(예: "37.501" → "37.5010000").
 *
 * <p>Fallback 규칙(읽기 공통): {@code *_ENC IS NOT NULL} 이면 복호화값, NULL 이면 구 평문 컬럼값.
 *
 * <p>보안: 평문 좌표/암호문 값은 예외 메시지·로그에 절대 포함하지 않는다.
 * 복호화 실패는 예외 전파(조용한 null 반환 금지 — 무결성 이상의 조기 발견).
 */
@Component
@RequiredArgsConstructor
public class GpsCoordCrypto {

    /** 좌표 정규화 소수 자릿수(decimal(10,7) 미러). */
    public static final int COORD_SCALE = 7;

    private final AesGcmCrypto aesGcmCrypto;

    /** BigDecimal 좌표 → scale 7 정규화 평문 문자열. null 이면 null(백필 자가검증 대조용 공개). */
    public String normalize(BigDecimal coord) {
        if (coord == null) {
            return null;
        }
        return coord.setScale(COORD_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    /** BigDecimal 좌표 → 정규화 → 암호문. null 이면 null(좌표 결측 행 미러). */
    public String encrypt(BigDecimal coord) {
        String normalized = normalize(coord);
        return normalized == null ? null : aesGcmCrypto.encrypt(normalized);
    }

    /**
     * Double 좌표 → 암호문. {@code BigDecimal.valueOf(double)} 경유
     * ({@code new BigDecimal(double)} 금지 — 이진 부동소수 오차 유입 방지).
     */
    public String encrypt(Double coord) {
        return coord == null ? null : encrypt(BigDecimal.valueOf(coord));
    }

    /**
     * String 좌표(클라이언트 전송 문자열) → trim → BigDecimal 파싱 → 암호문.
     * 공백/빈 문자열은 null 반환(기존 normalize 트림 규칙 미러).
     *
     * @throws NumberFormatException 숫자 파싱 실패 시(호출부가 기존 오류 체계로 매핑 — 예: TBM_400_012)
     */
    public String encryptString(String coord) {
        if (coord == null || coord.trim().isEmpty()) {
            return null;
        }
        return encrypt(new BigDecimal(coord.trim()));
    }

    /** 암호문 → 정규화 평문 문자열("37.5010000" 형태). null 이면 null. 실패 시 예외 전파. */
    public String decryptToPlain(String enc) {
        return enc == null ? null : aesGcmCrypto.decrypt(enc);
    }

    /**
     * Fallback resolve(BigDecimal 소비처용): 암호문이 있으면 복호화값, 없으면 구 평문 컬럼값.
     * lat/lon 은 항상 쌍 단위 동일 소스로 기록되므로 개별 컬럼 단위 판정으로 충분하다.
     */
    public BigDecimal resolveToBigDecimal(String enc, BigDecimal plain) {
        if (enc == null) {
            return plain;
        }
        return new BigDecimal(decryptToPlain(enc));
    }

    /**
     * Fallback resolve(String 소비처용): 암호문이 있으면 복호화 평문 문자열, 없으면 구 평문 값 그대로.
     * 복호화 평문은 scale 7 정규화 문자열이라 기존 decimal→String 매핑 결과와 동일 포맷이다.
     */
    public String resolveToString(String enc, String plain) {
        if (enc == null) {
            return plain;
        }
        return decryptToPlain(enc);
    }
}
