<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 조회 영역 -->
    <div class="viewSearch">
      <div>
        <label>기간</label>
        <CalendarSrch
          :range="false"
          style="width: 130px"
          v-model="fromDate"
        />
        <span class="period-sep">~</span>
        <CalendarSrch
          :range="false"
          style="width: 130px"
          v-model="toDate"
        />
      </div>
      <div>
        <label>사업장</label>
        <input
          id="siteNo"
          ref="siteNoFcs"
          type="text"
          v-model="siteNo"
          placeholder="사업장코드"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="siteDisabled"
          @click="fnSiteSearchPopOpen()"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="siteNm"
          type="text"
          v-model="siteNm"
          placeholder="사업장명"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
      </div>
      <div>
        <label>소속부서</label>
        <input
          id="nodeCd"
          type="text"
          v-model="nodeCd"
          placeholder="부서코드"
          :disabled="nodeDisabled"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="nodeDisabled"
          @click="fnSiteNodeSearchPopOpen()"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="nodeNm"
          type="text"
          v-model="nodeNm"
          placeholder="부서명"
          :disabled="nodeDisabled"
          @blur="focusKill"
        />
      </div>
      <div>
        <label class="checkbox-label">
          <input type="checkbox" v-model="incSubNodeYn" :disabled="!nodeCd" />
          하위부서 조회
        </label>
      </div>
      <div>
        <label>사용자명</label>
        <input v-model.trim="searchUserNm" type="text" />
      </div>
    </div>

    <!-- 본문: 좌측 결과 테이블 / 우측 상세 패널 -->
    <div class="viewBody a08-body" :class="{ 'detail-open': !!selected }">
      <div class="a08-table-wrap">
        <table class="a08-table">
          <thead>
            <tr>
              <th>사용자명</th>
              <th>부서</th>
              <th>근무일</th>
              <th>요일</th>
              <th>차수</th>
              <th>스케줄(1구간)</th>
              <th>스케줄(2구간)</th>
              <th>실제(1구간)</th>
              <th>실제(2구간)</th>
              <th>정규화(1구간)</th>
              <th>정규화(2구간)</th>
              <th>상태</th>
              <th>외근</th>
              <th>상세</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="rows.length === 0">
              <td colspan="14" class="a08-empty">조회 결과가 없습니다.</td>
            </tr>
            <tr
              v-for="r in rows"
              :key="r.attdId"
              :class="{ 'row-active': selected && selected.attdId === r.attdId }"
              @click="fnSelectRow(r)"
            >
              <td>{{ r.userNm }}</td>
              <td>{{ r.nodeNm }}</td>
              <td>{{ fmtYmd(r.workYmd) }}</td>
              <td :class="dowClass(r.workYmd)">{{ dowLabel(r.workYmd) }}</td>
              <td>{{ r.workSeq }}</td>
              <td>{{ planRange(r.plan1Start, r.plan1End) }}</td>
              <td>{{ planRange(r.plan2Start, r.plan2End) }}</td>
              <td>{{ actRange(r.act1InTime, r.act1OutTime) }}</td>
              <td>{{ actRange(r.act2InTime, r.act2OutTime) }}</td>
              <td>{{ actRange(r.act1InStdTime, r.act1OutStdTime) }}</td>
              <td>{{ actRange(r.act2InStdTime, r.act2OutStdTime) }}</td>
              <td>
                <span :class="['a08-badge', statusBadgeClass(r.attdStatusCd)]">
                  {{ statusLabel(r.attdStatusCd) }}
                </span>
              </td>
              <td>
                <span
                  :class="[
                    'a08-badge',
                    r.isOutsideYn === 'Y' ? 'b-out' : 'b-in',
                  ]"
                >
                  {{ r.isOutsideYn === "Y" ? "외근" : "내근" }}
                </span>
              </td>
              <td>
                <button class="a08-btn-detail" @click.stop="fnSelectRow(r)">
                  상세
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 상세 패널 (행 클릭 시 표시) -->
      <div v-if="selected" class="a08-detail-panel">
        <div class="a08-detail-head">
          <div>
            <div class="a08-detail-title">
              {{ selected.userNm }} ({{ selected.userId }})
            </div>
            <div class="a08-detail-sub">
              {{ selected.nodeNm }} · {{ fmtYmd(selected.workYmd) }} · 차수
              {{ selected.workSeq }}
            </div>
          </div>
          <button class="a08-detail-close" @click="fnCloseDetail">×</button>
        </div>

        <div class="a08-detail-meta">
          <div class="meta-row">
            <span class="meta-label">스케줄(1구간)</span>
            <span class="meta-value">{{
              planRange(selected.plan1Start, selected.plan1End)
            }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">스케줄(2구간)</span>
            <span class="meta-value">{{
              planRange(selected.plan2Start, selected.plan2End)
            }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">실제(1구간)</span>
            <span class="meta-value">{{
              actRange(selected.act1InTime, selected.act1OutTime)
            }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">실제(2구간)</span>
            <span class="meta-value">{{
              actRange(selected.act2InTime, selected.act2OutTime)
            }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">상태</span>
            <span
              class="meta-value"
              :class="['a08-badge', statusBadgeClass(selected.attdStatusCd)]"
            >
              {{ statusLabel(selected.attdStatusCd) }}
            </span>
          </div>
          <div class="meta-row">
            <span class="meta-label">외근여부</span>
            <span
              class="meta-value"
              :class="[
                'a08-badge',
                selected.isOutsideYn === 'Y' ? 'b-out' : 'b-in',
              ]"
            >
              {{ selected.isOutsideYn === "Y" ? "외근" : "내근" }}
            </span>
          </div>
        </div>

        <!-- 지도 영역: 외근일 때만 GPS 호출 -->
        <div class="a08-map-section">
          <div class="a08-map-title">GPS 동선</div>
          <div
            v-if="selected.isOutsideYn !== 'Y'"
            class="a08-map-empty"
          >
            내근 근태로 GPS 기록이 없습니다.
          </div>
          <div
            v-else-if="gpsLoading"
            class="a08-map-empty"
          >
            GPS 정보를 불러오는 중...
          </div>
          <div
            v-else-if="gpsList.length === 0"
            class="a08-map-empty"
          >
            수집된 GPS 좌표가 없습니다.
          </div>
          <div
            v-else
            id="a08-kakao-map"
            ref="mapContainer"
            class="a08-map-canvas"
          ></div>

          <div v-if="gpsList.length > 0" class="a08-gps-summary">
            총 <b>{{ gpsList.length }}</b
            >건
            <span v-if="mockedCount > 0" class="mocked-warn">
              (Mock 좌표 {{ mockedCount }}건 포함)
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  getCurrentInstance,
  defineProps,
  defineOptions,
  onBeforeUnmount,
  onMounted,
  nextTick,
} from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { useModal } from "@/utils/useModal";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";

defineOptions({ name: "Attd_08" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const localButtons = ref({ ...props.buttons });

// hide non-search buttons
(() => {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
})();

// 조회 조건
const fromDate = ref(defaultFrom());
const toDate = ref(defaultTo());
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteDisabled = ref(false);
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true);
const incSubNodeYn = ref(false);
const searchUserNm = ref("");
const siteNoFcs = ref(null);

const dowLabels = ["일", "월", "화", "수", "목", "금", "토"];

function defaultFrom() {
  const d = new Date();
  d.setMonth(d.getMonth() - 1);
  return toIsoDate(d);
}
function defaultTo() {
  return toIsoDate(new Date());
}
function toIsoDate(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

// 사업장/부서 자동조회 처리 (Attd_07 패턴 차용)
const focusKill = (e) => {
  if (e.target.id === "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    } else {
      siteNm.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id === "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    } else {
      siteNo.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id === "nodeCd") {
    if (proxy.$util.isEmpty(nodeCd.value)) {
      nodeNm.value = "";
    } else {
      nodeNm.value = "";
      fnSrchNodeInfo();
    }
  } else if (e.target.id === "nodeNm") {
    if (proxy.$util.isEmpty(nodeNm.value)) {
      nodeCd.value = "";
    } else {
      nodeCd.value = "";
      fnSrchNodeInfo();
    }
  }
};

const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: siteNo.value,
        siteNm: siteNm.value,
      },
    });
    if (response.status === 200) fnCallback(response);
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

const fnSrchNodeInfo = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) return;
  try {
    const response = await axios.get("/comApi/baseinfo/site-node-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        nodeNm: nodeNm.value,
      },
    });
    if (response.status === 200) {
      fnCallback({ ...response, config: { url: "/dummy/site-node-lists" } });
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

const fnCallback = (res) => {
  if (!proxy.$util.isNotEmpty(res)) return;
  const apiId = res.config.url.split("/").pop();
  if (apiId === "site-lists") {
    const list = res.data?.siteInfoResultList ?? [];
    if (list.length === 1) {
      siteCd.value = list[0].siteCd;
      siteNo.value = list[0].siteNo;
      siteNm.value = list[0].siteNm;
      nodeDisabled.value = false;
    } else if (list.length > 1) {
      fnSiteSearchPopOpen();
    } else {
      siteCd.value = "";
      siteNo.value = "";
      siteNm.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    }
  } else if (apiId === "site-node-lists") {
    const list = res.data?.siteNodeInfoList || [];
    if (list.length === 0) {
      nodeCd.value = "";
      nodeNm.value = "";
    } else if (list.length === 1) {
      nodeCd.value = list[0].nodeCd ?? "";
      nodeNm.value = list[0].nodeNm ?? "";
    } else {
      fnSiteNodeSearchPopOpen();
    }
  }
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
  nodeDisabled.value = false;
  nodeCd.value = "";
  nodeNm.value = "";
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

const fnSiteNodeSearchPopOpen = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED_FIRST));
    return;
  }
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    nodeCd_p: "",
    userCd_p: "",
    onSelect: (nodeCdVal, nodeNmVal) => {
      nodeCd.value = nodeCdVal ?? "";
      nodeNm.value = nodeNmVal ?? "";
    },
  });
};

// 결과
const rows = ref([]);
const selected = ref(null);

// 클라이언트측 기간 검증 (≤3개월)
function isWithinThreeMonths(fromIso, toIso) {
  if (!fromIso || !toIso) return false;
  const f = new Date(fromIso);
  const t = new Date(toIso);
  if (isNaN(f.getTime()) || isNaN(t.getTime())) return false;
  if (f > t) return false;
  const limit = new Date(f);
  limit.setMonth(limit.getMonth() + 3);
  return t <= limit;
}

// 조회 실행
const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert("사업장을 선택해 주세요.");
    return;
  }
  if (proxy.$util.isEmpty(fromDate.value) || proxy.$util.isEmpty(toDate.value)) {
    await proxy.$alert("조회 기간을 입력해 주세요.");
    return;
  }
  if (!isWithinThreeMonths(fromDate.value, toDate.value)) {
    await proxy.$alert("조회 기간은 최대 3개월까지만 가능합니다.");
    return;
  }

  try {
    const response = await axios.get("/webApi/attd08/attd-lists", {
      params: {
        fromDate: fromDate.value,
        toDate: toDate.value,
        siteCd: siteCd.value,
        nodeCd: nodeCd.value || "",
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        userNm: searchUserNm.value || "",
      },
    });
    if (response.status === 200) {
      rows.value = response.data?.attdListsResultList ?? [];
      // 상세 닫기 (조회 결과가 갱신됨)
      fnCloseDetail();
    }
  } catch (err) {
    console.error("[Attd_08] search failed", err);
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

// 표시 헬퍼
const fmtYmd = (ymd) => {
  const s = String(ymd ?? "");
  if (s.length !== 8) return s;
  return `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}`;
};
const fmtTime = (hhmm) => {
  if (!hhmm) return "";
  const v = String(hhmm);
  if (v.length < 4) return v;
  return `${v.slice(0, 2)}:${v.slice(2, 4)}`;
};
const planRange = (a, b) => {
  if (!a && !b) return "-";
  return `${fmtTime(a) || "-"} ~ ${fmtTime(b) || "-"}`;
};
const actRange = (a, b) => {
  if (!a && !b) return "-";
  return `${fmtTime(a) || "-"} ~ ${fmtTime(b) || "-"}`;
};
const dowLabel = (ymd) => {
  const s = String(ymd ?? "");
  if (s.length !== 8) return "";
  const d = new Date(
    Number(s.slice(0, 4)),
    Number(s.slice(4, 6)) - 1,
    Number(s.slice(6, 8))
  );
  return dowLabels[d.getDay()] || "";
};
const dowClass = (ymd) => {
  const s = String(ymd ?? "");
  if (s.length !== 8) return "";
  const d = new Date(
    Number(s.slice(0, 4)),
    Number(s.slice(4, 6)) - 1,
    Number(s.slice(6, 8))
  );
  const day = d.getDay();
  if (day === 0) return "dow-sun";
  if (day === 6) return "dow-sat";
  return "";
};
const statusLabel = (cd) => {
  switch (cd) {
    case "LATE":
      return "지각";
    case "EARLY_LEAVE":
      return "조퇴";
    case "ABSENT":
      return "결근";
    case "NORMAL":
    default:
      return "정상";
  }
};
const statusBadgeClass = (cd) => {
  switch (cd) {
    case "LATE":
      return "b-late";
    case "EARLY_LEAVE":
      return "b-early";
    case "ABSENT":
      return "b-absent";
    case "NORMAL":
    default:
      return "b-normal";
  }
};

// 상세 패널 + 지도
const mapContainer = ref(null);
const gpsList = ref([]);
const gpsLoading = ref(false);
const mockedCount = computed(
  () => gpsList.value.filter((g) => g.isMocked === "Y").length
);

let kakaoMap = null;
let kakaoMarkers = [];
let kakaoPolyline = null;

const fnSelectRow = async (r) => {
  selected.value = r;
  // 지도/GPS 초기화
  cleanupMap();
  gpsList.value = [];

  if (r.isOutsideYn !== "Y") return;

  gpsLoading.value = true;
  try {
    const response = await axios.get("/webApi/attd08/attd-gps-trail", {
      params: { attdId: r.attdId },
    });
    if (response.status === 200) {
      gpsList.value = response.data?.attdGpsTrailResultList ?? [];
    }
  } catch (err) {
    console.error("[Attd_08] gps trail load failed", err);
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
    gpsList.value = [];
  } finally {
    gpsLoading.value = false;
  }

  if (gpsList.value.length === 0) return;

  await nextTick();
  await renderMap();
};

const fnCloseDetail = () => {
  selected.value = null;
  cleanupMap();
  gpsList.value = [];
};

// Kakao Map loader (SiteInfoPop 패턴 차용)
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

const renderMap = async () => {
  if (!mapContainer.value) return;
  try {
    await loadKakaoMapScript();
  } catch (e) {
    console.error("[Attd_08] kakao load fail:", e);
    return;
  }

  const points = gpsList.value
    .map((g) => {
      const lat = Number(g.lat);
      const lon = Number(g.lon);
      if (isNaN(lat) || isNaN(lon)) return null;
      return { lat, lon, isMocked: g.isMocked === "Y", raw: g };
    })
    .filter(Boolean);

  if (points.length === 0) return;

  // 지도 생성 (첫 좌표 중심)
  const center = new window.kakao.maps.LatLng(points[0].lat, points[0].lon);
  kakaoMap = new window.kakao.maps.Map(mapContainer.value, {
    center,
    level: 4,
  });

  // 마커: 정상=파랑, mock=빨강 (기본 핀 색 차이용 SVG image)
  const normalImage = new window.kakao.maps.MarkerImage(
    "data:image/svg+xml;base64," +
      btoa(
        '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="32" viewBox="0 0 24 32"><path d="M12 0C5.4 0 0 5.4 0 12c0 9 12 20 12 20s12-11 12-20C24 5.4 18.6 0 12 0z" fill="#3B82F6"/><circle cx="12" cy="12" r="5" fill="#fff"/></svg>'
      ),
    new window.kakao.maps.Size(24, 32),
    { offset: new window.kakao.maps.Point(12, 32) }
  );
  const mockedImage = new window.kakao.maps.MarkerImage(
    "data:image/svg+xml;base64," +
      btoa(
        '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="32" viewBox="0 0 24 32"><path d="M12 0C5.4 0 0 5.4 0 12c0 9 12 20 12 20s12-11 12-20C24 5.4 18.6 0 12 0z" fill="#EF4444"/><circle cx="12" cy="12" r="5" fill="#fff"/></svg>'
      ),
    new window.kakao.maps.Size(24, 32),
    { offset: new window.kakao.maps.Point(12, 32) }
  );

  const bounds = new window.kakao.maps.LatLngBounds();
  const path = [];
  for (const p of points) {
    const pos = new window.kakao.maps.LatLng(p.lat, p.lon);
    const marker = new window.kakao.maps.Marker({
      map: kakaoMap,
      position: pos,
      image: p.isMocked ? mockedImage : normalImage,
    });
    kakaoMarkers.push(marker);
    path.push(pos);
    bounds.extend(pos);
  }

  if (path.length >= 2) {
    kakaoPolyline = new window.kakao.maps.Polyline({
      path,
      strokeWeight: 3,
      strokeColor: "#3B82F6",
      strokeOpacity: 0.8,
      strokeStyle: "solid",
    });
    kakaoPolyline.setMap(kakaoMap);
  }

  if (path.length === 1) {
    kakaoMap.setCenter(path[0]);
    kakaoMap.setLevel(4);
  } else {
    kakaoMap.setBounds(bounds);
  }
};

const cleanupMap = () => {
  if (kakaoPolyline) {
    try {
      kakaoPolyline.setMap(null);
    } catch (_e) {
      void 0;
    }
    kakaoPolyline = null;
  }
  for (const m of kakaoMarkers) {
    try {
      m.setMap(null);
    } catch (_e) {
      void 0;
    }
  }
  kakaoMarkers = [];
  kakaoMap = null;
};

// 진입 시 sessionStorage 의 사업장 정보로 초기화 (Attd_05 패턴 차용)
const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
  if (siteCd.value) {
    nodeDisabled.value = false;
  }
};

onMounted(() => {
  fnInit();
});

onBeforeUnmount(() => {
  cleanupMap();
});
</script>

<style scoped>
/* viewBody 위에 덮어쓰는 레이아웃 (Attd_05 패턴 차용).
   - flex 컨테이너 + min-height:0 으로 내부 wrap 이 100% 높이를 갖도록 함
   - height: calc 제거 → 하단 빈 공간/끊김 현상 해소 */
.a08-body {
  display: flex;
  flex-direction: row;
  gap: 1rem;
  padding: 0.75rem;
  overflow: hidden;
  min-height: 0;
}
.a08-table-wrap {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 6px;
  background: #fff;
}
.a08-body.detail-open .a08-table-wrap {
  flex: 1 1 60%;
}

.a08-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}
.a08-table thead th {
  background: var(--thead-bg, #f3f4f6);
  border-bottom: 2px solid var(--color-border, #d1d5db);
  padding: 0.5rem 0.4rem;
  position: sticky;
  top: 0;
  z-index: 1;
  text-align: center;
  white-space: nowrap;
}
.a08-table tbody td {
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  padding: 0.4rem;
  text-align: center;
  white-space: nowrap;
}
/* 마지막 행 하단 보더 제거 → wrap 의 border 와 이중선 방지 */
.a08-table tbody tr:last-child td {
  border-bottom: none;
}
.a08-table tbody tr {
  cursor: pointer;
}
.a08-table tbody tr:hover {
  background: #f9fafb;
}
.a08-table tbody tr.row-active {
  background: #eef2ff;
}
.a08-empty {
  padding: 2rem !important;
  color: #9ca3af;
  text-align: center;
}
.dow-sun {
  color: #ef4444;
}
.dow-sat {
  color: #2563eb;
}

.a08-badge {
  display: inline-block;
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
}
.b-normal {
  background: #d1fae5;
  color: #065f46;
}
.b-late {
  background: #fef3c7;
  color: #92400e;
}
.b-early {
  background: #fde68a;
  color: #92400e;
}
.b-absent {
  background: #fee2e2;
  color: #991b1b;
}
.b-out {
  background: #dbeafe;
  color: #1e40af;
}
.b-in {
  background: #f3f4f6;
  color: #374151;
}

.a08-btn-detail {
  padding: 0.2rem 0.6rem;
  font-size: 0.75rem;
  border: 1px solid #d1d5db;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
}
.a08-btn-detail:hover {
  background: #f3f4f6;
}

.a08-detail-panel {
  flex: 0 0 36rem;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.a08-detail-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e5e7eb;
  background: #f9fafb;
}
.a08-detail-title {
  font-weight: 600;
  font-size: 1rem;
}
.a08-detail-sub {
  font-size: 0.8rem;
  color: #6b7280;
  margin-top: 0.2rem;
}
.a08-detail-close {
  background: transparent;
  border: none;
  font-size: 1.4rem;
  cursor: pointer;
  color: #6b7280;
  line-height: 1;
}
.a08-detail-meta {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #f1f5f9;
}
.meta-row {
  display: flex;
  align-items: center;
  font-size: 0.85rem;
  padding: 0.2rem 0;
}
.meta-label {
  flex: 0 0 8rem;
  color: #6b7280;
}
.meta-value {
  flex: 1;
}

.a08-map-section {
  padding: 0.75rem 1rem;
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-height: 18rem;
}
.a08-map-title {
  font-weight: 600;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}
.a08-map-canvas {
  flex: 1 1 auto;
  width: 100%;
  min-height: 16rem;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
}
.a08-map-empty {
  flex: 1 1 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 0.85rem;
  background: #f9fafb;
  border: 1px dashed #e5e7eb;
  border-radius: 4px;
  min-height: 16rem;
}
.a08-gps-summary {
  margin-top: 0.4rem;
  font-size: 0.8rem;
  color: #4b5563;
}
.mocked-warn {
  color: #b91c1c;
  font-weight: 600;
  margin-left: 0.4rem;
}

/* Attd_07 패턴: viewSearch 의 div 간 gap(2rem)을 일부 상쇄해
   소속부서 입력 뭉치와 가깝게 붙여 보이게 한다. */
.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.85rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  user-select: none;
  margin-left: -1rem;
  margin-right: 0.4rem;
  white-space: nowrap;
}
.checkbox-label input[type="checkbox"] {
  width: 13px;
  height: 13px;
  cursor: pointer;
  accent-color: var(--color-primary, #16a34a);
  flex-shrink: 0;
}
</style>
