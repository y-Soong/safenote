package com.prafta.common.cmm.tbmshare.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.prafta.common.cmm.tbmshare.result.TbmEntryHandle;
import com.prafta.common.error.tbm.TbmErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.crypto.AesGcmCrypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 대리입실 후보 핸들 코덱(PRAFTA-SUBCON-T5 M1).
 *
 * <p>후보 목록 행마다 {@code (sessionCd, cmpnyCd, userTypeCd, userCd)} 를 묶어 <b>AES-GCM 으로 암호화</b>한
 * 불투명 문자열을 발급한다. 대리입실 요청은 이 핸들만 키로 받는다.
 *
 * <ul>
 *   <li><b>기밀성</b>: AES-GCM 암호문이라 클라이언트가 회사코드/사용자코드를 복원할 수 없다
 *       (2차 이하 회사의 존재·코드가 새지 않는다 — 마스터 §1-3).</li>
 *   <li><b>무결성</b>: GCM 인증태그로 위·변조 시 복호화가 실패한다(임의 회사/사용자 주입 불가).</li>
 *   <li><b>세션 바인딩</b>: 평문에 sessionCd 를 포함하고 복호 후 요청 sessionCd 와 대조해,
 *       다른 세션에서 발급받은 핸들의 재사용(replay)을 차단한다.</li>
 * </ul>
 *
 * <p>기존 선례({@link AesGcmCrypto} — mypage/baseinfo/dailyjoin 에서 PII 암복호에 사용)를 그대로 재사용한다.
 * 핸들은 서버 상태를 두지 않는 stateless 토큰이므로 별도 저장/만료 관리가 필요 없다(세션 상태 가드와
 * 대상 유효성 재검증이 입실 시점에 다시 수행되므로 오래된 핸들도 안전하게 거부된다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TbmEntryHandleCodec {

    private final AesGcmCrypto aesGcmCrypto;

    /** 평문 구분자(코드값에 등장하지 않는 문자). */
    private static final String DELIM = "|";
    /** 포맷 버전 태그(향후 구조 변경 대비). */
    private static final String TAG = "T5E1";

    /** 후보 1행 → 불투명 핸들 발급. */
    public String encode(String sessionCd, String cmpnyCd, String userTypeCd, String userCd) {
        String plain = String.join(DELIM, TAG, sessionCd, cmpnyCd, userTypeCd, userCd);
        return aesGcmCrypto.encrypt(plain);
    }

    /**
     * 핸들 복호 + 세션 바인딩 검증.
     *
     * @param handle           클라이언트가 돌려준 불투명 핸들
     * @param expectedSessionCd 현재 요청의 세션(발급 시점과 달라지면 거부)
     * @throws ApiException 위·변조/형식오류/세션 불일치 시 TBM_403_040(대상 부적합 — 존재 비노출)
     */
    public TbmEntryHandle decode(String handle, String expectedSessionCd) {
        if (!StringUtils.hasText(handle)) {
            throw new ApiException(TbmErrorCode.TBM_400_060);
        }

        String plain;
        try {
            plain = aesGcmCrypto.decrypt(handle);
        } catch (Exception e) {
            // 위·변조(GCM 인증태그 불일치) 또는 형식오류. 원문은 로깅하지 않는다.
            log.warn("TBM 대리입실 핸들 복호 실패(위·변조 의심) - sessionCd={}", expectedSessionCd);
            throw new ApiException(TbmErrorCode.TBM_403_040);
        }

        String[] parts = plain != null ? plain.split("\\" + DELIM, -1) : new String[0];
        if (parts.length != 5 || !TAG.equals(parts[0])) {
            log.warn("TBM 대리입실 핸들 형식 오류 - sessionCd={}", expectedSessionCd);
            throw new ApiException(TbmErrorCode.TBM_403_040);
        }

        // 세션 바인딩: 다른 세션에서 발급받은 핸들 재사용 차단.
        if (!parts[1].equals(expectedSessionCd)) {
            log.warn("TBM 대리입실 핸들 세션 불일치(재사용 시도) - sessionCd={}", expectedSessionCd);
            throw new ApiException(TbmErrorCode.TBM_403_040);
        }

        return new TbmEntryHandle(parts[1], parts[2], parts[3], parts[4]);
    }
}
