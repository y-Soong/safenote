mport com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.RandomStringUtils;



import java.io.\*;

import java.net.HttpURLConnection;

import java.net.URL;

import java.nio.charset.StandardCharsets;

import java.util.\*;



public class RequestService {

&#x20;   private static final Integer TIME\_OUT = 5000;

&#x20;   private static final String API\_KEY = "{연동 개발 인증키}";

&#x20;   private static final String PPURIO\_ACCOUNT = "{뿌리오 계정}";

&#x20;   private static final String FROM = "{발신 번호}";

&#x20;   private static final String FILE\_PATH = "{MMS 발송인 경우 첨부할 이미지 경로}";

&#x20;   private static final String URI = "https://message.ppurio.com";



&#x20;   public void requestSend() {

&#x20;       String basicAuthorization = Base64.getEncoder().encodeToString((PPURIO\_ACCOUNT + ":" + API\_KEY).getBytes());



&#x20;       Map<String, Object> tokenResponse = getToken(URI, basicAuthorization); // 토큰 발급

&#x20;       Map<String ,Object> sendResponse = send(URI, (String) tokenResponse.get("token")); // 발송 요청



&#x20;       System.out.println(sendResponse.toString());

&#x20;   }



&#x20;   public void requestCancel() {

&#x20;       String basicAuthorization = Base64.getEncoder().encodeToString((PPURIO\_ACCOUNT + ":" + API\_KEY).getBytes());



&#x20;       Map<String, Object> tokenResponse = getToken(URI, basicAuthorization); // 토큰 발급

&#x20;       Map<String, Object> cancelResponse = cancel(URI, (String) tokenResponse.get("token")); // 예약 취소 요청



&#x20;       System.out.println(cancelResponse.toString());

&#x20;   }



&#x20;   /\*\*

&#x20;    \* Access Token 발급 요청 (한 번 발급된 토큰은 24시간 유효합니다.)

&#x20;    \* @param baseUri 요청 URI ex) https://message.ppurio.com

&#x20;    \* @param BasicAuthorization "계정:연동 개발 인증키"를 Base64 인코딩한 문자열

&#x20;    \* @return Map

&#x20;    \*/

&#x20;   private Map<String, Object> getToken(String baseUri, String BasicAuthorization) {

&#x20;       HttpURLConnection conn = null;

&#x20;       try {

&#x20;           // 요청 파라미터 생성

&#x20;           Request request = new Request(baseUri + "/v1/token", "Basic " + BasicAuthorization);



&#x20;           // 요청 객체 생성

&#x20;           conn = createConnection(request);



&#x20;           // 응답 데이터 객체 변환

&#x20;           return getResponseBody(conn);

&#x20;       } catch (IOException e) {

&#x20;           throw new RuntimeException("API 요청과 응답 실패", e);

&#x20;       } finally {

&#x20;           if (conn != null) {

&#x20;               conn.disconnect();

&#x20;           }

&#x20;       }

&#x20;   }



&#x20;   /\*\*

&#x20;    \* 문자 발송 요청

&#x20;    \* @param baseUri 요청 URI ex) https://message.ppurio.com

&#x20;    \* @param accessToken 토큰 발급 API를 통해 발급 받은 Access Token, 유효기간이 1일이기 때문에 만료될 경우 재발급 필요

&#x20;    \* @return Map

&#x20;    \*/

&#x20;   private Map<String, Object> send(String baseUri, String accessToken) {

&#x20;       HttpURLConnection conn = null;

&#x20;       try {

&#x20;           // 요청 파라미터 생성

&#x20;           String bearerAuthorization = String.format("%s %s", "Bearer", accessToken);

&#x20;           Request request = new Request(baseUri + "/v1/message", bearerAuthorization);



&#x20;           // 요청 객체 생성

&#x20;           conn = createConnection(request, createSendTestParams());// sms 발송 테스트



&#x20;           // 응답 데이터 객체 변환

&#x20;           return getResponseBody(conn);

&#x20;       } catch (IOException e) {

&#x20;           throw new RuntimeException("API 요청과 응답 실패", e);

&#x20;       } finally {

&#x20;           if (conn != null) {

&#x20;               conn.disconnect();

&#x20;           }

&#x20;       }

&#x20;   }



&#x20;   /\*\*

&#x20;    \* 예약발송 취소 요청

&#x20;    \* @param baseUri 요청 URI ex) https://message.ppurio.com

&#x20;    \* @param accessToken 토큰 발급 API를 통해 발급 받은 Access Token, 유효기간이 1일이기 때문에 만료될 경우 재발급 필요

&#x20;    \* @return Map

&#x20;    \*/

&#x20;   private Map<String, Object> cancel(String baseUri, String accessToken) {

&#x20;       HttpURLConnection conn = null;

&#x20;       try {

&#x20;           // 요청 파라미터 생성

&#x20;           String token = String.format("%s %s", "Bearer", accessToken);

&#x20;           Request request = new Request(baseUri + "/v1/cancel", token);



&#x20;           // 요청 객체 생성

&#x20;           conn = createConnection(request, createCancelTestParams());// 예약 취소 테스트



&#x20;           // 응답 데이터 객체 변환

&#x20;           return getResponseBody(conn);

&#x20;       } catch (IOException e) {

&#x20;           throw new RuntimeException("API 요청과 응답 실패", e);

&#x20;       } finally {

&#x20;           if (conn != null) {

&#x20;               conn.disconnect();

&#x20;           }

&#x20;       }

&#x20;   }



&#x20;   private <T> HttpURLConnection createConnection(Request request, T requestObject) throws IOException {

&#x20;       ObjectMapper objectMapper = new ObjectMapper();

&#x20;       String jsonInputString = objectMapper.writeValueAsString(requestObject);

&#x20;       // 요청 객체 생성

&#x20;       HttpURLConnection connect = createConnection(request);

&#x20;       connect.setDoOutput(true); // URL 연결을 출력용으로 사용(true)

&#x20;       // 요청 데이터 처리

&#x20;       try (OutputStream os = connect.getOutputStream()) {

&#x20;           byte\[] input = jsonInputString.getBytes(StandardCharsets.UTF\_8);

&#x20;           os.write(input, 0, input.length);

&#x20;       } catch (Exception e) {

&#x20;           throw new RuntimeException(e);

&#x20;       }

&#x20;       return connect;

&#x20;   }



&#x20;   private HttpURLConnection createConnection(Request request) throws IOException {

&#x20;       URL url = new URL(request.getRequestUri());

&#x20;       HttpURLConnection conn = (HttpURLConnection) url.openConnection();

&#x20;       conn.setRequestMethod("POST");

&#x20;       conn.setRequestProperty("Content-Type", "application/json");

&#x20;       conn.setRequestProperty("Authorization", request.getAuthorization()); // Authorization 헤더 입력

&#x20;       conn.setConnectTimeout(TIME\_OUT); // 연결 타임아웃 설정(5초)

&#x20;       conn.setReadTimeout(TIME\_OUT); // 읽기 타임아웃 설정(5초)

&#x20;       return conn;

&#x20;   }



&#x20;   private Map<String, Object> getResponseBody(HttpURLConnection conn) {

&#x20;       InputStream inputStream;



&#x20;       if (conn.getResponseCode() == 200) { // 요청 성공

&#x20;           inputStream = conn.getInputStream();

&#x20;       } else { // 서버에서 요청은 수신했으나 특정 이유로 인해 실패함

&#x20;           inputStream = conn.getErrorStream();

&#x20;       }



&#x20;       try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF\_8))) {

&#x20;           String inputLine;

&#x20;           StringBuilder responseBody = new StringBuilder();

&#x20;           while ((inputLine = br.readLine()) != null) {

&#x20;               responseBody.append(inputLine);

&#x20;           }



&#x20;           // 성공 응답 데이터 변환

&#x20;           return convertJsonToMap(responseBody.toString());

&#x20;       } catch (IOException e) {

&#x20;           throw new RuntimeException("API 응답을 읽는 데 실패했습니다.", e);

&#x20;       }

&#x20;   }



&#x20;   private Map<String, Object> convertJsonToMap(String jsonString) throws JsonProcessingException {

&#x20;       ObjectMapper objectMapper = new ObjectMapper();

&#x20;       return objectMapper.readValue(jsonString, new TypeReference<>() {});

&#x20;   }



&#x20;   private Map<String, Object> createSendTestParams() throws IOException {

&#x20;       HashMap<String, Object> params = new HashMap<>();

&#x20;       params.put("account", PPURIO\_ACCOUNT);

&#x20;       params.put("messageType", "MMS");

&#x20;       params.put("from", FROM);

&#x20;       params.put("content", "\[\*이름\*], hello this is \[\*1\*]");

&#x20;       params.put("duplicateFlag", "Y");

&#x20;       params.put("rejectType", "AD"); // 광고성 문자 수신거부 설정, 비활성화할 경우 해당 파라미터 제외

&#x20;       params.put("targetCount", 1);

&#x20;       params.put("targets", List.of(

&#x20;               Map.of("to", "010XXXXXXXX",

&#x20;                       "name", "tester",

&#x20;                       "changeWord", Map.of(

&#x20;                               "var1", "ppurio api world")))

&#x20;       );

&#x20;       params.put("files", List.of(

&#x20;               createFileTestParams(FILE\_PATH)

&#x20;       ));

&#x20;       params.put("refKey", RandomStringUtils.random(32, true, true)); // refKey 생성, 32자 이내로 아무 값이든 상관 없음

&#x20;       return params;

&#x20;   }



&#x20;   private Map<String, Object> createFileTestParams(String filePath) throws RuntimeException, IOException {

&#x20;       FileInputStream fileInputStream = null;

&#x20;       try {

&#x20;           File file = new File(filePath);

&#x20;           byte\[] fileBytes = new byte\[ (int) file.length()];

&#x20;           fileInputStream = new FileInputStream(file);

&#x20;           int readBytes = fileInputStream.read(fileBytes);



&#x20;           if (readBytes != file.length()) {

&#x20;               throw new IOException();

&#x20;           }



&#x20;           String encodedFileData = Base64.getEncoder().encodeToString(fileBytes);



&#x20;           HashMap<String, Object> params = new HashMap<>();

&#x20;           params.put("size", file.length());

&#x20;           params.put("name", file.getName());

&#x20;           params.put("data", encodedFileData);

&#x20;           return params;

&#x20;       } catch (IOException e) {

&#x20;           throw new RuntimeException("파일을 가져오는데 실패했습니다.", e);

&#x20;       } finally {

&#x20;           if(fileInputStream != null) {

&#x20;               fileInputStream.close();

&#x20;           }

&#x20;       }

&#x20;   }



&#x20;   private Map<String, Object> createCancelTestParams() {

&#x20;       HashMap<String, Object> params = new HashMap<>();

&#x20;       params.put("account", PPURIO\_ACCOUNT);

&#x20;       params.put("messageKey", "230413110135117SMS029914servsUBn");

&#x20;       return params;

&#x20;   }

}



class Request {

&#x20;   private String requestUri;

&#x20;   private String authorization;



&#x20;   public Request(String requestUri, String authorization) {

&#x20;       this.requestUri = requestUri;

&#x20;       this.authorization = authorization;

&#x20;   }



&#x20;   public String getRequestUri() {

&#x20;       return requestUri;

&#x20;   }



&#x20;   public String getAuthorization() {

&#x20;       return authorization;

&#x20;   }

}

&#x20;   

