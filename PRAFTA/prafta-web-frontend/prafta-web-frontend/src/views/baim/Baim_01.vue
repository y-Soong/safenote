<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @save="fnSave"
      @create="fnCreate"
    />
    <!-- 
      @create="fnCreate"
      @save="fnSave"
      @delete="fnDelete"
    @excel="fnExcel"-->

    <div class="viewSearch">
      <div>
        <label>사업장번호</label>
        <input v-model.trim="siteNo" type="text" />
      </div>
      <div>
        <label>사업장명</label>
        <input v-model.trim="siteNm" type="text" />
      </div>

      <div>
        <label>사용여부</label>
        <select v-model.trim="useYn" name="combo">
          <option
            v-for="opt in systCodeArr['SYS003'] || []"
            :key="opt.systValDCd"
            :value="opt.systValDCd"
          >
            {{ opt.systValDNm }}
          </option>
        </select>
      </div>
    </div>

    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <!-- ⬇️ 소제목 바 -->
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <!-- 단순 마크 아이콘 (SVG) -->
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">사업장 리스트</span>
        </div>

        <div
          class="table-box"
          style="--box-h: 70vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table
            class="data-grid w-full table-fixed text-sm text-left rtl:text-right"
          >
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 2%">
                  No
                </th>
                <th style="width: 4%">
                  <input
                    id="headChk"
                    v-model="headChk"
                    type="checkbox"
                    @click="fnHeadChk"
                  />
                </th>
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
                <ThSortable
                  label="주소"
                  col-key="addr1"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.addr1"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="상세주소"
                  col-key="addr2"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.addr2"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="GPS허용범위"
                  col-key="gpsRange"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.gpsRange"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th class="editableCell" style="width: 8%">사용여부</th>
                <ThSortable
                  label="사업개시일"
                  col-key="strDate"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.strDate"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="사업종료일"
                  col-key="endDate"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.endDate"
                  @sort="onSort"
                  @update:width="onResize"
                />
              </tr>
            </thead>
            <tbody>
              <template v-if="!siteInfoList || siteInfoList.length === 0">
                <tr>
                  <td colspan="10" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(site, idx) in sortedData"
                  :key="site.id"
                  class="row-clickable"
                  @dblclick="fnSiteInfoPopOpen(site)"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input type="checkbox" v-model="site.chk" />
                  </td>
                  <td>{{ site.siteNo }}</td>
                  <td>
                    {{ site.siteNm
                    }}<span v-if="site.linkSrcCmpnyCd" class="link-badge"
                      >연동</span
                    >
                  </td>
                  <td>{{ site.addr1 }}</td>
                  <td>{{ site.addr2 }}</td>
                  <td>
                    {{ site.gpsRange }}
                  </td>
                  <td>
                    <!-- PRAFTA-SUBCON-T2-09: 미러(연동) 사업장은 사용여부(잠금 필드) 인라인 편집 비활성.
                         강제는 서버(T2-04)가 담당 — UI 는 안내 목적. -->
                    <BaseSelect
                      v-model="site.useYn"
                      :disabled="!!site.linkSrcCmpnyCd"
                    >
                      <option
                        v-for="opt in (systCodeArr['SYS003'] || []).filter(
                          (o) => o.systValDCd != null
                        )"
                        :key="opt.systValDCd"
                        :value="opt.systValDCd"
                      >
                        {{ opt.systValDNm }}
                      </option>
                    </BaseSelect>
                  </td>
                  <td>
                    <CalendarSrch
                      :range="false"
                      :readonly="true"
                      style="width: 150px"
                      v-model="site.strDate"
                    />
                  </td>
                  <td>
                    <CalendarSrch
                      :range="false"
                      :readonly="true"
                      style="width: 150px"
                      v-model="site.endDate"
                    />
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ================ Imports ================
import {
  ref,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useModal } from "@/utils/useModal";
import { useFieldWatcher } from "@/utils/useFieldWatcher";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import BaseSelect from "@/components/common/BaseSelect.vue";
import SiteInfoPop from "./popup/SiteInfoPop.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

// ================ Options ================
defineOptions({ name: "Baim_01" });

// ================ Props & Emits ================
const props = defineProps({
  title: String,
  buttons: Object,
});

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ================ Refs (Variables) ================
const localButtons = ref({ ...props.buttons });
const siteInfoList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(siteInfoList);
const { colWidths, onResize } = useColumnResize({
  siteNo: 110,
  siteNm: 120,
  addr1: 200,
  addr2: 160,
  gpsRange: 110,
  strDate: 110,
  endDate: 110,
});
const systCodeArr = ref({});
// 회사 통상근로시간 기준값(분) — 사업장 팝업의 "회사 기본값 사용" 라벨 표기용.
const cmpnyWeekStdMinutes = ref(null);

// 조회조건 변수
const siteNo = ref("");
const siteNm = ref("");
const useYn = ref("Y");

// 화면 제어 변수
const p_siteCd = ref("");
const headChk = ref(false);

// ================ Watchers ================
useFieldWatcher(
  siteInfoList,
  (item) => {
    item.chk = true;
  },
  ["chk"]
);

// ================ Life Cycle Functions ================
onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
  await fnSearch();
});

// ================ API Functions ================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS003"],
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

const fnSearch = async () => {
  siteInfoList.value = [];

  try {
    const response = await axios.get("/webApi/baim01/site-info-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: siteNo.value,
        siteNm: siteNm.value,
        useYn: useYn.value,
      },
    });

    if (response.status === 200) {
      siteInfoList.value = response.data?.siteInfoList || [];
      cmpnyWeekStdMinutes.value = response.data?.cmpnyWeekStdMinutes ?? null;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSave = async () => {
  const filteredSite = siteInfoList.value.filter((user) => user.chk);

  if (filteredSite.length == 0) {
    proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/baim01/save-site-infos",
      filteredSite
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

// ================ Methods/Functions ================
const fnButtonControll = () => {
  // localButtons.value.search = "N";
  // localButtons.value.create = "N";
  // localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

const fnHeadChk = () => {
  headChk.value = !headChk.value;
  siteInfoList.value.forEach((item) => {
    item.CHK = headChk.value;
  });
};

const fnSiteInfoPopOpen = (siteInfo) => {
  p_siteCd.value = siteInfo.siteCd;

  fnSiteOpenPop(SiteInfoPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: p_siteCd.value,
    cmpnyWeekStdMinutes_p: cmpnyWeekStdMinutes.value,
    onSelect: fnSearch,
    reset: fnCreate,
  });
};

const fnCreate = () => {
  fnSiteOpenPop(SiteInfoPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    cmpnyWeekStdMinutes_p: cmpnyWeekStdMinutes.value,
    onSelect: fnSearch,
  });
};

const fnSiteOpenPop = (popId, param) => {
  openPop(popId, param);
};
</script>

<style scoped>
/* PRAFTA-SUBCON-T2-09: 연동(미러) 사업장 배지 — Subcon_02 status-badge 톤 정합 */
.link-badge {
  display: inline-block;
  margin-left: 0.35rem;
  padding: 0.05rem 0.4rem;
  border-radius: var(--btn-radius, 8px);
  background: var(--color-primary-bg, #dcfce7);
  color: var(--color-primary, #16a34a);
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.4;
}
</style>
