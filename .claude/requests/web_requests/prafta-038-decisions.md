# prafta-038 확정 결정 (단일 출처)

> 분해: prafta-038-plan.md. 본 문서는 사용자 확정 답변. developer/qa/security 는 이 결정을 사실로 따른다.

## 결정
- **D1 지오코딩 방식**: 옵션 A — 클라이언트 Kakao Maps Geocoder(`services.Geocoder.addressSearch`)가 산출한 좌표를 저장 payload(`lat`/`lon`)에 실어 BE가 `TB_SITE.LAT/LON` 에 그대로 적재. **신규 REST 키·외부연동·DB 마이그레이션 없음**(LAT/LON 컬럼 기존재).
- **D2 필수화 적용 범위**: 주소 필수를 **생성 + 수정 모두** 적용. → BE `SiteInfoRequest` 에 `@NotBlank`(addr1, zipCode) + Controller `@Valid` (생성/수정 공통 upsert 경로에 모두 적용).
  - ⚠️ 동일 endpoint(`POST /baim01/save-site-infos`)를 목록 인라인 저장(useYn 토글)도 공유한다. 따라서 인라인 저장에도 addr1/zipCode 검증이 걸린다. 조회행에 주소가 있으면 통과하나, **주소 없는 레거시 사업장은 수정/인라인 저장 시 주소 입력이 강제된다**(의도된 동작 — 모든 사업장이 좌표 산출용 주소를 갖도록).
- **D3 필수 필드 범위**: **우편번호(zipCode) + 기본주소(addr1) 필수, 상세주소(addr2) 선택**. (공통정책 §6.1 "상세주소 필수" 문구와 다르나 사용자 확정 = addr2 선택)
- **D4 좌표 산출 실패 시**: 주소는 정상인데 geocode 가 좌표를 못 얻으면 → **저장 허용(LAT/LON NULL)**, 단 사용자에게 경고 노출.
  - 경고 문구(필수 포함): **"출퇴근 유효범위 체크가 안됩니다. 관리자에게 문의해주세요."**
  - 웹 Baim01 팝업 저장 시: 좌표가 비어 있으면 위 문구를 포함한 경고를 보여주고 저장은 진행(NULL 적재).
  - 연계(app-003): 앱 출퇴근 시 해당 사업장에 LAT/LON 이 없으면 유효범위(지오펜스) 판정 불가 → 동일 문구 노출. (app-003 범위에 요구사항으로 추가)

## developer 작업 단위 (병렬, 계약 필드명 `lat`/`lon`)
- 단위①[backend]: `SiteInfoRequest`(+`@NotBlank` addr1/zipCode, `lat`/`lon` 필드), `SiteInfoParam`/`SiteInfoModel`/`SiteInfoCommand`(record) 에 lat/lon 전파, `Baim01Controller` `@Valid`, `Baim01Mapper.xml mergeSiteInfo` INSERT/VALUES/ON DUP UPDATE 에 `LAT/LON`, `selectSiteInfoList`+`SiteInfoResult` 에 LAT/LON 조회(수정 재진입 지도 복원). DB 마이그레이션 없음.
- 단위②[frontend]: `SiteInfoPop.vue` — geocoder 콜백 좌표를 `lat`/`lon` ref 보관, `fnSiteSave` payload 에 추가, 저장 시 좌표 비면 D4 경고, (BE 조회 추가 시) 수정 재진입 좌표 복원.
