package com.prafta.web.leave.promotion.leavepromo01.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.web.leave.promotion.leavepromo01.dto.PromotionExcelFailItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-com-008-A-6: 엑셀 업로드 실패행(2시트 다운로드)용 임시 보관소(in-memory).
 *
 * <p>prafta-052 보안 규약: 실패행 sourceRow 에는 평문 이름/부서(PII)가 포함되므로, 직렬화한 JSON 을
 * AES-GCM 으로 암호화("v1.*")하여 보관한다(메모리에도 평문 미보관). 다운로드 시 cmpnyCd+userCd 바인딩을
 * 재검증해 타 회사/타 관리자 토큰 도용을 차단한다. 동기 업로드 응답~다운로드 사이의 단기 보관(TTL)이며
 * 영속 저장은 하지 않는다(별도 잡 테이블 불필요 — 동기 단순화).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionExcelFailStore {

    /** 토큰 유효시간(ms) — 업로드 직후 다운로드용 단기 보관. */
    private static final long TTL_MS = 10L * 60 * 1000;
    private static final int MAX_ENTRIES = 500;

    private final AesGcmCrypto aesGcmCrypto;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /** 보관 항목 — 소유자 바인딩 + 암호문 + 만료. */
    private record Entry(String cmpnyCd, String ownerUserCd, String encryptedJson, long expiresAt) {
    }

    /**
     * 실패행을 암호화해 보관하고 토큰을 반환한다. 실패가 없으면 null(토큰 발급 안 함).
     *
     * @param cmpnyCd     소유 회사
     * @param ownerUserCd 업로드 관리자(다운로드 권한 바인딩)
     * @param fails       실패 항목
     * @return 다운로드 토큰(없으면 null)
     */
    public String store(String cmpnyCd, String ownerUserCd, List<PromotionExcelFailItem> fails) {
        if (fails == null || fails.isEmpty()) {
            return null;
        }
        try {
            evictIfNeeded();
            String json = objectMapper.writeValueAsString(fails);
            String enc = aesGcmCrypto.encrypt(json); // at-rest 암호화
            String token = newToken();
            store.put(token, new Entry(cmpnyCd, ownerUserCd, enc, System.currentTimeMillis() + TTL_MS));
            return token;
        } catch (Exception e) {
            // 보관 실패는 다운로드만 막힐 뿐 업로드 결과에는 영향 없음.
            log.error("[promoExcel] 실패행 보관/암호화 실패 — token 미발급", e);
            return null;
        }
    }

    /**
     * 토큰으로 실패행을 복호화해 반환한다. 소유자(cmpnyCd+userCd) 불일치/만료/없음이면 null.
     */
    public List<PromotionExcelFailItem> consume(String token, String cmpnyCd, String userCd) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Entry e = store.get(token);
        if (e == null) {
            return null;
        }
        if (System.currentTimeMillis() > e.expiresAt()) {
            store.remove(token);
            return null;
        }
        // 소유자 바인딩 재검증(타 회사/타 관리자 토큰 도용 차단).
        if (!e.cmpnyCd().equals(cmpnyCd) || !e.ownerUserCd().equals(userCd)) {
            log.warn("[promoExcel] 실패행 토큰 소유자 불일치 — 거부");
            return null;
        }
        try {
            String json = aesGcmCrypto.decrypt(e.encryptedJson());
            PromotionExcelFailItem[] arr = objectMapper.readValue(json, PromotionExcelFailItem[].class);
            return List.of(arr);
        } catch (Exception ex) {
            log.error("[promoExcel] 실패행 복호화 실패", ex);
            return null;
        }
    }

    private String newToken() {
        byte[] b = new byte[24];
        secureRandom.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    /** 만료분 정리 + 상한 초과 시 가장 임박 만료부터 제거(메모리 누수 방지). */
    private void evictIfNeeded() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(en -> now > en.getValue().expiresAt());
        if (store.size() < MAX_ENTRIES) {
            return;
        }
        store.entrySet().stream()
                .sorted((a, b) -> Long.compare(a.getValue().expiresAt(), b.getValue().expiresAt()))
                .limit(Math.max(1, store.size() - MAX_ENTRIES + 1))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(store::remove);
    }
}
