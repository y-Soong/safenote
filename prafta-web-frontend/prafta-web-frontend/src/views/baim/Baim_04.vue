<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @save="fnSave"
      @delete="fnDelete"
    />
    <!-- 
      @create="fnCreate"
      @save="fnSave"
      @delete="fnDelete"
      @excel="fnExcel" -->

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
        <label>링크 활성화 여부</label>
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
          <span class="subtitle-text">계정발급 링크 리스트</span>
        </div>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
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
                  label="사업장"
                  col-key="siteNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.siteNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th class="event_cell" style="width: 15%">링크 활성화 여부</th>
                <th style="width: 15%">활성화 계정 수</th>
                <ThSortable
                  label="일일계정 회원가입 코드"
                  col-key="joinCd"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.joinCd"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th class="editableCell" style="width: 15%">QR코드</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!LinkPolicyList || LinkPolicyList.length === 0">
                <tr>
                  <td colspan="7" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(linkPolicy, idx) in sortedData"
                  :key="linkPolicy.id"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input type="checkbox" v-model="linkPolicy.chk" />
                  </td>
                  <td>
                    {{ linkPolicy.siteNm }}
                  </td>
                  <td
                    :style="{
                      backgroundColor:
                        linkPolicy.useYn === 'N' ? '#e9f4f0' : '#ffe6e6',
                    }"
                  >
                    <BaseSelect
                      v-model="linkPolicy.useYn"
                      :readonly="false"
                      name="useYn"
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
                    <input
                      type="number"
                      v-model.number="linkPolicy.dayLimitCnt"
                      min="0"
                      placeholder="0"
                    />
                  </td>
                  <td>
                    {{ linkPolicy.joinCd }}
                  </td>
                  <td style="text-align: center">
                    <button
                      class="btn btn-custom"
                      @click="fnQrCodePopOpen(linkPolicy)"
                    >
                      QRCODE
                    </button>
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
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import QrCodePop from "@/components/popup/QrCodePop.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

// ================ Options ================
defineOptions({ name: "Baim_04" });

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
const LinkPolicyList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(LinkPolicyList);
const { colWidths, onResize } = useColumnResize({ siteNm: 150, joinCd: 200 });
const systCodeArr = ref({});
const SiteSearchPopOpen = ref(false);

// 조회조건 변수
const useYn = ref();
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");

// 화면 제어 변수
const headChk = ref(false);
const siteDisabled = ref(false);

// ================ Watchers ================
useFieldWatcher(
  LinkPolicyList,
  (item) => {
    item.chk = true;
  },
  ["chk"]
);

// ================ Life Cycle Functions ================
const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
};

onMounted(async () => {
  fnInit();
  await fnButtonControll();
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
      useYn.value = systCodeArr.value.SYS003[0].systValDCd;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  LinkPolicyList.value = [];

  try {
    const response = await axios.get(
      "/webApi/baim04/daily-user-link-policies",
      {
        params: {
          siteCd: siteCd.value,
          useYn: useYn.value,
        },
      }
    );

    if (response.status === 200) {
      LinkPolicyList.value = response.data?.dailyUserLinkPolicyList || [];
      // dayLimitCnt 기본값 설정 (null, 빈 값, 음수는 0으로 설정)
      LinkPolicyList.value.forEach((item) => {
        if (
          item.dayLimitCnt == null ||
          item.dayLimitCnt === "" ||
          item.dayLimitCnt < 0
        ) {
          item.dayLimitCnt = 0;
        }
      });
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSave = async () => {
  const filteredData = LinkPolicyList.value.filter(
    (linkPolicy) => linkPolicy.chk
  );

  if (filteredData.length == 0) {
    proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }

  // dayLimitCnt가 비어있거나 음수면 0으로 치환
  filteredData.forEach((item) => {
    if (
      item.dayLimitCnt == null ||
      item.dayLimitCnt === "" ||
      item.dayLimitCnt < 0
    ) {
      item.dayLimitCnt = 0;
    }
  });

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/baim04/save-daily-user-link-policies",
      filteredData
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

const fnDelete = async () => {
  const filteredData = LinkPolicyList.value.filter(
    (linkPolicy) => linkPolicy.chk
  );

  if (filteredData.length == 0) {
    proxy.$alert(getMessage(MSG.DELETE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.DELETE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/baim04/delete-daily-user-link-policies",
      filteredData
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

const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: siteNo.value,
        siteNm: siteNm.value,
        useYn: useYn.value,
      },
    });

    if (response.status === 200) {
      fnCallback(response);
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

// ================ Methods/Functions ================
const fnButtonControll = async () => {
  // localButtons.value.search = "N";
  localButtons.value.create = "N";
  // localButtons.value.save = "N";
  // localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

const focusKill = (e) => {
  if (e.target.id == "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
    } else {
      siteNm.value = "";
      siteFocusKill();
    }
  } else if (e.target.id == "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
    } else {
      siteNo.value = "";
      siteFocusKill();
    }
  }
};

const fnCallback = (res) => {
  if (proxy.$util.isNotEmpty(res)) {
    const apiId = res.config.url.split("/").pop();

    if (apiId == "site-lists") {
      const siteList = res.data?.siteInfoResultList ?? [];
      if (siteList.length === 1) {
        siteCd.value = siteList[0].siteCd;
        siteNo.value = siteList[0].siteNo;
        siteNm.value = siteList[0].siteNm;
      } else if (siteList.length > 1) {
        //        handleResetSiteSearchPop();
        fnSiteSearchPopOpen();
        SiteSearchPopOpen.value = true;
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

const fnHeadChk = () => {
  headChk.value = !headChk.value;
  LinkPolicyList.value.forEach((item) => {
    item.chk = headChk.value;
  });
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

const fnQrCodePopOpen = (linkPolicy) => {
  openPop(QrCodePop, {
    qrValue: JSON.stringify({
      cmpnyCd: linkPolicy.cmpnyCd,
      siteCd: linkPolicy.siteCd,
      qrTitle: linkPolicy.siteNm + " - 일일계정 발급 QR코드",
    }),
  });
};
</script>

<style scoped>
/* table.css 의 .data-grid button 전역 border 가 .btn-custom 색을 덮으므로
   테이블 내부에서도 동일한 border 가 유지되도록 specificity 보강 */
.data-grid .btn.btn-custom {
  border-color: var(--color-primary, #16a34a);
}
</style>
