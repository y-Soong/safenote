package com.prafta.app.ai.ai01.client;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.config.AiProperties;
import com.prafta.common.error.ai.AiErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.extern.slf4j.Slf4j;

/**
 * HyperCLOVA X(HCX-005) 근거답변 호출 클라이언트.
 *
 * <p>★게이트: HCX REST 클라이언트 빈({@code hcxRestClient})은 {@code prafta.ai.llm.enabled=true} 일 때만 존재한다.
 *    ObjectProvider 로 주입해 게이트 OFF(빈 부재) 시 {@link #isEnabled()}=false 로 감지하고,
 *    호출 시 AI_503_001 로 실패시킨다.
 *
 * <p>★하드가드 #4: 본 클라이언트는 <b>systemPrompt 와 userPrompt(recompose 컨텍스트)만</b> 요청 바디에 싣는다.
 *    verbatim 콘텐츠는 서비스에서 애초에 userPrompt 에 포함되지 않으므로 이 지점을 지나지 않는다.
 *
 * <p>★HCX API 계약: {@code POST {host}/v3/chat-completions/HCX-005}. messages 의 content 는 항상 배열
 *    (text 파트 / image_url 파트). 이미지는 {@code dataUri.data=순수 base64}(data: 접두어 없음)로 싣는다.
 *    응답은 {@code status.code=="20000"} 성공 판정, {@code result.message.content}(텍스트)/{@code result.usage} 매핑.
 *    HCX 에는 Claude 의 refusal 개념이 없으므로 {@link LlmRawResponse#refusal()} 은 항상 false.
 */
@Slf4j
@Component
public class LlmAnswerClient {

    /** HCX 비스트리밍 chat-completions 엔드포인트 경로(모델 세그먼트 치환). */
    private static final String CHAT_COMPLETIONS_PATH = "/v3/chat-completions/{model}";
    /** HCX 성공 status 코드. */
    private static final String STATUS_OK = "20000";
    /** 요청 추적 헤더(요청마다 UUID). */
    private static final String REQUEST_ID_HEADER = "X-NCP-CLOVASTUDIO-REQUEST-ID";

    private final ObjectProvider<RestClient> hcxRestClientProvider;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    // ★ObjectProvider 지연 조회는 파라미터 이름을 한정자로 쓰지 않는다 — RestClient 빈이 2개(hcx/ppurio)인
    //   운영에서 @Qualifier 없이는 getIfAvailable() 이 호출 시점 NoUniqueBeanDefinitionException 으로 죽는다.
    public LlmAnswerClient(@Qualifier("hcxRestClient") ObjectProvider<RestClient> hcxRestClientProvider,
                           AiProperties aiProperties,
                           ObjectMapper objectMapper) {
        this.hcxRestClientProvider = hcxRestClientProvider;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    /** 게이트 ON(빈 존재) 여부. */
    public boolean isEnabled() {
        return hcxRestClientProvider.getIfAvailable() != null;
    }

    /**
     * 근거답변 합성 호출(텍스트 전용).
     *
     * @param systemPrompt 그라운딩 시스템 프롬프트
     * @param userPrompt   recompose 청크 컨텍스트 + 질문(★verbatim 미포함)
     * @return refusal(항상 false)/결합 텍스트/usage. 실제 파싱은 서비스가 수행.
     * @throws ApiException AI_503_001(게이트 OFF), AI_502_003(호출 실패/응답 이상)
     */
    public LlmRawResponse answer(String systemPrompt, String userPrompt) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message("system", List.of(textPart(systemPrompt))));
        messages.add(message("user", List.of(textPart(userPrompt))));
        return call(messages, 0);
    }

    /**
     * 이미지 포함 근거답변(멀티모달) 호출.
     *
     * <p>PRAFTA-WEB_003: 위험성평가 이미지 유해요인 분석용. user content 에 image_url 파트 +
     *    text 파트를 함께 싣는다. HCX dataUri.data 는 data URI 형식(data:&lt;mediatype&gt;;base64,...)이라
     *    {@link ImagePart#mediaType()} 로 접두를 조립한다(이미지 화이트리스트/media_type 은 상위 FileService 에서 검증).
     *
     * @param systemPrompt 시스템 프롬프트
     * @param userPrompt   텍스트 컨텍스트(요청 설명 등)
     * @param images       전송 이미지 목록(비어 있으면 텍스트 전용과 동일)
     * @return refusal(항상 false)/결합 텍스트/usage
     * @throws ApiException AI_503_001(게이트 OFF), AI_502_003(호출 실패/응답 이상)
     */
    public LlmRawResponse answerWithImages(String systemPrompt, String userPrompt, List<ImagePart> images) {
        // user content = 이미지 파트들 + 텍스트 파트(마지막).
        List<Map<String, Object>> userContent = new ArrayList<>();
        int imageCount = 0;
        if (images != null) {
            for (ImagePart img : images) {
                if (img == null || img.base64() == null || img.base64().isBlank()) {
                    continue;
                }
                userContent.add(imagePart(img.base64(), img.mediaType()));
                imageCount++;
            }
        }
        userContent.add(textPart(userPrompt));

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message("system", List.of(textPart(systemPrompt))));
        messages.add(message("user", userContent));
        return call(messages, imageCount);
    }

    /**
     * 멀티턴 대화 호출(PRAFTA-WEB_003 v2 대화형 이미지분석).
     *
     * <p>HCX 는 stateless 이므로 매 호출마다 system + 전 대화(turns)를 messages 배열로 재구성한다.
     *    각 턴은 {@code message(role, content[])} 로 조립하며, 이미지 파트가 있으면 imagePart*N + textPart,
     *    없으면 textPart 만 싣는다(HCX content 는 항상 배열). 이미지 요청당5/턴당1 제약은 상위(서비스)에서 지킨다.
     *
     * @param systemPrompt 시스템 프롬프트(별도 system 메시지로 선두 투입)
     * @param turns        대화 턴 목록(role=user|assistant, text, 선택 images)
     * @return refusal(항상 false)/결합 텍스트/usage
     * @throws ApiException AI_503_001(게이트 OFF), AI_502_003(호출 실패/응답 이상)
     */
    public LlmRawResponse chat(String systemPrompt, List<HcxTurn> turns) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message("system", List.of(textPart(systemPrompt))));

        int imageCount = 0;
        if (turns != null) {
            for (HcxTurn turn : turns) {
                if (turn == null || turn.role() == null) {
                    continue;
                }
                List<Map<String, Object>> content = new ArrayList<>();
                if (turn.images() != null) {
                    for (ImagePart img : turn.images()) {
                        if (img == null || img.base64() == null || img.base64().isBlank()) {
                            continue;
                        }
                        content.add(imagePart(img.base64(), img.mediaType()));
                        imageCount++;
                    }
                }
                content.add(textPart(turn.text()));
                messages.add(message(turn.role(), content));
            }
        }
        return call(messages, imageCount);
    }

    // ------------------------------------------------------------------
    // 내부 호출/파싱
    // ------------------------------------------------------------------

    /** 공통 호출: 게이트 확인 → 본문 직렬화 → POST → 응답 파싱. */
    private LlmRawResponse call(List<Map<String, Object>> messages, int imageCount) {
        RestClient client = hcxRestClientProvider.getIfAvailable();
        if (client == null) {
            // 게이트 OFF/빈 부재.
            throw new ApiException(AiErrorCode.AI_503_001);
        }

        String model = aiProperties.getLlm().getModel();

        // ★주입된 ObjectMapper 로 요청 본문 직렬화(질의 원문/이미지 바이트는 로그 미출력).
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(buildBody(messages));
        } catch (Exception e) {
            log.error("HCX 요청 본문 직렬화 실패 - model={}, 원인={}", model, e.getMessage());
            throw new ApiException(AiErrorCode.AI_502_003);
        }

        String requestId = UUID.randomUUID().toString();
        String responseBody;
        try {
            responseBody = client.post()
                .uri(CHAT_COMPLETIONS_PATH, model)
                .contentType(MediaType.APPLICATION_JSON)
                .header(REQUEST_ID_HEADER, requestId)
                .body(requestJson)
                .retrieve()                       // 4xx/5xx → RestClientResponseException(=RestClientException)
                .body(String.class);
        } catch (RestClientException e) {
            // HTTP 4xx/5xx/타임아웃/네트워크 → 원인은 서버 로그만.
            log.error("HCX 호출 실패 - model={}, reqId={}, 원인={}", model, requestId, e.getMessage());
            throw new ApiException(AiErrorCode.AI_502_003);
        }

        return parseResponse(responseBody, model, imageCount, requestId);
    }

    /** HCX 비스트리밍 응답 파싱 → LlmRawResponse(refusal=false 고정, cache=0). */
    private LlmRawResponse parseResponse(String responseBody, String model, int imageCount, String requestId) {
        if (responseBody == null || responseBody.isBlank()) {
            log.error("HCX 응답 본문이 비어 있음 - model={}, reqId={}", model, requestId);
            throw new ApiException(AiErrorCode.AI_502_003);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            log.error("HCX 응답 JSON 파싱 실패 - model={}, reqId={}, 원인={}", model, requestId, e.getMessage());
            throw new ApiException(AiErrorCode.AI_502_003);
        }

        // 성공 판정: status.code == "20000". 그 외 → 실패(원문/필터 사유는 서버 로그만).
        String statusCode = root.path("status").path("code").asText("");
        if (!STATUS_OK.equals(statusCode)) {
            log.error("HCX status.code 비정상 - model={}, reqId={}, code={}", model, requestId, statusCode);
            throw new ApiException(AiErrorCode.AI_502_003);
        }

        JsonNode result = root.path("result");
        String combined = result.path("message").path("content").asText("");
        long inputTokens = result.path("usage").path("promptTokens").asLong(0L);
        long outputTokens = result.path("usage").path("completionTokens").asLong(0L);
        String finishReason = result.path("finishReason").asText("");

        log.info("HCX 호출 완료 - model={}, finishReason={}, images={}, inputTokens={}, outputTokens={}",
            model, finishReason, imageCount, inputTokens, outputTokens);

        // ★refusal 개념 없음(false 고정), 캐시 토큰 없음(0). 빈/이상 content 는 상위 파싱 단계에서 처리.
        return new LlmRawResponse(false, combined, inputTokens, outputTokens, 0L, 0L);
    }

    /** 요청 본문 조립(HCX 계약: messages + 샘플링 파라미터 + stop/includeAiFilters). */
    private Map<String, Object> buildBody(List<Map<String, Object>> messages) {
        AiProperties.Llm llm = aiProperties.getLlm();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messages", messages);
        body.put("topP", llm.getTopP());
        body.put("topK", llm.getTopK());
        body.put("maxTokens", llm.getMaxTokens());       // ★≤4096 강제(getter 클램프)
        body.put("temperature", llm.getTemperature());
        body.put("repetitionPenalty", llm.getRepetitionPenalty());  // ★필드명 정확(repeatPenalty 아님)
        body.put("stop", List.of());
        body.put("includeAiFilters", false);             // MVP: 콘텐츠 필터 미적용
        return body;
    }

    /** 텍스트 파트({type:text, text}). */
    private Map<String, Object> textPart(String text) {
        Map<String, Object> part = new LinkedHashMap<>();
        part.put("type", "text");
        part.put("text", (text == null) ? "" : text);
        return part;
    }

    /**
     * 이미지 파트({type:image_url, dataUri:{data:"data:<mediatype>;base64,<base64>"}}).
     * ★HCX dataUri.data 는 <b>data URI 형식</b>(data:image/jpeg;base64,...)을 요구한다.
     *    순수 base64 를 넣으면 400(status.code 40001 Invalid parameter)로 거부된다(E2E 로 확인).
     *    mediaType 은 상위 FileService 가 확장자 화이트리스트로 검증해 넘긴 값(image/jpeg|png|webp).
     *
     * <p>★HCX Vision 정규화(40063 Invalid image size 방지): dataUri 조립 전 {@link #normalizeForHcx}로
     *    긴 변 상한(기본 2000px)에 맞춰 다운스케일한다. answerWithImages/chat 두 경로가 모두 여기를 지나므로
     *    위험성평가·TBM 전 도메인에 단일 초크포인트로 적용된다.
     */
    private Map<String, Object> imagePart(String base64, String mediaType) {
        String mt = (mediaType == null || mediaType.isBlank()) ? "image/jpeg" : mediaType;
        String[] normalized = normalizeForHcx(base64, mt);
        String outBase64 = normalized[0];
        String outMediaType = normalized[1];
        Map<String, Object> dataUri = new LinkedHashMap<>();
        dataUri.put("data", "data:" + outMediaType + ";base64," + outBase64);
        Map<String, Object> part = new LinkedHashMap<>();
        part.put("type", "image_url");
        part.put("dataUri", dataUri);
        return part;
    }

    /**
     * HCX Vision 전송 직전 이미지 정규화(다운스케일). {40063 Invalid image size} 방지.
     *
     * <p>HCX-005 Vision 한도(긴 변 ≤ 2240px)에 맞춰, 긴 변이 config 상한({@code visionMaxImageSide}, 기본 2000)을
     *    초과하면 비율 유지로 축소해 JPEG 로 재인코딩한다. 상한 이하면 원본을 그대로 반환(재인코딩 안 함, 화질 보존).
     *
     * <p>디코딩 폭탄 방어: 먼저 헤더만 읽어(width/height) 픽셀 수를 확인한다. 전체 디코드는 상한 초과 시에만 수행한다.
     *    픽셀 수가 하드캡({@code maxDecodeMegapixels}, 기본 60MP)을 넘으면 전체 디코드 없이 AI_400_008 로 거부한다.
     *
     * <p>best-effort: 리더 부재(webp 등 ImageIO 미지원)·헤더 판독 실패·재인코딩 실패는 정규화를 스킵하고
     *    원본을 반환한다(기능 자체를 죽이지 않는다). 단 하드캡 초과(AI_400_008)만 명시적으로 throw 한다.
     *
     * @param base64    순수 base64(접두어 없음)
     * @param mediaType 원본 media type(image/jpeg|png|webp 등)
     * @return [정규화된 base64, media type] 2요소 배열. 재인코딩 시 media type 은 "image/jpeg".
     */
    private String[] normalizeForHcx(String base64, String mediaType) {
        if (base64 == null || base64.isBlank()) {
            return new String[] {base64, mediaType};
        }

        int visionMaxSide = aiProperties.getLlm().getVisionMaxImageSide();
        long maxMegapixels = aiProperties.getLlm().getMaxDecodeMegapixels();

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            // base64 손상 → 정규화 스킵(원본 반환). 실제 전송 오류는 HCX 가 판정.
            log.debug("HCX Vision 정규화 스킵 - base64 디코드 실패");
            return new String[] {base64, mediaType};
        }

        // 1) 헤더만으로 width/height 판독(전체 디코드 없이).
        int width;
        int height;
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (iis == null) {
                return new String[] {base64, mediaType};
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                // ImageIO 미지원 포맷(webp 등) → best-effort 스킵.
                log.debug("HCX Vision 정규화 스킵 - 지원 리더 없음(mediaType={})", mediaType);
                return new String[] {base64, mediaType};
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                width = reader.getWidth(0);
                height = reader.getHeight(0);
            } finally {
                reader.dispose();
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.debug("HCX Vision 정규화 스킵 - 헤더 판독 실패: {}", e.getMessage());
            return new String[] {base64, mediaType};
        }

        if (width <= 0 || height <= 0) {
            return new String[] {base64, mediaType};
        }

        // 2) 디코딩 폭탄 하드캡: 헤더 픽셀 수가 상한 초과 → 전체 디코드 없이 거부.
        long megapixels = (long) width * (long) height;
        if (megapixels > maxMegapixels) {
            log.warn("HCX Vision 이미지 해상도 하드캡 초과 - {}x{}({}px) > {}px",
                width, height, megapixels, maxMegapixels);
            throw new ApiException(AiErrorCode.AI_400_008);
        }

        // 3) 긴 변이 상한 이하 → 원본 그대로(재인코딩 안 함).
        int longSide = Math.max(width, height);
        if (longSide <= visionMaxSide) {
            return new String[] {base64, mediaType};
        }

        // 4) 초과 → 전체 디코드 후 비율 유지 다운스케일 → JPEG 재인코딩.
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(bytes));
            if (src == null) {
                log.debug("HCX Vision 정규화 스킵 - 전체 디코드 결과 null");
                return new String[] {base64, mediaType};
            }
            double scale = (double) visionMaxSide / (double) longSide;
            int newW = Math.max(4, (int) Math.floor(width * scale));
            int newH = Math.max(4, (int) Math.floor(height * scale));

            BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            try {
                // 투명 PNG 등 알파 채널 대비 흰 배경으로 채운 뒤 그린다(JPEG 는 알파 미지원).
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, newW, newH);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(src, 0, 0, newW, newH, null);
            } finally {
                g.dispose();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (!ImageIO.write(scaled, "jpg", baos)) {
                log.debug("HCX Vision 정규화 스킵 - JPEG 인코딩 writer 없음");
                return new String[] {base64, mediaType};
            }
            String outBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            // 종횡비 1:5~5:1 초과는 스케일로 못 고침 → 경고만(throw 하지 않음).
            int longAfter = Math.max(newW, newH);
            int shortAfter = Math.min(newW, newH);
            if (shortAfter > 0 && longAfter > shortAfter * 5) {
                log.warn("HCX Vision 종횡비 한도(1:5~5:1) 초과 가능 - {}x{}", newW, newH);
            }
            log.info("HCX Vision 이미지 다운스케일 - {}x{} -> {}x{}(image/jpeg)",
                width, height, newW, newH);
            return new String[] {outBase64, "image/jpeg"};
        } catch (Exception e) {
            // 재인코딩 실패는 기능을 죽이지 않는다 → 원본 반환(best-effort).
            log.warn("HCX Vision 정규화 실패 - 원본 전송으로 폴백: {}", e.getMessage());
            return new String[] {base64, mediaType};
        }
    }

    /** 메시지 1건({role, content:[...]}) — content 는 항상 배열. */
    private Map<String, Object> message(String role, List<Map<String, Object>> content) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }
}
