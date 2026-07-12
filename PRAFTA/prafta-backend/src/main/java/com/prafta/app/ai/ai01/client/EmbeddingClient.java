package com.prafta.app.ai.ai01.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.prafta.common.config.AiProperties;
import com.prafta.common.error.ai.AiErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.extern.slf4j.Slf4j;

/**
 * TEI(BGE-m3) 임베딩 서버 클라이언트.
 *
 * <p>POST {tei.url}/embed, body {@code {"inputs":"<query>"}} → 응답 {@code [[...1024 floats...]]}.
 *    응답 첫 벡터를 float[1024] 로 반환하며, 길이가 1024가 아니면 AI_502_002 로 실패시킨다.
 */
@Slf4j
@Component
public class EmbeddingClient {

    /** BGE-m3 임베딩 차원. */
    private static final int EMBED_DIM = 1024;

    private final RestTemplate aiTeiRestTemplate;
    private final AiProperties aiProperties;

    // RestTemplate 빈이 여러 개(holidayRestTemplate/aiTeiRestTemplate)라
    // @Qualifier로 임베딩 전용 빈을 명시 주입한다. Lombok 생성자에 의존하지 않도록 명시적 생성자 사용.
    public EmbeddingClient(@Qualifier("aiTeiRestTemplate") RestTemplate aiTeiRestTemplate,
                           AiProperties aiProperties) {
        this.aiTeiRestTemplate = aiTeiRestTemplate;
        this.aiProperties = aiProperties;
    }

    /**
     * 질의를 임베딩하여 float[1024] 반환.
     * @throws ApiException 호출 실패(AI_502_001) / 응답 형식·차원 오류(AI_502_002)
     */
    public float[] embed(String query) {

        String url = aiProperties.getTei().getUrl() + "/embed";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("inputs", query), headers);

        float[][] response;
        try {
            // TEI /embed 응답은 벡터의 배열([[...]]) 형태.
            response = aiTeiRestTemplate.postForObject(url, entity, float[][].class);
        } catch (RestClientException e) {
            log.error("TEI 임베딩 호출 실패 - url={}, 원인={}", url, e.getMessage());
            throw new ApiException(AiErrorCode.AI_502_001);
        }

        if (response == null || response.length == 0 || response[0] == null) {
            log.error("TEI 임베딩 응답이 비어 있음 - url={}", url);
            throw new ApiException(AiErrorCode.AI_502_002);
        }

        float[] vector = response[0];
        if (vector.length != EMBED_DIM) {
            log.error("TEI 임베딩 차원 불일치 - 기대={}, 실제={}", EMBED_DIM, vector.length);
            throw new ApiException(AiErrorCode.AI_502_002);
        }

        return vector;
    }
}
