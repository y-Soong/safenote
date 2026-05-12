<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @create="fnCreate"
    />

    <div class="viewSearch">
      <div>
        <label>사업장</label>
        <input
          id="siteNo"
          type="text"
          v-model="siteNo"
          placeholder="사업장코드"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="siteDisabled"
          @click="fnSiteSearchPopOpen"
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
        <label>교대타입코드</label>
        <input v-model.trim="shiftNo" type="text" placeholder="검색" />
      </div>
      <div>
        <label>교대일수</label>
        <select v-model="shiftCycleDays" name="combo">
          <option value="">전체</option>
          <option v-for="n in 7" :key="n" :value="String(n)">{{ n }}일</option>
        </select>
      </div>
      <div>
        <label>사용여부</label>
        <select v-model="useYn" name="combo">
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

    <div class="attd01-2-info-banner">
      <span class="info-icon">ℹ</span>
      <span
        >교대근무는 지정한 교대일수 동안 스케줄 타입을 순환 적용합니다.</span
      >
    </div>

    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">교대근무 타입 리스트</span>
        </div>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="
            --box-h: calc(70vh - 3.5rem - 3.75rem);
            --box-sticky-top: 1px;
            --box-ox: auto;
          "
        >
          <table
            class="data-grid w-full table-fixed text-sm text-left rtl:text-right"
          >
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 3%">
                  <input
                    id="headChk"
                    v-model="headChk"
                    type="checkbox"
                    @change="fnHeadChk"
                  />
                </th>
                <th class="event_cell" style="text-align: center; width: 4%">
                  No
                </th>
                <ThSortable
                  label="타입코드"
                  col-key="shiftNo"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.shiftNo"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="교대일수"
                  col-key="shiftCycleDays"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.shiftCycleDays"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="패턴요약"
                  col-key="schNmList"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.schNmList"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="등록사용자수"
                  col-key="regUserCnt"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.regUserCnt"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th style="width: 12%">사용여부</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!shiftList || shiftList.length === 0">
                <tr>
                  <td colspan="8" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(shift, idx) in sortedData"
                  :key="shift.shiftNo || idx"
                  class="row-clickable"
                  @dblclick="fnShiftTypeDetail(shift)"
                >
                  <td style="text-align: center" @click.stop>
                    <input
                      v-model="shift.chk"
                      type="checkbox"
                      @change="fnRowChk"
                    />
                  </td>
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <span class="type-code-link">
                      {{ shift.shiftNo }}
                    </span>
                  </td>
                  <td>{{ shift.shiftCycleDays }}일</td>
                  <td class="pattern-cell" :title="shift.patternSummary">
                    {{ shift.schNmList }}
                  </td>
                  <td>{{ shift.regUserCnt }}명</td>
                  <td>
                    <select v-model="shift.useYn" class="use-yn-select">
                      <option
                        v-for="opt in (systCodeArr['SYS003'] || []).filter(
                          (o) => o.systValDCd != null
                        )"
                        :key="opt.systValDCd"
                        :value="opt.systValDCd"
                      >
                        {{ opt.systValDNm }}
                      </option>
                    </select>
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
import {
  ref,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { getMessage, MSG } from "@/messages";
import { useModal } from "@/utils/useModal";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import ShiftTypeCreatePop from "@/views/attd/popup/ShiftTypeCreatePop.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

defineOptions({ name: "Attd_01_2" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const localButtons = ref({ ...props.buttons });
const shiftList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(shiftList);
const { colWidths, onResize } = useColumnResize({
  shiftNo: 110,
  shiftCycleDays: 90,
  schNmList: 200,
  regUserCnt: 110,
});
const systCodeArr = ref([]);
const headChk = ref(false);

const shiftNo = ref("");
const shiftCycleDays = ref("");
const useYn = ref("");
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteDisabled = ref(false);

const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
};

onMounted(async () => {
  fnInit();
  fnButtonControll();
  await fnGetSystinfoList();
});

const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: { systCodeList: ["SYS003"] },
    });
    if (response.status === 200) {
      const resData = response.data?.systInfoList || [];
      const grouped = {};
      resData.forEach((item) => {
        const key = item.systValCd;
        if (!grouped[key]) grouped[key] = [];
        grouped[key].push(item);
      });
      systCodeArr.value = grouped;
      useYn.value = systCodeArr.value.SYS003?.[0]?.systValDCd;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
    return;
  }
  shiftList.value = [];
  try {
    const response = await axios.get("/webApi/attd01/shift-sch-info-lists", {
      params: {
        siteCd: siteCd.value,
        shiftNo: shiftNo.value,
        shiftCycleDays: shiftCycleDays.value,
        useYn: useYn.value,
      },
    });
    if (response.status === 200) {
      shiftList.value = response.data.shiftSchInfoResultList;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

const fnButtonControll = () => {
  localButtons.value.create = "Y";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

const focusKill = (e) => {
  if (e.target.id === "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
    } else {
      siteNm.value = "";
      siteFocusKill();
    }
  } else if (e.target.id === "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
    } else {
      siteNo.value = "";
      siteFocusKill();
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
    proxy.$alert(
      err?.response?.data?.message ||
        err?.message ||
        "사업장 조회 중 오류가 발생했습니다."
    );
  }
};

const fnCallback = (res) => {
  if (proxy.$util.isNotEmpty(res)) {
    const apiId = res.config.url.split("/").pop();
    if (apiId === "site-lists") {
      const siteList = res.data?.siteInfoResultList ?? [];
      if (siteList.length === 1) {
        siteCd.value = siteList[0].siteCd;
        siteNo.value = siteList[0].siteNo;
        siteNm.value = siteList[0].siteNm;
      } else if (siteList.length > 1) {
        fnSiteSearchPopOpen();
      } else {
        siteCd.value = "";
        siteNo.value = "";
        siteNm.value = "";
      }
    }
  }
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
};

const siteFocusKill = async () => {
  await fnSrchSiteInfo();
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

const fnHeadChk = () => {
  shiftList.value.forEach((row) => (row.chk = headChk.value));
};

const fnRowChk = () => {
  headChk.value =
    shiftList.value.length > 0 && shiftList.value.every((row) => row.chk);
};

const fnCreate = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
    return;
  }
  openPop(ShiftTypeCreatePop, {
    siteCd_p: siteCd.value,
    onSearch: fnSearch,
  });
};

const fnShiftTypeDetail = (shift) => {
  openPop(ShiftTypeCreatePop, {
    siteCd_p: shift.siteCd,
    shift_p: shift,
    onSearch: fnSearch,
  });
};
</script>

<style scoped>
.attd01-2-info-banner {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  background: #d1fae5;
  border: 1px solid #a7f3d0;
  border-radius: 8px;
  color: #065f46;
  font-size: 0.875rem;
}
.attd01-2-info-banner .info-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.25rem;
  height: 1.25rem;
  background: #10b981;
  color: #fff;
  border-radius: 50%;
  font-size: 0.75rem;
  font-weight: 600;
  flex-shrink: 0;
}
.type-code-link {
  color: #16a34a;
  font-weight: 500;
  cursor: pointer;
}
.type-code-link:hover {
  text-decoration: underline;
}
.pattern-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.use-yn-select {
  padding: 0.25rem 0.5rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 6px;
  font-size: 0.8125rem;
  min-width: 6rem;
}
.row-clickable {
  cursor: default;
}
.btn-history-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.35rem;
  background: transparent;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 6px;
  cursor: pointer;
  color: #6b7280;
  transition:
    color 0.2s,
    background 0.2s,
    border-color 0.2s;
}
.btn-history-icon:hover {
  color: #30796a;
  background: rgba(48, 121, 106, 0.08);
  border-color: #30796a;
}
.btn-history-icon svg {
  width: 18px;
  height: 18px;
}
</style>
