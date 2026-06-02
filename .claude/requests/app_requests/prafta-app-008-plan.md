# prafta-app-008 작업 분해 (planner)

> 출처 요청서: `.claude/requests/app_requests/prafta-app-008.md`
> 정책서: `attd/05-checkin-limits.md` §5.5 / `attd/07-checkin-checkout.md` §7.2~7.3
> 선행 자산: prafta-app-003 (checkIn/checkOut + 지오펜스 확정 모델)
> 분해일: 2026-05-29

---

## 0. 개요 / 두 파트

| 파트 | 내용 | 정책 출처 |
| --- | --- | --- |
| Part1 | §5.5 익일 경계 선행 구간 마감 규칙 (야간 교대 2구간 출근 분기 Case A/B/C) | `attd/05-checkin-limits.md` §5.5.1~§5.5.6 |
| Part2 | 지오펜스 범위 밖 출퇴근 시 지도+현위치 표시 + 외근 사유 작성 | `attd/07-checkin-checkout.md` §7.2~§7.3 |

두 파트 모두 prafta-app-003 의 `checkIn`/`checkOut` (`com.prafta.app.attd.attd01`) 확장이다. 신규 모듈/컨트롤러는 만들지 않고 기존 엔드포인트를 연장한다.

---

## 1. 핵심 사실 / 스키마·SDK 갭 (착수 전 필독)

### 1-1. ⚠️ §5.5 모델과 현행 스키마의 구조적 불일치 (Part1 최대 리스크)

요청서 §5.5 예시는 **"어제 23:00\~오늘 06:00 (1구간)"** / **"오늘 23:00\~내일 06:00 (2구간)"** 처럼 1구간과 2구간이 **서로 다른 캘린더 일자**에 걸친 야간 교대를 가정한다.

그러나 현행 코드(`AppAttd01ServiceImpl`)와 스키마는 1구간·2구간을 **동일 WORK_YMD 한 행**(`TB_SCH_MGMT` 의 `FST_SCH_*` / `SEC_SCH_*`)으로 보고, 근태 레코드도 같은 `WORK_YMD` 의 `WORK_SEQ`=1/2 로 적재한다. 즉 현 `checkIn` 의 1구간/2구간은 "같은 날 두 번 출근"이지 "어제·오늘 걸친 야간 교대"가 아니다.

→ 요청서가 말하는 "선행 1구간"이 다음 둘 중 무엇인지 **사용자 확정 필요(P1-D4)**:
- (해석 a) **같은 WORK_YMD** 내 WORK_SEQ=1(=현행 모델 그대로). 야간 여부는 `SEC_SCH_END < SEC_SCH_STR`(자정 넘김)로만 식별. → 현행 재출근 가드(§5.2)가 이미 Case B(미마감 차단)를 처리 중. 추가는 Case C(1구간 스킵=결번 출근) 허용 경로뿐.
- (해석 b) **전일 WORK_YMD** 의 미마감 1구간을 탐지(진짜 야간 교대 = 일자 분리). → 신규 mapper(전일 미마감 1구간 탐지) + 다음날 게이트(④ `countPastOpenAttd`)와의 충돌 정리 필요. 현재는 전날 미퇴근이 있으면 **무조건 출근 차단**(ATTD_400_082)이라 Case C 자체가 도달 불가.

본 분해는 해석을 강제하지 않고 두 해석 모두에 대응하는 단위로 쪼갠다(P1-D4 확정 후 실작업에서 택1).

### 1-2. ⚠️ 외근 사유 저장 컬럼 없음 (Part2, 메인 MCP 확인)
- `tb_user_attd_gps` / `tb_user_attd_mgmt` 어디에도 reason/memo/desc 컬럼 0건.
- GPS 행은 **외근일 때만** INSERT 되므로(GPS 행 존재 = 외근), 사유를 `tb_user_attd_gps.OFFSITE_REASON` 에 두는 것이 가장 자연스럽다(권장). → P2-D1 확정.
- 신규 컬럼 = ALTER 마이그레이션 필요(운영 미적용, SQL 파일만 산출).

### 1-3. ⚠️ 지도 SDK 미탑재 (Part2, 메인 MCP 확인)
- 앱 프론트에 Kakao Maps **JS SDK** 미탑재(prafta-038 은 Geocoder **REST** 만 사용, tb_site.LAT/LON 적재됨).
- Part2 "지도 표시" = Kakao Maps JS SDK 웹뷰 내 신규 로드 + JS 앱키 필요. → P2-D2 확정(SDK 채택/키 조달/로드 방식).

### 1-4. 확정 재사용 자산 (그대로 연장)
- 백엔드: `AppAttd01ServiceImpl.checkIn / checkOut`, `isOutsideGeofence`(haversine), `insertCheckInGps / insertCheckOutGps`, `isOffsite` 응답.
- GPS 브리지: `@/utils/gpsBridge` `requestGps()` (Flutter `GET_GPS`). 위치권한 하드게이트.
- 요청 DTO: `CheckInRequest` / `CheckOutRequest` (`{ lat, lon, accuracy, isMocked, workYmd }`).
- 호출처: `MyAttendanceView.vue` `submitCheckIn / submitCheckOut`, `onTodayAction`(checkIn/checkOut).

---

## 2. 단위표 (Part1 / Part2)

### Part1 — §5.5 야간 익일 경계 (APP008-N1x)

| ID | 유형 | 작업 요약 | 산출물(예상 파일) | 선행 | 사이즈 | 정책 출처 |
| --- | --- | --- | --- | --- | --- | --- |
| APP008-N11 | backend | 야간 스케줄·"선행 1구간" 식별 로직 추가 (`SEC_SCH_END < SEC_SCH_STR` 자정넘김 판정 + 선행 1구간 상태 분류 헬퍼). 해석 a/b 분기는 P1-D4 확정값 적용 | `AppAttd01ServiceImpl`(헬퍼 추가), (해석 b 시) `AppAttd01Mapper`+`*.xml`(전일 미마감 1구간 탐지 쿼리), 신규 enum/상수 | P1-D4 확정 | M | §5.5.4 |
| APP008-N12 | backend | `checkIn` 2구간 출근 분기 Case A/B/C 구현. A=즉시출근(기존), B=강한차단(신규 errorCode 또는 ATTD_400_081 재사용), C=신규 errorCode "확인 필요" 반환(소프트 차단) + `confirm` 플래그 수신 시 통과 | `AppAttd01ServiceImpl.checkIn`, `CheckInRequest`(+`confirmSkipPrevSlot` bool), `CheckInParam`, `AttdErrorCode`(+Case C 안내코드), mapper(필요 시) | APP008-N11 | M | §5.5.1~§5.5.3, §5.5.6 |
| APP008-N13 | frontend | Case C 확인 얼럿 처리. 서버가 "확인 필요" 코드 반환 시 `askConfirm("1구간 출근 데이터 없이 2구간 출근하는 게 맞나요?")` → 확인 시 `confirmSkipPrevSlot:true` 재호출. 얼럿 시점은 P1-D3 확정값 적용 | `MyAttendanceView.vue`(`submitCheckIn`/`onTodayAction` 확장) | APP008-N12 | S | §5.5.1(표), §5.5.6 |

> Case B 는 "강한 차단"이므로 프론트는 서버 메시지 그대로 노출(별도 UI 없음, 기존 errorCode 안내 경로 재사용). 신규 화면/모달 불필요.

### Part2 — 외근 지도 + 사유 (APP008-N2x)

| ID | 유형 | 작업 요약 | 산출물(예상 파일) | 선행 | 사이즈 | 정책 출처 |
| --- | --- | --- | --- | --- | --- | --- |
| APP008-N21 | backend | 외근 사유 저장 컬럼 추가 마이그레이션 (`tb_user_attd_gps.OFFSITE_REASON varchar(500)` 권장, P2-D1 확정값). 운영 미적용, SQL 파일만 | `.claude/requests/app_requests/prafta-app-008-offsite-reason.sql` | P2-D1 확정 | S | §7.2 |
| APP008-N22 | backend | `checkIn`/`checkOut` 에 `offsiteReason` 수신·저장. 외근(isOffsite=true) 경로에서만 GPS 행 INSERT 시 컬럼 채움. 미작성 차단 여부는 P2-D3 확정값 적용(차단 시 신규 errorCode) | `CheckInRequest`/`CheckOutRequest`(+`offsiteReason`), `CheckInParam`/`CheckOutParam`, `CheckInGpsCommand`/`CheckOutGpsCommand`(+OFFSITE_REASON), `AppAttd01Mapper.xml`(INSERT 컬럼), `AppAttd01ServiceImpl`(검증 분기) | APP008-N21, P2-D3 | M | §7.2~§7.3 |
| APP008-N23 | frontend | Kakao Maps JS SDK 웹뷰 로드 셋업 (P2-D2 확정). `index.html` 또는 동적 로더 + JS 앱키 주입 방식. 지도 1회 초기화 유틸 | `index.html`(SDK script) 또는 `src/utils/kakaoMap.js`(동적 로더), 환경변수(.env VITE_KAKAO_MAP_KEY) | P2-D2 확정 | M | §7.2 |
| APP008-N24 | frontend-component | 외근 사유+지도 모달 (`OffsiteReasonSheet.vue`). 지도+현위치 마커 표시 + 사유 textarea + 등록/취소. `BaseBottomSheet` 패턴 차용. 골격은 본 문서 §6 | `src/views/attd/components/OffsiteReasonSheet.vue` (UI-A0xx) | APP008-N23 | M | §7.2~§7.3 |
| APP008-N25 | frontend | 외근 플로우 연동: 출근/퇴근 시 지오펜스 밖이면 모달 노출 → 사유 입력 → `offsiteReason` 동봉하여 check-in/out 호출. 출근·퇴근 양쪽 적용. 외근 선판정 방식은 P2-D2 노트 참조 | `MyAttendanceView.vue`(`onTodayAction`/`submitCheckIn`/`submitCheckOut` 확장) | APP008-N22, APP008-N24 | M | §7.2~§7.3 |

---

## 3. §5.5 Case A/B/C 백엔드 로직 (의사코드)

> 위치: `AppAttd01ServiceImpl.checkIn` 의 "6) 출근횟수/구간 제한" 블록 확장.
> 아래는 **해석 a (같은 WORK_YMD, 2구간 출근 시점)** 기준. 해석 b 확정 시 "선행 1구간" 탐지를 전일 미마감 1구간 쿼리로 교체.

```
// 전제: hasSchedule && isTwoSlot && (이번 출근이 2구간 = existing == 1) 일 때만 §5.5 분기.
// (1구간 출근(existing==0)은 선행 구간 없음 → §5.5.4 별도, P1-D1 참조.)

if (hasSchedule && isTwoSlot && existing == 1) {

    boolean isNightShift = isOvernight(sched.secSchStrTime, sched.secSchEndTime); // 종료<시작 = 자정넘김
    // (P1-D4: 야간 한정인지, 2구간 전부 적용인지 확정. 아래는 야간 한정 가정.)

    AttdRecord prevSlot = attdBySeq.get(1);   // 선행 1구간 레코드 (해석 a)
    // 해석 b: prevSlot = mapper.selectPrevDayOpenFirstSlot(...) 로 전일 미마감 1구간 탐지

    // ── 선행 1구간 상태 분류 ──
    if (prevSlot != null && hasText(prevSlot.checkOutTime)) {
        // Case A — 마감 완료 → 즉시 2구간 출근 (기존 흐름 그대로 진행)
        // (아래 정상 INSERT 로 흘러감)
    }
    else if (prevSlot != null && !hasText(prevSlot.checkOutTime)) {
        // Case B — 1구간 미마감 (강한 차단)
        //   "이전 1구간 근무가 마감되지 않았습니다. 1구간 퇴근을 먼저 처리해주세요."
        //   기존 ATTD_400_081(재출근 미퇴근 차단)과 의미 동일 → 재사용 또는 전용 코드 신설
        throw new ApiException(AttdErrorCode.ATTD_400_081); // (또는 신규 Case B 코드)
    }
    else { // prevSlot == null
        // Case C — 1구간 스킵 (소프트 차단)
        //   서버 1차 호출(confirmSkipPrevSlot != true)에서는 "확인 필요" 코드 반환 → 프론트 얼럿
        //   확인 후 재호출(confirmSkipPrevSlot == true)에서만 통과
        if (!param.confirmSkipPrevSlot()) {
            throw new ApiException(AttdErrorCode.ATTD_XXX_SKIP_CONFIRM); // 신규: 소프트 차단(확인 필요)
        }
        // 확인됨 → 2구간 단독 출근 허용.
        //   ⚠️ P1-D2: 누락된 1구간 데이터 처리(NULL 유지 / 마커 / 마감 보정 대상). 확정값에 따라
        //   WORK_SEQ 채번(1 건너뛰고 2? 아니면 1로?) 및 보정마커 컬럼 처리.
    }
}

// (이하 기존 WORK_SEQ 채번 + INSERT + 지오펜스 + GPS 행)
```

핵심 정리:
- **Case A**: 추가 코드 거의 없음(기존 정상 흐름). 단 "야간 2구간 즉시 허용"이 기존 재출근 가드와 충돌하지 않는지 확인(기존엔 1구간 퇴근 완료 시 재출근 허용 = 이미 A 처리).
- **Case B**: 기존 `ATTD_400_081`(직전 구간 미퇴근 재출근 차단)이 사실상 동일 동작. 메시지 문구만 §5.5 표에 맞춰 점검. 신규 코드 신설은 선택.
- **Case C**: **유일한 신규 로직**. 현행 코드는 `existing>=1 && open==0`(1구간 퇴근됨)일 때만 2구간 허용하므로, "1구간 자체가 없음"(existing==0 에서 2구간을 강제로?) 케이스는 현 모델상 도달 경로가 모호 → P1-D2/P1-D4 확정 필수. 소프트 차단은 신규 errorCode + `confirmSkipPrevSlot` 플래그 왕복으로 구현.

### §5.5.4 1구간 출근 시점(역방향, P1-D1)
- 현행: 전날 미퇴근 근태가 있으면 `countPastOpenAttd` 로 **이미 무조건 차단**(ATTD_400_082, "전날 퇴근 먼저").
- 따라서 "전일 2구간 미마감 발견 시 차단 여부"는 이미 차단 쪽으로 구현돼 있음. P1-D1 은 이 동작을 §5.5.4 규칙으로 **확정/유지**할지, 야간 교대만 예외 허용할지 결정.

---

## 4. Part2 외근 지도+사유 흐름

```
[출근/퇴근 버튼 탭]
   │
   ├─ askConfirm("출근/퇴근하시겠어요?")  (기존)
   │
   ├─ requestGps()  (기존, Flutter GET_GPS)
   │     ├─ status != OK → 기존 측위 실패 안내
   │     └─ isMocked → 기존 위변조 차단
   │
   ├─ [외근 선판정]  (P2-D2 노트: 두 방식 중 택1)
   │     (A) 클라이언트가 사업장 좌표/반경을 미리 받아 프론트에서 지오펜스 밖 선판정
   │     (B) 일단 사유 없이 호출 → 서버가 isOffsite=true + "사유 필요" 신호 반환 → 모달
   │       └ 권장: (A) 프론트 선판정(모달을 먼저 띄워야 하므로). 사업장 좌표는
   │              today 카드/별도 endpoint 로 확보(미확인 — 현재 today 응답에 사업장 좌표 없음 → follow-up).
   │
   ├─ 지오펜스 안 → 기존 즉시 check-in/out (offsiteReason 없음)
   │
   └─ 지오펜스 밖 → [OffsiteReasonSheet 모달 오픈]
         ├─ 지도(Kakao JS SDK) + 현위치 마커 + 사업장 범위(옵션) 표시
         ├─ 사유 textarea 입력  (P2-D3: 미작성 시 등록 버튼 비활성/차단)
         ├─ 취소 → 출퇴근 미등록(중단)
         └─ 등록 → submitCheckIn/Out({ ..., offsiteReason })  → 서버 OFFSITE_REASON 저장
               └ 성공 → "근무지 밖이라 외근으로 처리되었어요." (기존 메시지 유지)
```

⚠️ follow-up(미확인): 프론트 선판정(A)을 쓰려면 today/day 응답에 사업장 LAT/LON/GPS_RANGE 가 있어야 하나 현재 없음 → endpoint 보강 또는 (B) 서버 2-pass 방식 채택 필요. P2-D2 확정 시 함께 결정.

---

## 5. 엔드포인트 변경 초안

기존 2개 엔드포인트 본문만 확장(URL/메서드 불변):

```
POST /prafta/appApi/attd/check-in
  기존:  { lat, lon, accuracy, isMocked, workYmd }
  추가:  + offsiteReason (string, nullable)          # Part2
         + confirmSkipPrevSlot (boolean, default false) # Part1 Case C

POST /prafta/appApi/attd/check-out
  기존:  { lat, lon, accuracy, isMocked, workYmd }
  추가:  + offsiteReason (string, nullable)          # Part2
```

응답: 기존 `MyAttendanceDayResponse + isOffsite` 유지. (Case C 소프트 차단은 HTTP 4xx + 전용 errorCode 로 구분 — 프론트가 코드로 얼럿 분기.)

신규 errorCode(예시, security/developer 확정):
- `ATTD_4xx_xxx` — Case C 소프트 차단(확인 필요). 프론트가 이 코드 수신 시 confirm 얼럿.
- (선택) Case B 전용 코드 — 미설치 시 `ATTD_400_081` 재사용.
- (P2-D3 차단 채택 시) 외근 사유 미작성 차단 코드.

---

## 6. Vue 골격 — OffsiteReasonSheet.vue (APP008-N24, 승인 후 저장)

> 위치: `prafta-app-frontend/prafta-app-frontend/src/views/attd/components/OffsiteReasonSheet.vue`
> 참조 패턴: `views/req/components/BaseBottomSheet.vue`(시트 dimmer/handle/footer), `MyAttendanceView.vue`(디자인 토큰 상속).
> script 의 지도 초기화/제출은 전부 `// TODO(developer):`. planner 는 template+style+선언만.

```vue
<!--
  OffsiteReasonSheet.vue — 외근(근무지 외) 출퇴근 사유 + 지도 모달
  - 작업 ID: APP008-N24 (UI 명세: UI-A0xx)
  - 정책: attd §7.2~§7.3 (지오펜스 밖 = 외근 태그, 사유 작성)
  - 트리거: MyAttendanceView 가 지오펜스 밖 출퇴근 감지 시 open
  - 참조 패턴: views/req/components/BaseBottomSheet.vue
  - planner 스코프: template/style + props/emits/ref 선언만.
    지도(Kakao JS SDK) 초기화·현위치 마커·제출은 developer (TODO).
-->
<template>
  <transition name="ofs-fade">
    <div
      v-if="modelValue"
      class="ofs__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="외근 사유 작성"
      @click.self="onCancel"
    >
      <div class="ofs">
        <div class="ofs__handle" aria-hidden="true"></div>

        <header class="ofs__header">
          <h2 class="ofs__title">{{ mode === 'checkOut' ? '외근 퇴근 등록' : '외근 출근 등록' }}</h2>
          <button type="button" class="ofs__close" aria-label="닫기" @click="onCancel">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>

        <div class="ofs__body">
          <!-- 안내 배너 -->
          <div class="ofs__notice">
            <svg class="ofs__notice-ic" width="16" height="16" viewBox="0 0 24 24" fill="none"
                 stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M21 10c0 7-9 12-9 12s-9-5-9-12a9 9 0 0 1 18 0z" />
              <circle cx="12" cy="10" r="3" />
            </svg>
            <span>근무지 범위 밖이에요. 외근 사유를 작성하면 외근으로 등록돼요.</span>
          </div>

          <!-- 지도 영역 (Kakao JS SDK — developer 초기화) -->
          <div class="ofs__map-wrap">
            <!-- TODO(developer): kakao.maps.Map 을 #ofsMap 에 초기화하고 현위치 마커/사업장 범위 표시 -->
            <div id="ofsMap" ref="mapEl" class="ofs__map" role="img" aria-label="현재 위치 지도"></div>
            <p v-if="!mapReady" class="ofs__map-fallback">지도를 불러오는 중…</p>
          </div>

          <!-- 현위치 좌표 요약 -->
          <p class="ofs__coord">
            현재 위치: {{ coordText }}
          </p>

          <!-- 사유 입력 -->
          <label class="ofs__label" for="ofsReason">외근 사유<span class="ofs__req">*</span></label>
          <textarea
            id="ofsReason"
            v-model="reason"
            class="ofs__textarea"
            rows="3"
            maxlength="500"
            placeholder="예: 거래처 방문, 현장 점검 등"
          ></textarea>
          <p class="ofs__count">{{ reason.length }}/500</p>
        </div>

        <footer class="ofs__footer">
          <button type="button" class="ofs__btn ofs__btn--ghost" @click="onCancel">취소</button>
          <button
            type="button"
            class="ofs__btn ofs__btn--primary"
            :disabled="!canSubmit"
            @click="onSubmit"
          >
            외근으로 등록
          </button>
        </footer>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'

// props: modelValue(v-model), mode('checkIn'|'checkOut'), lat/lon/accuracy(현위치 좌표)
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'checkIn' }, // 'checkIn' | 'checkOut'
  lat: { type: Number, default: null },
  lon: { type: Number, default: null },
  accuracy: { type: Number, default: null },
})

// emits: update:modelValue(닫기), submit({ reason }), cancel
const emit = defineEmits(['update:modelValue', 'submit', 'cancel'])

// 사유 입력 (UI 바인딩 — 허용 범위)
const reason = ref('')

// 지도 엘리먼트/준비 상태 (developer 가 초기화)
const mapEl = ref(null)
const mapReady = ref(false)

// 현위치 좌표 표시 텍스트 (단순 표시 — 허용 범위)
const coordText = computed(() => {
  if (props.lat == null || props.lon == null) return '확인 중…'
  return `${props.lat.toFixed(5)}, ${props.lon.toFixed(5)}`
})

// 등록 가능 여부: 사유 필수(P2-D3 확정값에 따라 조정). 단순 length 체크 — 허용 범위
const canSubmit = computed(() => reason.value.trim().length > 0)

const onCancel = () => {
  emit('cancel')
  emit('update:modelValue', false)
}

const onSubmit = () => {
  if (!canSubmit.value) return
  emit('submit', { reason: reason.value.trim() })
  // 닫기/초기화는 부모(성공 후) 또는 아래 watch 가 처리
}

// 열림/닫힘에 따른 사유 초기화 + 지도 초기화 트리거
watch(
  () => props.modelValue,
  async (open) => {
    if (open) {
      reason.value = ''
      mapReady.value = false
      await nextTick()
      // TODO(developer): Kakao Maps JS SDK 로드 확인 후 mapEl 에 지도 생성,
      //   props.lat/lon 으로 중심/마커 세팅, 사업장 GPS_RANGE 원(circle) 표시,
      //   완료 시 mapReady.value = true. (SDK/키는 APP008-N23 셋업 의존)
    }
  },
)
</script>

<style scoped>
.ofs__dimmer {
  position: fixed;
  inset: 0;
  background: var(--color-overlay);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 110;
}
.ofs {
  width: 100%;
  max-width: 414px;
  background: var(--color-surface);
  border-top-left-radius: var(--radius-xl);
  border-top-right-radius: var(--radius-xl);
  padding: var(--space-sm) 0 calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  display: flex;
  flex-direction: column;
  max-height: 88vh;
}
.ofs__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  margin: var(--space-xs) auto var(--space-sm);
}
.ofs__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-xs) var(--space-lg) var(--space-sm);
}
.ofs__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.ofs__close {
  width: 32px;
  height: 32px;
  background: transparent;
  border: 0;
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.ofs__body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-xs) var(--space-lg) var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.ofs__notice {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
  padding: var(--space-md);
  background: var(--color-warning-tint);
  border: 1px solid var(--color-warning-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--color-warning-text);
}
.ofs__notice-ic {
  flex-shrink: 0;
  margin-top: 1px;
}
.ofs__map-wrap {
  position: relative;
  width: 100%;
  height: 200px;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border);
  background: var(--color-bg);
}
.ofs__map {
  width: 100%;
  height: 100%;
}
.ofs__map-fallback {
  position: absolute;
  inset: 0;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.ofs__coord {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}
.ofs__label {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.ofs__req {
  color: var(--color-danger);
  margin-left: 2px;
}
.ofs__textarea {
  width: 100%;
  box-sizing: border-box;
  resize: none;
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
}
.ofs__textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}
.ofs__count {
  margin: 0;
  text-align: right;
  font-size: 11px;
  color: var(--color-text-tertiary);
}
.ofs__footer {
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg) 0;
  border-top: 0.5px solid var(--color-border-light);
}
.ofs__btn {
  flex: 1;
  height: 48px;
  border: 0;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.ofs__btn--ghost {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
}
.ofs__btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
}
.ofs__btn--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.ofs-fade-enter-active,
.ofs-fade-leave-active {
  transition: opacity 0.18s ease;
}
.ofs-fade-enter-from,
.ofs-fade-leave-to {
  opacity: 0;
}
</style>
```

---

## 7. 사용자 확정 결정 목록

### Part1 (§5.5)
- **P1-D1**: 1구간 출근 시 전일 2구간(또는 전일 미마감 근태) 발견 시 차단 여부(§5.5.4 역방향). 현행은 `countPastOpenAttd` 로 이미 무조건 차단. 이 동작을 §5.5.4 규칙으로 확정/유지할지, 야간 교대 예외를 둘지.
- **P1-D2**: Case C(스킵) 후 생성된 2구간 출근 기록에서 누락된 1구간 데이터 처리 방식 — NULL 유지 / 별도 마커 컬럼(신규) / 근태 마감 시 보정 요구 대상. WORK_SEQ 채번도 함께(1 건너뛰고 2로? 1로 적재?).
- **P1-D3**: Case C 확인 얼럿 시점 — 출근 버튼 탭 직후 vs GPS 판정 후. (Part2 외근 모달과의 순서도 함께 — 둘 다 발생 시 무엇이 먼저인지.)
- **P1-D4**: 야간 판정 기준(`SEC_SCH_END < SEC_SCH_STR` = 자정 넘김)으로 확정할지, "선행 1구간" 식별을 **해석 a(같은 WORK_YMD WORK_SEQ=1)** vs **해석 b(전일 WORK_YMD 미마감 1구간 탐지)** 중 무엇으로 할지. ⚠️ 현행 스키마/코드는 해석 a 모델. 해석 b 면 다음날 게이트(ATTD_400_082)와 충돌 정리 필요(§1-1 참조).

### Part2 (외근 사유/지도)
- **P2-D1**: 외근 사유 저장 위치/컬럼 — `tb_user_attd_gps.OFFSITE_REASON varchar(500)`(권장: GPS 행=외근) vs `tb_user_attd_mgmt` 신규 컬럼 vs 신규 테이블. 필수 여부 / 길이(권장 500, REQ_REASON 관례와 동일).
- **P2-D2**: 지도 SDK — Kakao Maps **JS** SDK 채택 + JS 앱키 조달처/주입 방식(env `VITE_KAKAO_MAP_KEY` + index.html 또는 동적 로더). 웹뷰 도메인 등록(키 발급 시 도메인 화이트리스트). 추가로 **외근 선판정 방식**(프론트 선판정 A vs 서버 2-pass B) — A 채택 시 today/day 응답에 사업장 좌표/반경 보강 필요(현재 없음).
- **P2-D3**: 외근 사유 작성 UX — 모달(바텀시트, 권장) vs 별도 화면. 출근·퇴근 양쪽 동일 적용 확정. 사유 미작성 시 등록 차단 여부(권장: 차단 = 등록 버튼 비활성 + 서버 검증).

---

## 8. 스키마 / SDK 미확인 (⚠️)
- ⚠️ 외근 사유 컬럼: 신규 추가 필요(현재 0건). P2-D1 확정 후 ALTER(운영 미적용).
- ⚠️ Kakao Maps JS SDK: 앱 프론트 미탑재. P2-D2 확정 후 셋업.
- ⚠️ today/day-detail 응답에 사업장 LAT/LON/GPS_RANGE 없음 → 프론트 외근 선판정 시 endpoint 보강 follow-up.
- ⚠️ §5.5 "선행 1구간"의 데이터 모델 해석(a/b) 미확정 → P1-D4. 해석 b 면 mapper 신규.
- ⚠️ Case B/Case C errorCode 신설 여부(security/developer 영역) — 본 분해는 신규 1종(Case C 소프트) + 기존 재사용(Case B) 가정.

---

## 9. 권장 착수 순서
1. **결정 먼저**: P1-D4 / P1-D2 (Part1 모델), P2-D1 / P2-D2 (Part2 인프라). 이게 안 풀리면 착수 불가.
2. **Part2 백엔드 먼저** (API 없이 화면 불가, 우선순위 원칙):
   APP008-N21(컬럼 마이그) → APP008-N22(check-in/out offsiteReason 저장).
3. **Part1 백엔드**: APP008-N11(야간/선행구간 식별) → APP008-N12(Case A/B/C).
4. **Part2 프론트 인프라**: APP008-N23(Kakao SDK 셋업) → APP008-N24(OffsiteReasonSheet 골격→developer 지도 연동).
5. **프론트 연동**: APP008-N25(외근 플로우) + APP008-N13(Case C 얼럿).

> Part1·Part2 는 독립적이라 병렬 가능. 법적 책임 영역(attd) +1단계 격상 적용 — 정합성·차단 로직(Part1) 우선 검토 권장.
