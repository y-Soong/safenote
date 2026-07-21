<template>
  <!--
    PlatformSiteSearchPop.vue — 플랫폼 콘솔 전용 사업장 검색 팝업 (Platform_04 위치정보 열람)
    - 표준 SiteSearchPop 패턴을 따르되, 스코프가 다르다:
      · 표준: /comApi/baseinfo/site-lists (세션 회사 스코프)
      · 플랫폼: /platformApi/location/site-lists?cmpnyCd=대상회사 (SMS 게이트 인가)
    - 반환: onSelect(site) — { siteCd, siteNo, siteNm, lat, lon, gpsRange } 전량 전달
      (Platform_04 지도가 좌표/지오펜스에 lat/lon/gpsRange 를 사용하므로 코드/명칭만이 아닌 행 전체를 넘긴다).
    - 서버는 cmpnyCd 만 파라미터로 받으므로 사업장번호/명 검색은 클라이언트 필터로 처리한다.
  -->
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- Title -->
        <div class="modal-header" @mousedown="startDrag">
          <span>사업장 검색</span>
          <button class="icon-button" @click="$emit('close')">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              class="w-6 h-6"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <!-- 조회 Form -->
        <div class="viewSearch">
          <div class="form-left">
            <label>사업장번호</label>
            <input v-model.trim="srchSiteNo" @keyup.enter="fnApplyFilter" />
            <label>사업장명</label>
            <input v-model.trim="srchSiteNm" @keyup.enter="fnApplyFilter" />
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnApplyFilter">조회</button>
          </div>
        </div>

        <!-- 그리드 -->
        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th style="display: none">사업장코드</th>
                  <ThSortable
                    label="사업장번호"
                    col-key="siteNo"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.siteNo"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="사업장명"
                    col-key="siteNm"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.siteNm"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                </tr>
              </thead>
              <tbody>
                <template v-if="!sortedData || sortedData.length === 0">
                  <tr>
                    <td colspan="2" class="edu-grid-empty">
                      등록된 세부 항목이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr
                    v-for="site in sortedData"
                    :key="site.siteCd"
                    @dblclick="fnSelectRow(site)"
                  >
                    <td style="display: none">{{ site.siteCd }}</td>
                    <td>{{ site.siteNo }}</td>
                    <td>{{ site.siteNm }}</td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import {
  defineProps,
  defineEmits,
  ref,
  computed,
  getCurrentInstance,
  onMounted,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

const props = defineProps({
  // 대상 회사코드(Platform_04 에서 선택한 회사). 없으면 조회 불가.
  cmpnyCd_p: String,
  // 선택 콜백 — 행 전체({ siteCd, siteNo, siteNm, lat, lon, gpsRange })를 전달.
  onSelect: Function,
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// 서버에서 받은 전량(회사 단위) — 검색은 클라이언트 필터
const allSites = ref([]);
const srchSiteNo = ref("");
const srchSiteNm = ref("");

// 필터 적용 결과를 정렬 훅에 연결
const filtered = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(filtered);
const { colWidths, onResize } = useColumnResize({
  siteNo: 160,
  siteNm: 240,
});

const fnApplyFilter = () => {
  const no = srchSiteNo.value;
  const nm = srchSiteNm.value;
  filtered.value = allSites.value.filter((s) => {
    const okNo = !no || (s.siteNo && String(s.siteNo).includes(no));
    const okNm = !nm || (s.siteNm && String(s.siteNm).includes(nm));
    return okNo && okNm;
  });
};

const fnLoadSites = async () => {
  allSites.value = [];
  filtered.value = [];
  if (proxy.$util.isEmpty(props.cmpnyCd_p)) return;

  try {
    const response = await axios.get("/platformApi/location/site-lists", {
      params: { cmpnyCd: props.cmpnyCd_p },
    });
    if (response.status === 200) {
      allSites.value = response.data?.siteList || [];
      fnApplyFilter();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "사업장 목록 조회 중 오류가 발생했습니다.")
    );
  }
};

const fnSelectRow = (site) => {
  props.onSelect?.(site);
  emit("close");
};

onMounted(fnLoadSites);
</script>
