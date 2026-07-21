<!--
  Platform_04.vue — 위치정보 열람 (플랫폼 운영자 전용 콘솔, LBS 필수 기능)
  - 메뉴: tb_syst_menu_d MENU_D_ID='Platform_04', MENU_VIEW='platform/Platform_04.vue'
  - 접근: CMPNY_CD='prafta_system_admin' 운영자만(서버 /platformApi 게이트가 강제. 메뉴 숨김은 보조).
  - 진입 게이트: SMS 인증 오버레이(PlatformSmsGateOverlay) — 서버 판정 10분 유효.
    조회 API(gps-lists)는 인증 없이는 403(PLATFORM_403_003) — 프론트 게이트는 보조.
  - 동작: 회사 → 사업장 → 단일날짜(기본 오늘) 필터 → GET /platformApi/location/gps-lists
    → 좌측 목록 + 우측 카카오맵(행 클릭 → 마커 포커스, 사업장 중심 + GPS_RANGE 원).
  - 지도 이식 원본: src/views/attd/popup/AttdGpsCoordPanel.vue (loadKakaoMapScript/마커/cleanup).
  - 골격: planner 작성(template + scoped style), script 로직: developer 작성(PLT-LOC-06).
-->
<template>
  <div class="viewComm p04-root">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 검색 영역: 회사 → 사업장 → 단일 날짜 (요청서 §3-2). 표준 viewSearch 패턴(라벨+필드) -->
    <div class="viewSearch">
      <div>
        <label>회사</label>
        <select
          v-model="srchCmpnyCd"
          name="combo"
          style="width: 200px"
          :disabled="!smsVerified"
        >
          <option value="">회사 선택</option>
          <option
            v-for="cmpny in cmpnyList"
            :key="cmpny.cmpnyCd"
            :value="cmpny.cmpnyCd"
          >
            {{ cmpny.cmpnyNm }}
          </option>
        </select>
      </div>
      <div>
        <label>사업장</label>
        <input
          type="text"
          v-model="srchSiteNo"
          placeholder="사업장번호"
          readonly
          style="width: 110px"
        />
        <button
          class="search-btn"
          :disabled="!smsVerified || !srchCmpnyCd"
          @click="fnSiteSearchPopOpen"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          type="text"
          v-model="srchSiteNm"
          placeholder="사업장명"
          readonly
          style="width: 160px"
        />
      </div>
      <div>
        <label>날짜</label>
        <CalendarSrch
          v-model="srchDate"
          :disabled="!smsVerified"
          style="width: 140px"
        />
      </div>
      <!-- 인증 잔여시간 배지 — 조회 필드에서 분리해 행 우측으로 정렬(상태 표시, 서버 10분 판정의 보조) -->
      <div v-if="smsVerified" class="p04-auth-badge-wrap">
        <span class="p04-auth-badge">
          <span class="p04-auth-badge__dot" aria-hidden="true"></span>
          인증됨 · 남은 시간 {{ fnFormatRemain(authRemainSec) }}
        </span>
      </div>
    </div>

    <div class="viewBody p04-body">
      <!-- 1,000건 초과 절단 안내 (요청서 §3-2 대량 대비) -->
      <div v-if="truncated" class="p04-truncated-banner">
        조회 결과가 1,000건을 초과합니다. 상위 1,000건만 표시합니다.
      </div>

      <div class="p04-panes">
        <!-- 좌: 위치정보 목록 -->
        <div class="table-wrapper subtitle-pane p04-list-pane">
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">위치정보 목록</span>
            <span v-if="gpsList.length > 0" class="p04-count">
              총 {{ gpsList.length }}건
              <span v-if="mockedCount > 0" class="p04-mock-warn"
                >(Mock {{ mockedCount }}건)</span
              >
            </span>
          </div>

          <div
            class="table-box"
            style="--box-h: 62vh; --box-sticky-top: 1px; --box-ox: auto"
          >
            <table
              class="data-grid w-full table-fixed text-sm text-left rtl:text-right"
            >
              <thead>
                <tr>
                  <th class="event_cell" style="text-align: center; width: 4%">
                    No
                  </th>
                  <th style="width: 10%">측정시각</th>
                  <th style="width: 14%">사용자코드</th>
                  <th style="width: 10%">GPS유형</th>
                  <th style="width: 14%">위도</th>
                  <th style="width: 14%">경도</th>
                  <th style="width: 10%">정확도(m)</th>
                  <th style="width: 8%">Mock</th>
                  <th style="width: 10%">수집원</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="listLoading">
                  <tr>
                    <td colspan="9" class="edu-grid-empty">
                      위치정보를 불러오는 중...
                    </td>
                  </tr>
                </template>
                <template v-else-if="!gpsList || gpsList.length === 0">
                  <tr>
                    <td colspan="9" class="edu-grid-empty">
                      수집된 위치정보가 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr
                    v-for="(gps, idx) in gpsList"
                    :key="idx"
                    class="p04-row"
                    :class="{ 'is-selected': selectedIdx === idx }"
                    @click="fnRowClick(gps, idx)"
                  >
                    <td style="text-align: center">{{ idx + 1 }}</td>
                    <td style="text-align: center">
                      {{ formatHms(gps.measureTime) }}
                    </td>
                    <td class="p04-mono">{{ gps.userCd }}</td>
                    <td style="text-align: center">
                      {{ fnGpsTypeLabel(gps.gpsInfoType) }}
                    </td>
                    <td>{{ gps.lat }}</td>
                    <td>{{ gps.lon }}</td>
                    <td style="text-align: right">{{ gps.accuracy ?? "-" }}</td>
                    <td style="text-align: center">
                      <span
                        v-if="gps.isMocked === 'Y'"
                        class="p04-badge p04-badge--mock"
                        >Mock</span
                      >
                      <span v-else>-</span>
                    </td>
                    <td style="text-align: center">
                      <span
                        class="p04-badge"
                        :class="
                          gps.srcType === 'TBM'
                            ? 'p04-badge--tbm'
                            : 'p04-badge--attd'
                        "
                      >
                        {{ gps.srcType === "TBM" ? "TBM입실" : "근태" }}
                      </span>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 우: 카카오맵 -->
        <div class="table-wrapper subtitle-pane p04-map-pane">
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path
                  d="M12 2C7.6 2 4 5.6 4 10c0 6 8 12 8 12s8-6 8-12c0-4.4-3.6-8-8-8zm0 11a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"
                />
              </svg>
            </span>
            <span class="subtitle-text">지도</span>
          </div>

          <!-- 지도 상태: error / empty / canvas -->
          <div v-if="mapError" class="p04-map-empty p04-map-empty--error">
            지도를 불러오지 못했습니다.
          </div>
          <div
            v-else-if="!listLoading && gpsList.length === 0"
            class="p04-map-empty"
          >
            조회 결과가 있으면 지도에 표시됩니다.
          </div>
          <div v-else ref="mapContainer" class="p04-map-canvas"></div>

          <!-- 범례 -->
          <div class="p04-map-legend">
            <span class="p04-legend-item"
              ><span class="p04-legend-pin p04-legend-pin--attd"></span>근태
              GPS</span
            >
            <span class="p04-legend-item"
              ><span class="p04-legend-pin p04-legend-pin--tbm"></span>TBM
              입실</span
            >
            <span class="p04-legend-item"
              ><span class="p04-legend-pin p04-legend-pin--site"></span>사업장
              중심 · 허용반경</span
            >
          </div>
        </div>
      </div>

      <!-- SMS 인증 게이트 오버레이 — 미인증 시 조회 UI 전면 차단(프론트 보조 게이트) -->
      <PlatformSmsGateOverlay v-if="!smsVerified" @verified="fnGateVerified" />
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  watch,
  nextTick,
  onMounted,
  onBeforeUnmount,
  getCurrentInstance,
  defineOptions,
  defineProps,
} from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import PlatformSmsGateOverlay from "@/views/platform/components/PlatformSmsGateOverlay.vue";
import PlatformSiteSearchPop from "@/views/platform/components/PlatformSiteSearchPop.vue";
import { useModal } from "@/utils/useModal";
import { formatHms } from "@/utils/dateFormat";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import search_icon from "@/assets/img/search_icon.png";

// keep-alive 매칭용 컴포넌트 이름 = 라우트 이름(MENU_D_ID)
defineOptions({ name: "Platform_04" });

// MainLayout 이 주입하는 공통 props(탭 제목/버튼 권한)
const props = defineProps({
  title: { type: String, default: "위치정보 열람" },
  buttons: { type: Object, default: () => ({}) },
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

/* ── SMS 게이트 상태 ── */
const smsVerified = ref(false); // 프론트 보조 게이트(서버가 gps-lists 호출마다 재판정)
const authRemainSec = ref(0); // 인증 잔여초(sms-status remainSec 기반 카운트다운)

/* ── 조회조건 ── */
const srchCmpnyCd = ref("");
const srchSiteCd = ref(""); // 내부 사업장코드(조회/지도 기준) — 팝업 선택으로만 설정
const srchSiteNo = ref(""); // 표시용 사업장번호(입력칸)
const srchSiteNm = ref(""); // 표시용 사업장명(입력칸)
const srchDate = ref(""); // 기본=오늘 (developer: KST 기준 세팅 — toISOString(UTC) 금지)

/* ── 선택 사업장(지도 중심/지오펜스용 좌표 보유) — 팝업이 행 전체를 넘겨준다 ── */
const selectedSite = ref(null); // { siteCd, siteNo, siteNm, lat, lon, gpsRange }

/* ── 필터 소스 ── */
const cmpnyList = ref([]); // GET /platformApi/customer/customer-lists 재사용

/* ── 목록/지도 상태 ── */
const gpsList = ref([]);
const listLoading = ref(false);
const truncated = ref(false); // 서버 LIMIT 1000 초과 여부
const selectedIdx = ref(-1);
const mapContainer = ref(null);
const mapError = ref(false);
const systCodeArr = ref({}); // SYS028 라벨 (comApi syst-info-lists — Platform_02 전례)

const mockedCount = computed(
  () => gpsList.value.filter((g) => g.isMocked === "Y").length
);

/* read-only 화면 — 조회 외 버튼 숨김 (Platform_02 fnButtonControll 전례) */
const localButtons = ref({ ...props.buttons });
function fnButtonControll() {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
}

/* ── 인증 잔여시간 카운트다운 (반응형 불필요 — 일반 변수) ── */
let authTimer = null;

function fnStartAuthCountdown() {
  fnStopAuthCountdown();
  authTimer = setInterval(() => {
    if (authRemainSec.value > 0) authRemainSec.value -= 1;
    if (authRemainSec.value <= 0) {
      fnStopAuthCountdown();
      smsVerified.value = false; // 10분 만료 → 오버레이 재표시(서버 판정의 보조 표시)
    }
  }, 1000);
}

function fnStopAuthCountdown() {
  if (authTimer) {
    clearInterval(authTimer);
    authTimer = null;
  }
}

/* 오늘(로컬 시간 기준) → CalendarSrch 모델 형식 YYYY-MM-DD — toISOString(UTC) 금지 */
function fnTodayLocal() {
  const now = new Date();
  const m = String(now.getMonth() + 1).padStart(2, "0");
  const d = String(now.getDate()).padStart(2, "0");
  return `${now.getFullYear()}-${m}-${d}`;
}

/*
 * SMS 인증 상태 동기화 — GET /platformApi/location/sms-status (PLT-LOC-03).
 *   verified=true 면 remainSec 기반 카운트다운 시작(재진입 시 불필요한 재인증 생략).
 *   확인 실패 시 상태를 바꾸지 않는다(서버가 gps-lists 호출마다 진짜 게이트를 재판정).
 */
async function fnSyncSmsStatus() {
  try {
    const response = await axios.get("/platformApi/location/sms-status");
    if (response.status === 200) {
      const verified = response.data?.verified === true;
      smsVerified.value = verified;
      authRemainSec.value = verified
        ? Number(response.data?.remainSec) || 0
        : 0;
      if (verified) {
        fnStartAuthCountdown();
      } else {
        fnStopAuthCountdown();
      }
    }
  } catch (err) {
    console.error("[Platform_04] sms-status 확인 실패:", err);
  }
}

/* SYS028(GPS정보타입) 라벨 소스 — Platform_02 fnGetSystinfoList 전례 */
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS028"],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.systInfoList || [];

      const grouped = {};
      resData.forEach((item) => {
        const key = item.systValCd;
        if (!grouped[key]) {
          grouped[key] = [];
        }
        grouped[key].push(item);
      });

      systCodeArr.value = grouped;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

/* 회사 select 소스 — 고객 리스트 API 재사용(PLT-LOC-02) */
async function fnGetCmpnyList() {
  try {
    const response = await axios.get("/platformApi/customer/customer-lists");
    if (response.status === 200) {
      cmpnyList.value = response.data?.customerList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(
      err,
      "회사 목록 조회 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  }
}

onMounted(async () => {
  fnButtonControll();
  srchDate.value = fnTodayLocal();
  await fnGetSystinfoList();
  await fnGetCmpnyList();
  await fnSyncSmsStatus();
});

onBeforeUnmount(() => {
  fnStopAuthCountdown();
  fnCleanupMap();
});

/* SMS 게이트 통과 콜백 — 서버 remainSec 로 동기화(실패 시 폴백 10분 유지) */
async function fnGateVerified() {
  smsVerified.value = true;
  authRemainSec.value = 600;
  fnStartAuthCountdown();
  await fnSyncSmsStatus();
}

/* 회사 select 변경 감지 → 사업장 선택/결과 초기화(네이티브 select v-model 갱신 이후 실행 보장) */
watch(srchCmpnyCd, () => {
  fnCmpnyChanged();
});

/* 회사 변경 → 선택 사업장/조회결과 초기화(사업장 목록은 검색 팝업이 대상 회사로 재조회) */
function fnCmpnyChanged() {
  srchSiteCd.value = "";
  srchSiteNo.value = "";
  srchSiteNm.value = "";
  selectedSite.value = null;
  gpsList.value = [];
  truncated.value = false;
  selectedIdx.value = -1;
  fnCleanupMap();
}

/* 사업장 검색 팝업 열기 — 대상 회사(srchCmpnyCd) 스코프의 플랫폼 사업장 목록 조회 */
function fnSiteSearchPopOpen() {
  if (!smsVerified.value || !srchCmpnyCd.value) return;
  openPop(PlatformSiteSearchPop, {
    cmpnyCd_p: srchCmpnyCd.value,
    onSelect: onSiteSelected,
  });
}

/* 팝업 사업장 선택 콜백 — 내부코드/표시값/좌표(지도용)를 함께 세팅 */
function onSiteSelected(site) {
  if (!site) return;
  selectedSite.value = site;
  srchSiteCd.value = site.siteCd;
  srchSiteNo.value = site.siteNo || "";
  srchSiteNm.value = site.siteNm || "";
}

/*
 * 위치정보 조회 — GET /platformApi/location/gps-lists (PLT-LOC-04).
 *   서버가 매 호출 SMS 인증을 재판정: 403(PLATFORM_403_003) 수신 시
 *   오버레이 재표시(smsVerified=false — 서버가 진짜 게이트, 프론트는 보조).
 */
async function fnSearch() {
  if (!smsVerified.value) return; // 게이트 미통과(오버레이 표시 중) — 서버도 403 으로 차단

  if (!srchCmpnyCd.value || !srchSiteCd.value || !srchDate.value) {
    await proxy.$alert("회사, 사업장, 조회 일자를 모두 지정해 주세요.");
    return;
  }

  listLoading.value = true;
  truncated.value = false;
  selectedIdx.value = -1;
  gpsList.value = [];
  mapError.value = false;
  fnCleanupMap();

  try {
    const response = await axios.get("/platformApi/location/gps-lists", {
      params: {
        cmpnyCd: srchCmpnyCd.value,
        siteCd: srchSiteCd.value,
        // CalendarSrch 모델(YYYY-MM-DD) → YYYYMMDD
        date: String(srchDate.value).replace(/\D/g, "").slice(0, 8),
      },
    });

    if (response.status === 200) {
      // 백엔드 응답 필드는 mockedYn — 템플릿 바인딩(isMocked)에 맞춰 매핑(응답 가공)
      gpsList.value = (response.data?.gpsList || []).map((g) => ({
        ...g,
        isMocked: g.mockedYn,
      }));
      truncated.value = response.data?.truncated === true;
    }

    listLoading.value = false;

    if (gpsList.value.length > 0) {
      await nextTick(); // 지도 캔버스(v-else 분기) DOM 반영 대기
      await fnRenderMap();
    }
  } catch (err) {
    listLoading.value = false;

    // SMS 인증 만료/미통과(서버 판정) → 오버레이 재표시
    if (
      err?.response?.status === 403 &&
      err?.response?.data?.errorCode === "PLATFORM_403_003"
    ) {
      fnStopAuthCountdown();
      authRemainSec.value = 0;
      smsVerified.value = false;
      return;
    }

    const msg = resolveApiErrorMessage(
      err,
      "위치정보 조회 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  }
}

/* 목록 행 클릭 → 해당 좌표로 지도 중심 이동 */
function fnRowClick(gps, idx) {
  selectedIdx.value = idx;
  if (!kakaoMap || !window.kakao?.maps) return;
  const lat = Number(gps.lat);
  const lon = Number(gps.lon);
  if (isNaN(lat) || isNaN(lon)) return;
  kakaoMap.panTo(new window.kakao.maps.LatLng(lat, lon));
}

/* ── 카카오맵 (AttdGpsCoordPanel 이식) ── */

// 지도 인스턴스/오브젝트 핸들 (반응형 불필요 — 일반 변수)
let kakaoMap = null;
let kakaoOverlays = []; // 마커·원 등 지도 오브젝트 일괄 관리

/*
 * 카카오맵 SDK 동적 로더 — AttdGpsCoordPanel 이식.
 * 중복 로드 가드 + 10초 타임아웃 포함. 앱키는 기존 VITE_PUBLIC_KAKAO_APP_JS_KEY 그대로.
 */
const loadKakaoMapScript = () => {
  return new Promise((resolve, reject) => {
    if (window.kakao && window.kakao.maps) {
      resolve();
      return;
    }
    const existingScript = document.querySelector(
      'script[src*="dapi.kakao.com"]'
    );
    if (existingScript) {
      const checkInterval = setInterval(() => {
        if (window.kakao && window.kakao.maps) {
          clearInterval(checkInterval);
          resolve();
        }
      }, 100);
      setTimeout(() => {
        clearInterval(checkInterval);
        if (!window.kakao || !window.kakao.maps) {
          reject(new Error("카카오 지도 API 로드 타임아웃"));
        }
      }, 10000);
      return;
    }
    const kakaoKey = import.meta.env.VITE_PUBLIC_KAKAO_APP_JS_KEY;
    if (!kakaoKey) {
      reject(new Error("카카오 지도 API 키가 없습니다."));
      return;
    }
    const scriptUrl = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${kakaoKey}&libraries=services&autoload=false`;
    const script = document.createElement("script");
    script.src = scriptUrl;
    script.async = true;
    script.onload = () => {
      if (window.kakao && window.kakao.maps) {
        window.kakao.maps.load(() => resolve());
      } else {
        reject(new Error("카카오 지도 API 객체를 찾을 수 없습니다."));
      }
    };
    script.onerror = () => {
      reject(new Error("카카오 지도 API 로드 실패."));
    };
    document.head.appendChild(script);
  });
};

/*
 * 지도 렌더 — AttdGpsCoordPanel renderMap 패턴 이식.
 *   근태(ATTD)/TBM 마커 색 구분(범례와 일치) + 사업장 중심 마커 + GPS_RANGE 원.
 *   실패 시 mapError=true (캔버스 대신 오류 문구).
 */
async function fnRenderMap() {
  if (!mapContainer.value) return;

  mapError.value = false;
  try {
    await loadKakaoMapScript();
  } catch (e) {
    console.error("[Platform_04] 카카오 지도 로드 실패:", e);
    mapError.value = true;
    return;
  }

  fnCleanupMap();

  // gpsList → 유효 좌표 배열
  const points = gpsList.value
    .map((g) => {
      const lat = Number(g.lat);
      const lon = Number(g.lon);
      if (isNaN(lat) || isNaN(lon)) return null;
      return { lat, lon, srcType: g.srcType };
    })
    .filter(Boolean);

  // 선택 사업장 중심/지오펜스 (lat/lon NULL 이면 미표시 — Number(null)=0 오인 방지 가드)
  const site = selectedSite.value;
  const siteLat = Number(site?.lat);
  const siteLon = Number(site?.lon);
  const hasSite =
    site != null &&
    site.lat != null &&
    site.lon != null &&
    !isNaN(siteLat) &&
    !isNaN(siteLon);

  if (points.length === 0 && !hasSite) return;

  // 지도 생성 (사업장 중심 우선, 없으면 첫 좌표)
  const center = hasSite
    ? new window.kakao.maps.LatLng(siteLat, siteLon)
    : new window.kakao.maps.LatLng(points[0].lat, points[0].lon);
  kakaoMap = new window.kakao.maps.Map(mapContainer.value, {
    center,
    level: 4,
  });

  // 마커 이미지 — SVG data-uri 는 CSS 변수 사용 불가라 토큰과 일치하는 hex 하드코딩(전례 동일)
  const buildPinImage = (fill, stroke) =>
    new window.kakao.maps.MarkerImage(
      "data:image/svg+xml;base64," +
        btoa(
          `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="32" viewBox="0 0 24 32"><path d="M12 1C5.9 1 1 5.9 1 12c0 8.6 11 19 11 19s11-10.4 11-19C23 5.9 18.1 1 12 1z" fill="${fill}" stroke="${stroke}" stroke-width="1.5"/><circle cx="12" cy="12" r="5" fill="#fff"/></svg>`
        ),
      new window.kakao.maps.Size(24, 32),
      { offset: new window.kakao.maps.Point(12, 32) }
    );

  const attdImage = buildPinImage("#16a34a", "#16a34a"); // --color-primary (근태)
  const tbmImage = buildPinImage("#fef3c7", "#d1d5db"); // --color-warning-bg / --color-border-strong (TBM — 범례 일치)
  const siteImage = buildPinImage("#ffffff", "#4b5563"); // --color-surface / --color-text-muted (사업장 — 범례 일치)

  const bounds = new window.kakao.maps.LatLngBounds();

  // 사업장 중심 마커 + GPS_RANGE 원(값 있을 때만)
  if (hasSite) {
    const sitePos = new window.kakao.maps.LatLng(siteLat, siteLon);
    bounds.extend(sitePos);

    const siteMarker = new window.kakao.maps.Marker({
      map: kakaoMap,
      position: sitePos,
      image: siteImage,
      zIndex: 1,
    });
    kakaoOverlays.push(siteMarker);

    const radius = Number(site.gpsRange);
    if (
      site.gpsRange != null &&
      String(site.gpsRange).trim() !== "" &&
      !isNaN(radius) &&
      radius > 0
    ) {
      const circle = new window.kakao.maps.Circle({
        center: sitePos,
        radius,
        strokeWeight: 2,
        strokeColor: "#4b5563", // --color-text-muted (범례 site 링과 일치)
        strokeOpacity: 0.8,
        strokeStyle: "solid",
        fillColor: "#4b5563",
        fillOpacity: 0.08,
      });
      circle.setMap(kakaoMap);
      kakaoOverlays.push(circle);
    }
  }

  // 위치정보 마커 (근태/TBM 색 구분)
  points.forEach((p) => {
    const pos = new window.kakao.maps.LatLng(p.lat, p.lon);
    bounds.extend(pos);

    const marker = new window.kakao.maps.Marker({
      map: kakaoMap,
      position: pos,
      image: p.srcType === "TBM" ? tbmImage : attdImage,
    });
    kakaoOverlays.push(marker);
  });

  // 자동 줌 (좌표 1점뿐이면 setBounds 가 과확대되므로 중심 고정)
  if (points.length + (hasSite ? 1 : 0) === 1) {
    kakaoMap.setCenter(center);
    kakaoMap.setLevel(4);
  } else {
    kakaoMap.setBounds(bounds);
  }
}

/* 지도 정리 — AttdGpsCoordPanel cleanupMap 패턴 이식 */
function fnCleanupMap() {
  for (const o of kakaoOverlays) {
    try {
      o.setMap(null);
    } catch (_e) {
      void 0;
    }
  }
  kakaoOverlays = [];
  kakaoMap = null;
}

/* SYS028 GPS유형 라벨 (TBM 수집분은 유형 없음 → '-', 미해석 시 코드 원값) */
function fnGpsTypeLabel(gpsInfoType) {
  if (!gpsInfoType) return "-";
  const found = (systCodeArr.value.SYS028 || []).find(
    (c) => c.systValDCd === gpsInfoType
  );
  return found?.systValDNm || gpsInfoType;
}

/* 잔여초 → m:ss 표시 */
function fnFormatRemain(sec) {
  const s = Math.max(0, Number(sec) || 0);
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;
}
</script>

<style scoped>
/* 오버레이 절대배치 기준 컨테이너 */
.p04-body {
  position: relative;
}

/* 인증 잔여시간 배지 — 조회 필드 묶음과 분리해 행 우측 끝으로 정렬 */
.p04-auth-badge-wrap {
  margin-left: auto;
}
.p04-auth-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.7rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-primary);
  background: var(--color-surface);
  border: 1px solid var(--color-primary);
  border-radius: 999px;
}
.p04-auth-badge__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
}

/* 1,000건 초과 절단 배너 — 제한 상황은 배너로 명시(공통 정책서 §13.3) */
.p04-truncated-banner {
  margin: 0 0 0.5rem;
  padding: 0.5rem 0.75rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-warning-text);
  background: var(--color-warning-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}

/* 좌 목록(40%) / 우 지도(60%) 2-pane */
.p04-panes {
  display: flex;
  gap: 0.75rem;
  align-items: stretch;
}
.p04-list-pane {
  flex: 0 0 40%;
  min-width: 0;
}
.p04-map-pane {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

/* 소제목 우측 건수/Mock 경고 */
.p04-count {
  margin-left: 0.5rem;
  font-size: 0.78rem;
  font-weight: 400;
  color: var(--color-text-muted);
}
.p04-mock-warn {
  color: var(--color-danger);
  font-weight: 600;
}

/* 목록 행 — 클릭 선택 강조 */
.p04-row {
  cursor: pointer;
}
.p04-row.is-selected td {
  background: var(--color-warning-bg);
}

.p04-mono {
  font-family: "D2Coding", Consolas, monospace;
  word-break: break-all;
}

/* 배지: Mock / 수집원(근태·TBM) — 상태 배지로 구분(요청서 §5-4, 공통 §13.3) */
.p04-badge {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--color-surface);
  white-space: nowrap;
}
.p04-badge--mock {
  background: var(--color-danger);
}
.p04-badge--attd {
  background: var(--color-primary);
}
.p04-badge--tbm {
  color: var(--color-text-strong);
  background: var(--color-warning-bg);
  border: 1px solid var(--color-border-strong);
}

/* 지도 캔버스/빈 상태 (AttdGpsCoordPanel 스타일 패턴 차용) */
.p04-map-canvas {
  flex: 1 1 auto;
  width: 100%;
  min-height: 56vh;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}
.p04-map-empty {
  flex: 1 1 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 56vh;
  font-size: 0.85rem;
  color: var(--color-text-muted);
  background: var(--color-bg);
  border: 1px dashed var(--color-border);
  border-radius: var(--input-radius);
}
.p04-map-empty--error {
  color: var(--color-danger);
}

/* 범례 */
.p04-map-legend {
  display: flex;
  gap: 1rem;
  margin-top: 0.4rem;
  font-size: 0.78rem;
  color: var(--color-text-muted);
}
.p04-legend-item {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
}
.p04-legend-pin {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.p04-legend-pin--attd {
  background: var(--color-primary);
}
.p04-legend-pin--tbm {
  background: var(--color-warning-bg);
  border: 1px solid var(--color-border-strong);
}
.p04-legend-pin--site {
  background: var(--color-surface);
  border: 2px solid var(--color-text-muted);
}
</style>
