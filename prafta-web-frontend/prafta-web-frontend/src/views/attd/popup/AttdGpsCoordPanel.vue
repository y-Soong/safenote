<template>
  <div class="gps-panel">
    <div class="gps-panel__title">외근 GPS 동선</div>

    <!-- loading -->
    <div v-if="loading" class="gps-panel__empty">
      GPS 정보를 불러오는 중...
    </div>

    <!-- empty -->
    <div v-else-if="trail.length === 0" class="gps-panel__empty">
      수집된 GPS 좌표가 없습니다.
    </div>

    <!-- error: 카카오 SDK 로드 실패 -->
    <div v-else-if="mapError" class="gps-panel__empty gps-panel__empty--error">
      지도를 불러오지 못했습니다.
    </div>

    <!-- success: 카카오맵 캔버스 -->
    <div
      v-else
      ref="mapContainer"
      class="gps-panel__canvas"
    ></div>

    <div v-if="trail.length > 0 && !loading" class="gps-panel__summary">
      총 <b>{{ trail.length }}</b
      >건
      <span v-if="mockedCount > 0" class="gps-panel__mocked-warn">
        (Mock 좌표 {{ mockedCount }}건 포함)
      </span>
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
} from "vue";

const props = defineProps({
  // 시간 오름차순 GPS 좌표 배열 [{ lat, lon, recordTime?, isMocked? }]
  trail: { type: Array, default: () => [] },
  // 부모의 trail 조회 로딩 상태
  loading: { type: Boolean, default: false },
});

// 표시 상태
const mapContainer = ref(null);
const mapError = ref(false);

// Mock 좌표 건수 (표시 전용 계산)
const mockedCount = computed(
  () =>
    props.trail.filter((g) => g.isMocked === "Y" || g.isMocked === true).length
);

// 카카오맵 인스턴스 핸들 (반응형 불필요 — 일반 변수)
let kakaoMap = null;
let kakaoMarkers = [];
let kakaoPolyline = null;

/*
 * 카카오맵 SDK 동적 로더 — Attd_08.vue 패턴 차용.
 * 중복 로드 가드 + 10초 타임아웃 포함.
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
 * 지도 렌더 — Attd_08.vue renderMap 패턴 차용.
 * 첫 점=출근 마커, 마지막 점=퇴근 마커로 색/라벨 구분.
 */
const renderMap = async () => {
  if (!mapContainer.value) return;

  mapError.value = false;
  try {
    await loadKakaoMapScript();
  } catch (e) {
    console.error("[AttdGpsCoordPanel] 카카오 지도 로드 실패:", e);
    mapError.value = true;
    return;
  }

  // props.trail -> 유효 좌표 배열로 변환 (시간 오름차순 가정)
  const points = props.trail
    .map((g) => {
      const lat = Number(g.lat);
      const lon = Number(g.lon);
      if (isNaN(lat) || isNaN(lon)) return null;
      return {
        lat,
        lon,
        isMocked: g.isMocked === "Y" || g.isMocked === true,
        raw: g,
      };
    })
    .filter(Boolean);

  if (points.length === 0) return;

  // 지도 생성 (첫 좌표 중심)
  const center = new window.kakao.maps.LatLng(points[0].lat, points[0].lon);
  kakaoMap = new window.kakao.maps.Map(mapContainer.value, {
    center,
    level: 4,
  });

  // 마커 이미지 — 출근(초록)/퇴근(빨강) 구분
  const buildPinImage = (color) =>
    new window.kakao.maps.MarkerImage(
      "data:image/svg+xml;base64," +
        btoa(
          `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="32" viewBox="0 0 24 32"><path d="M12 0C5.4 0 0 5.4 0 12c0 9 12 20 12 20s12-11 12-20C24 5.4 18.6 0 12 0z" fill="${color}"/><circle cx="12" cy="12" r="5" fill="#fff"/></svg>`
        ),
      new window.kakao.maps.Size(24, 32),
      { offset: new window.kakao.maps.Point(12, 32) }
    );

  // CSS 토큰과 일치하는 색상값 사용 (primary / danger)
  const startImage = buildPinImage("#16a34a"); // 출근
  const endImage = buildPinImage("#ef4444"); // 퇴근

  // 중간 점 — 작은 원형 마커. 정상 좌표는 회색, Mock 좌표(isMocked)는 빨강으로 구분.
  //   Attd_08.vue 의 normalImage/mockedImage 색 구분 패턴을 원형 마커로 적용.
  const buildDotImage = (color) =>
    new window.kakao.maps.MarkerImage(
      "data:image/svg+xml;base64," +
        btoa(
          `<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 12 12"><circle cx="6" cy="6" r="5" fill="${color}" stroke="#fff" stroke-width="1.5"/></svg>`
        ),
      new window.kakao.maps.Size(12, 12),
      { offset: new window.kakao.maps.Point(6, 6) }
    );
  const midDotImage = buildDotImage("#6b7280"); // 정상 중간 점
  const mockedDotImage = buildDotImage("#ef4444"); // Mock 중간 점

  const bounds = new window.kakao.maps.LatLngBounds();
  const path = [];
  const lastIdx = points.length - 1;

  points.forEach((p, idx) => {
    const pos = new window.kakao.maps.LatLng(p.lat, p.lon);
    path.push(pos);
    bounds.extend(pos);

    // 첫 점=출근, 마지막 점=퇴근. 그 외 중간 점.
    const isStart = idx === 0;
    const isEnd = idx === lastIdx && lastIdx !== 0;

    if (isStart || isEnd) {
      const marker = new window.kakao.maps.Marker({
        map: kakaoMap,
        position: pos,
        image: isStart ? startImage : endImage,
      });
      kakaoMarkers.push(marker);

      // 출근/퇴근 라벨 (CustomOverlay)
      const label = new window.kakao.maps.CustomOverlay({
        map: kakaoMap,
        position: pos,
        yAnchor: 2.2,
        content: `<div class="gps-pin-label ${
          isStart ? "is-start" : "is-end"
        }">${isStart ? "출근" : "퇴근"}</div>`,
      });
      kakaoMarkers.push(label);
    } else {
      // 중간 점 — 작은 원형 마커. Mock 좌표는 빨강으로 구분 표시.
      const dot = new window.kakao.maps.Marker({
        map: kakaoMap,
        position: pos,
        image: p.isMocked ? mockedDotImage : midDotImage,
        zIndex: 1,
      });
      kakaoMarkers.push(dot);
    }
  });

  // 중간 점 폴리라인 트레일 (시간순 연결)
  if (path.length >= 2) {
    kakaoPolyline = new window.kakao.maps.Polyline({
      path,
      strokeWeight: 3,
      strokeColor: "#16a34a",
      strokeOpacity: 0.8,
      strokeStyle: "solid",
    });
    kakaoPolyline.setMap(kakaoMap);
  }

  // 자동 줌
  if (path.length === 1) {
    kakaoMap.setCenter(path[0]);
    kakaoMap.setLevel(4);
  } else {
    kakaoMap.setBounds(bounds);
  }
};

/*
 * 지도 정리 — Attd_08.vue cleanupMap 패턴 차용.
 */
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

// trail 변경 시 재렌더 (cleanup 후 renderMap)
//   mapContainer 는 success 분기(v-else)에서만 mount 되므로, trail 도착으로
//   loading=false && trail.length>0 이 된 직후 nextTick 으로 DOM 반영을 기다린 뒤
//   렌더한다. nextTick 후에도 mapContainer 가 없으면 renderMap 내부 가드로 무시된다.
watch(
  () => props.trail,
  async () => {
    cleanupMap();
    if (props.loading || props.trail.length === 0) return;
    await nextTick();
    await renderMap();
  },
  { deep: true }
);

// loading 종료 시점에도 재렌더 보장 — 부모가 trail 을 먼저 세팅하고
// loading 을 나중에 false 로 내리는 순서일 때 mapContainer 가 그제서야 mount 된다.
watch(
  () => props.loading,
  async (isLoading) => {
    if (isLoading || props.trail.length === 0) return;
    await nextTick();
    await renderMap();
  }
);

onMounted(async () => {
  if (!props.loading && props.trail.length > 0) {
    await nextTick();
    await renderMap();
  }
});

onBeforeUnmount(() => {
  cleanupMap();
});
</script>

<style scoped>
/* Attd_08.vue a08-map-section 스타일 패턴 차용 */
.gps-panel {
  display: flex;
  flex-direction: column;
  min-height: 18rem;
}

.gps-panel__title {
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--color-text-strong);
  margin-bottom: 0.5rem;
}

.gps-panel__canvas {
  flex: 1 1 auto;
  width: 100%;
  min-height: 16rem;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}

.gps-panel__empty {
  flex: 1 1 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 16rem;
  font-size: 0.85rem;
  color: var(--color-text-muted);
  background: var(--color-bg);
  border: 1px dashed var(--color-border);
  border-radius: var(--input-radius);
}

.gps-panel__empty--error {
  color: var(--color-danger);
}

.gps-panel__summary {
  margin-top: 0.4rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.gps-panel__mocked-warn {
  color: var(--color-danger);
  font-weight: 600;
  margin-left: 0.4rem;
}

/* 출근/퇴근 핀 라벨 — CustomOverlay content 는 .gps-panel__canvas(scoped 데이터
   속성 보유) 하위에 삽입되므로 :deep() 로 자식 셀렉터를 관통시켜 스타일이 적용된다.
   (canvas 컨테이너 자체가 이 컴포넌트 스코프에 속하므로 전역 스타일 불필요) */
:deep(.gps-pin-label) {
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: var(--color-surface);
  white-space: nowrap;
}

:deep(.gps-pin-label.is-start) {
  background: var(--color-primary);
}

:deep(.gps-pin-label.is-end) {
  background: var(--color-danger);
}
</style>
