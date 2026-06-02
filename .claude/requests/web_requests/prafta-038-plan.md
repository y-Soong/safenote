# PRAFTA-038 작업 분해 (Baim_01 사업장 주소 필수화 + LAT/LON 적재)

> 영역: web · 모듈: baim/baim01 (사업장 관리)
> 요청서: `.claude/requests/web_requests/prafta-038.md`
> 정책서 출처: 공통 정책서 v1.1 §6.1(사업장 구성 요소), §6.2(GPS 허용범위)
> 작성: planner (코드 수정 없음 / 분해·명세만)

---

## 0. 요청 요지

1. 사업장 정보 **생성 시 주소 정보를 필수 입력**으로 변경.
2. 입력 주소 기반으로 **`TB_SITE.LAT / LON` 데이터 적재**(지오코딩).

확정 스키마(메인 세션 운영 DB MCP 확인): `TB_SITE.LAT decimal(10,7)`, `LON decimal(10,7)` **컬럼 이미 존재**(nullable). → **컬럼 추가 마이그레이션 불필요, 데이터 적재만 필요.**

---

## 1. 현행 분석

### 1-1. 화면 (생성/수정은 팝업에서 수행)
- 목록 화면: `prafta-web-frontend/.../src/views/baim/Baim_01.vue`
  - 그리드 조회 + 인라인 useYn 토글. 생성/수정은 **행 더블클릭 / 생성 버튼 → `SiteInfoPop.vue` 팝업**에서 처리.
  - 목록 자체 `fnSave`는 인라인 변경분(주로 useYn) 저장용.
- 생성/수정 팝업: `prafta-web-frontend/.../src/views/baim/popup/SiteInfoPop.vue`
  - 주소 입력 UI **이미 존재**: 우편번호(disabled) + [주소찾기] 버튼 + 기본주소(disabled) + 상세주소(입력 가능).
  - 주소찾기: `@/utils/addrUtil.js`의 `fnSearchAddress(zipCode, addr1, addr2)` → **Daum Postcode** 팝업. 선택 시 `zonecode→zipCode`, `address→addr1`, `addr2=""`.
  - **Daum Postcode SDK는 `index.html`에 전역 로드**(`postcode.v2.js`).
  - 지도: `#kakao-map` 컨테이너. **Kakao Maps SDK(`libraries=services`)를 동적 로드**하고 `kakao.maps.services.Geocoder()`로 `addressSearch(addr1, ...)` 호출. 콜백에서 **`result[0].y`(=위도), `result[0].x`(=경도)를 이미 획득**하지만 → **현재는 마커/반경 원 표시에만 사용하고 좌표를 저장 payload에 넣지 않음.** (LAT/LON 미적재의 직접 원인)
  - Kakao JS 키: `import.meta.env.VITE_PUBLIC_KAKAO_APP_JS_KEY` (env로 이미 주입됨, services 라이브러리 사용 중).
  - 저장: `fnSiteSave` → `POST /webApi/baim01/save-site-infos` (배열 1건). payload에 `addr1/addr2/zipCode/gpsRange` 등은 포함하나 **`lat/lon` 없음**.
  - 검증 `fnSiteInfoValidationChk`: 사업장명 필수 → **`zipCode` 또는 `addr1` 빈 값이면 "주소를 입력해주세요" 차단(이미 주소 필수 검증 존재)** → 관리자 계정 필수. 단, **상세주소(addr2)는 검증하지 않음.**

### 1-2. 백엔드 (insert/update 경로)
- Controller: `Baim01Controller.saveSiteInfo` → `POST /baim01/save-site-infos`, body `List<SiteInfoRequest>`.
- DTO: `SiteInfoRequest` — `cmpnyCd, siteCd, siteNo, siteNm, addr1, addr2, zipCode, strDate, endDate, useYn, siteAdminCd, telNo, gpsRange, siteDesc`. **`lat/lon` 필드 없음.** (Bean Validation 어노테이션 전혀 없음)
- Param/Model/Command: `SiteInfoParam` → `SiteInfoModel`(record) → `SiteInfoCommand`(record). 셋 다 동일 필드 셋, **`lat/lon` 없음.**
- Service: `Baim01ServiceImpl.saveSiteInfo` — siteCd null이면 신규 채번(`selectSiteCd`), 노드 생성 → `mergeSiteInfo`(upsert) → 마스터 권한 부여.
- Mapper: `Baim01Mapper.xml`
  - `mergeSiteInfo`: `TB_SITE` INSERT ... ON DUPLICATE KEY UPDATE. **컬럼 목록/VALUES/UPDATE 절에 `LAT`, `LON` 없음.** (적재 누락의 직접 원인)
  - `selectSiteInfoList`: 조회 컬럼에 `LAT/LON` 없음(수정 팝업 재진입 시 좌표 복원하려면 추가 필요 가능).

### 1-3. 기존 지오코딩/주소검색 선례 (grep 결과)
- **있음.** 동일 화면(`SiteInfoPop.vue`)에 이미:
  - 주소검색: **Daum Postcode**(전역 SDK, `addrUtil.js`).
  - 지오코딩: **Kakao Maps `services.Geocoder.addressSearch`** (JS 키 env 주입, 동적 로드). 좌표를 이미 산출 중(마커용).
- 앱 프론트(`prafta-app-frontend/src/utils/addrUtil.js`)에도 동일 패턴 존재(웹과 별개, 본 작업 범위 아님).
- **백엔드 측 외부 지오코딩 REST 호출 선례는 없음**(서버에서 주소→좌표 변환 코드 없음).

> 결론: 클라이언트 지오코딩 인프라가 이미 갖춰져 있고 좌표까지 산출하고 있으므로, **"산출된 좌표를 저장 payload에 실어 BE가 그대로 적재"** 가 가장 적은 변경·재사용 최대 경로.

---

## 2. 현행 → 목표 변경표

| 구분 | 위치 | 현행 | 목표 |
| --- | --- | --- | --- |
| 주소 필수(FE) | `SiteInfoPop.vue` `fnSiteInfoValidationChk` | zipCode+addr1 빈값 차단 **이미 있음** | 유지. (상세주소addr2 필수 여부는 §5 질문) |
| 주소 필수(BE) | `SiteInfoRequest` | 검증 없음 | `@NotBlank` addr1/zipCode 추가 + `@Valid` 적용(생성/수정 공통, §5 질문) |
| 좌표 산출(FE) | `SiteInfoPop.vue` geocoder 콜백 | `result[0].y/x`를 마커에만 사용 | 산출 좌표를 `lat/lon` ref에 보관 |
| 좌표 전송(FE) | `fnSiteSave` payload | lat/lon 없음 | payload에 `lat`, `lon` 추가 |
| DTO/Model/Command(BE) | 3개 record/DTO | lat/lon 없음 | `lat`, `lon`(String 또는 BigDecimal) 필드 추가 |
| 적재(BE) | `Baim01Mapper.mergeSiteInfo` | LAT/LON 미포함 | INSERT 컬럼/VALUES + ON DUP UPDATE에 `LAT=#{lat}`, `LON=#{lon}` 추가 |
| 조회(BE, 선택) | `Baim01Mapper.selectSiteInfoList` + `SiteInfoResult` | LAT/LON 없음 | (수정 재진입 시 지도 복원 정확도용) LAT/LON 조회 추가 — §5 질문 |

> DTO 컬럼/타입 사실: `LAT/LON`은 DB `decimal(10,7)`. DTO는 프로젝트 관례상 String 바인딩 또는 BigDecimal. (developer가 매핑 결정, 추측 금지 — 본 plan은 컬럼명 `LAT/LON`만 사실로 사용.)

---

## 3. 지오코딩 설계 옵션 비교

| 옵션 | 좌표 산출 위치 | 제공자/키 | 장점 | 단점 |
| --- | --- | --- | --- | --- |
| **A. 클라이언트 산출 + 저장 시 전송 (권장)** | FE(Kakao Geocoder) | **Kakao Maps JS 키(이미 env 주입, 이미 사용 중)** | 신규 의존성/키 0개. geocoder 콜백이 이미 좌표 산출 중 → ref 보관 + payload 추가만. BE는 단순 적재. | FE가 보낸 좌표를 BE가 신뢰(위변조 가능하나 사업장 등록은 master/관리자 권한). 주소 선택 직후 비동기 geocode 완료 타이밍 관리 필요. |
| B. 서버 지오코딩 | BE(외부 REST) | 신규 키 필요(Kakao Local REST / Naver / VWorld / juso 중 택1) — **현재 BE에 키·연동 없음** | FE 신뢰 불필요, 단일 적재 경로 | 신규 외부연동·키 발급·application.yml 시크릿·HTTP client·에러/타임아웃·rate limit 전부 신규. 작업량 大. 비대화형 키 발급 사용자 의존. |
| C. 혼합(FE 산출, BE 검증/보정) | FE 1차 + BE 재호출 | FE Kakao JS + BE REST 키 둘 다 | 정합성 최상 | 가장 무거움. 현 요구(단순 적재) 대비 과설계. |

**planner 권장: 옵션 A.**
근거: (1) Kakao geocoder가 이미 동작·좌표 산출 중(`result[0].y/x`), (2) JS 키 이미 주입, (3) BE 신규 외부연동·시크릿 0, (4) 요청 요지가 "입력 주소 기반 LAT/LON 적재"로 단순 적재 수준. 옵션 B는 신규 키 발급 의존성 때문에 자율 진행 불가 지점이 생김.

### 옵션 A 좌표 저장 시점 흐름
1. [주소찾기] → Daum Postcode 선택 → `addr1` 세팅.
2. `watch(addr1)` → Kakao `geocoder.addressSearch(addr1)` 콜백에서 `result[0].y(lat)/x(lon)` 산출 → 마커 + **`lat`/`lon` ref 보관(신규)**.
3. 저장 시 payload에 `lat/lon` 포함 → BE `mergeSiteInfo`가 `TB_SITE.LAT/LON` 적재.
4. 엣지: geocode 실패/미완료 시 lat/lon 빈 값 → BE는 NULL 적재(컬럼 nullable). FE 저장 차단 여부는 §5 질문.

---

## 4. developer 작업 단위

> ID 채번은 메인 세션이 Notion "작업 로그" 최대 ID +1로 확정(본 plan은 의존관계·범위만 정의). 아래는 분해 단위.

### 단위 ① [backend] TB_SITE LAT/LON 적재 + 주소 서버검증
- 유형: 보완 · 영역 web · 모듈 baim/baim01
- 정책서 출처: 공통 §6.1(주소·좌표 필수/기준), §6.2(GPS 반경 중심좌표)
- 핵심 요구사항:
  1) `SiteInfoRequest`에 `lat`, `lon` 필드 추가 + `addr1`/`zipCode`(및 §5에 따라 addr2) `@NotBlank`, Controller `@Valid @RequestBody List<@Valid ...>` 적용.
  2) `SiteInfoModel`, `SiteInfoCommand`(+`from` 매핑) `record`에 `lat`, `lon` 전파.
  3) `Baim01Mapper.xml mergeSiteInfo` INSERT 컬럼/VALUES/ON DUP UPDATE에 `LAT=#{lat}`, `LON=#{lon}` 추가.
  4) (§5 결정 시) `selectSiteInfoList` + `SiteInfoResult`에 `LAT/LON` 조회 추가.
- 영향 파일:
  - `prafta-backend/.../baim01/dto/request/SiteInfoRequest.java`
  - `prafta-backend/.../baim01/application/model/SiteInfoModel.java`
  - `prafta-backend/.../baim01/application/command/SiteInfoCommand.java`
  - `prafta-backend/.../baim01/application/param/SiteInfoParam.java` (매핑 전파)
  - `prafta-backend/.../baim01/controller/Baim01Controller.java` (@Valid)
  - `prafta-backend/src/main/resources/com/prafta/web/baim/baim01/mapper/Baim01Mapper.xml`
  - (선택) `.../baim01/result/SiteInfoResult.java`
- 영향 endpoint: `POST /baim01/save-site-infos`, (선택) `GET /baim01/site-info-lists`
- 산출물: DTO/record/Controller/Mapper.xml 수정. **DB 마이그레이션 없음(컬럼 기존재).**
- 선행: 없음 (단위 ②와 payload 계약만 합의)

### 단위 ② [frontend-screen] SiteInfoPop 좌표 산출 보관 + 저장 전송 + 필수화 확정
- 유형: 보완 · 영역 web · 모듈 baim
- 정책서 출처: 공통 §6.1
- 핵심 요구사항:
  1) Kakao geocoder 콜백(`updateMapLocation`)에서 산출한 `result[0].y/x`를 신규 `lat`/`lon` ref에 보관.
  2) `fnSiteSave` payload에 `lat`, `lon` 추가.
  3) 주소 필수 검증 유지/강화(현재 zipCode+addr1 차단 있음; addr2 필수 여부는 §5 반영).
  4) (BE에서 LAT/LON 조회 추가 시) `fnGetSiteInfo`에서 lat/lon 복원하여 지도 정확도 향상(선택).
- 영향 파일: `prafta-web-frontend/.../src/views/baim/popup/SiteInfoPop.vue`
- 연결 UI 명세: **신규 화면 아님(기존 팝업 보완)** → 별도 UI-xxx 미발행, 기존 도메인 지식 베이스 항목 보완 기록 권장.
- 산출물: `SiteInfoPop.vue` (developer가 script 보완)
- 선행: 단위 ①과 payload 필드명(`lat`/`lon`) 계약 합의 필요(병렬 가능, 계약만 선합의)

> Baim_01.vue 목록의 인라인 `fnSave`(useYn 위주)는 본 요구 범위 아님(주소/좌표는 팝업에서만 입력). 단, 동일 endpoint를 공유하므로 BE에 @NotBlank 추가 시 **인라인 저장 payload에도 addr1/zipCode가 포함되어 전송되는지** developer가 확인(현재 목록 행 객체는 조회결과라 addr1/zipCode 보유 → 통과 예상이나 회귀 확인 필요). → §5 질문.

---

## 5. 사용자 확인 질문 목록 (메인 세션 → 사용자)

1. **지오코딩 제공자/키 (가장 중요)**: planner 권장은 **옵션 A(클라이언트 Kakao Geocoder 산출 → 저장 시 전송)**. 이미 Kakao JS 키가 env에 주입되어 있고 geocoder가 좌표를 산출 중이라 신규 키 발급이 불필요합니다. 서버 지오코딩(옵션 B)으로 가려면 Kakao Local REST/Naver/VWorld/juso 중 하나의 **신규 REST 키 발급·시크릿 등록**이 필요합니다. **옵션 A로 진행해도 될까요?** (B를 원하면 어느 제공자인지 + 키 제공 필요)

2. **적용 범위(생성 vs 수정)**: 요청서는 "생성 시" 주소 필수이나, `mergeSiteInfo`는 생성/수정 공통 upsert이고 BE `@NotBlank`는 둘 다에 걸립니다. **수정 시에도 주소 필수로 강제할까요?** (기존 데이터에 주소 없는 사업장이 있으면 수정 저장이 막힐 수 있음)

3. **필수 필드 범위**: 공통 정책서 §6.1은 "주소 / 상세주소 필수"로 기재. 현행 검증은 **우편번호(zipCode)+기본주소(addr1)만** 필수, 상세주소(addr2)는 선택입니다. **상세주소(addr2)도 필수로 할까요, 아니면 zipCode+addr1만 필수로 둘까요?** (정책서 문구 vs 현행 동작 충돌 지점)

4. **좌표 산출 실패 시 동작**: 주소는 정상인데 Kakao geocode가 좌표를 못 찾는 경우(또는 SDK 로드 실패) → (a) LAT/LON NULL로 저장 허용, (b) 저장 차단하고 재시도 안내. **어느 쪽으로 처리할까요?** (LAT/LON 컬럼 nullable이라 (a) 기술적으로 가능)

5. **목록 인라인 저장 회귀**: Baim_01.vue 목록의 useYn 인라인 저장도 같은 endpoint(`save-site-infos`)를 씁니다. BE에 addr1/zipCode @NotBlank를 추가하면 인라인 저장에도 동일 검증이 걸립니다. 조회결과 행에는 addr1/zipCode가 있어 통과가 예상되나, **인라인 저장에도 주소 검증을 적용하는 것이 맞는지(또는 생성 전용 별도 검증 경로가 필요한지)** 확인 부탁드립니다.

---

## 6. 비고
- DB 마이그레이션 없음 (`LAT/LON` 컬럼 기존재, schema-full.sql 스냅샷 대신 메인 세션 MCP 확인값을 사실로 사용).
- 보안/IDOR 검토(다른 회사 사업장 저장 등)는 security 에이전트 영역 — 본 분해 범위 아님(단, ②/① 모두 `gv_cmpnyCd` 토큰 기반이라 기존 가드 유지).
- 코드 수정은 본 plan에서 수행하지 않음(분해/명세만).
