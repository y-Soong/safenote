<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="props.buttons"
      @search="fnSearch"
      @save="fnSave"
      @create="fnCreate"
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
          id="sr_siteNo"
          type="text"
          v-model="sr_siteNo"
          placeholder="사업장코드"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="siteDisabled"
          @click="fnSiteSearchPopOpen('searchForm')"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="sr_siteNm"
          type="text"
          v-model="sr_siteNm"
          placeholder="사업장명"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
      </div>

      <div>
        <label>점검대상명칭</label>
        <input v-model.trim="sr_chkptNm" type="text" />
      </div>

      <div>
        <label>순회회점검구분</label>
        <select v-model="sr_chkLstType" name="combo">
          <option
            v-for="opt in baseCodeArr['COM001'] || []"
            :key="opt.baimValDCd"
            :value="opt.baimValDCd"
          >
            {{ opt.baimValDNm }}
          </option>
        </select>
      </div>

      <div>
        <label>사용여부</label>
        <select v-model.trim="sr_useYn" name="combo">
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
          <span class="subtitle-text">점검대상 리스트</span>
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
                <th class="editableCell" style="width: 10%">사업장</th>
                <th class="editableCell" style="width: 10%">점검구분</th>
                <th class="editableCell" style="width: 15%">점검대상명칭</th>
                <th class="editableCell" style="width: 10%">관리자</th>
                <th style="width: 8%">사용여부</th>
                <th class="editableCell">비고</th>
                <th class="editableCell" style="width: 2%">QR코드</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(chkpt, idx) in chkptList" :key="chkpt.chkptCd">
                <td style="text-align: center">{{ idx + 1 }}</td>
                <td>
                  <input type="checkbox" v-model="chkpt.chk" />
                </td>
                <td>
                  <div class="flex items-center gap-2 w-full">
                    <span class="truncate min-w-0">{{ chkpt.siteNm }}</span>
                    <button
                      v-if="!chkpt.chkptCd"
                      class="ml-auto border rounded"
                      style="
                        background-color: #007bff;
                        border: none;
                        padding: 0.2rem 0.2rem;
                      "
                      @click="fnSiteSearchPopOpen(idx)"
                    >
                      <img class="search_icon" :src="search_icon" alt="검색" />
                    </button>
                  </div>
                </td>
                <td>
                  <BaseSelect
                    v-model="chkpt.chkLstType"
                    :readonly="chkpt.chkptCd ? true : false"
                    name="CHKLST_TYPE"
                  >
                    <option
                      v-for="opt in (baseCodeArr['COM001'] || []).filter(
                        (o) => o.baimValDCd != null
                      )"
                      :key="opt.baimValDCd"
                      :value="opt.baimValDCd"
                    >
                      {{ opt.baimValDNm }}
                    </option>
                  </BaseSelect>
                </td>
                <td>
                  <input id="chkptNm" v-model="chkpt.chkptNm" />
                </td>
                <td>
                  <div class="flex items-center gap-2 w-full">
                    <span class="truncate min-w-0">{{ chkpt.mgmtUserNm }}</span>
                    <button
                      class="ml-auto border rounded"
                      style="
                        background-color: #30796a;
                        border: none;
                        padding: 0.2rem 0.2rem;
                      "
                      @click="fnUserSearchPopOpen(idx)"
                    >
                      <img class="search_icon" :src="search_icon" alt="검색" />
                    </button>
                  </div>
                </td>
                <td>
                  <BaseSelect
                    v-model="chkpt.useYn"
                    :readonly="true"
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
                    id="chkptDesc"
                    style="width: 100%"
                    v-model="chkpt.chkptDesc"
                  />
                </td>
                <td>
                  <div class="flex items-center gap-2 w-full">
                    <button
                      v-if="chkpt.chkptCd"
                      class="border rounded"
                      style="
                        background-color: #30796a;
                        border: none;
                        padding: 0.2rem 0.5rem;
                        color: #fff; /* ← 글자색 흰색 */
                      "
                      @click="fnQrCodePopOpen(chkpt)"
                    >
                      QRCODE
                    </button>
                  </div>
                </td>
              </tr>
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
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import BaseSelect from "@/components/common/BaseSelect.vue";
import UserSearchPop from "@/components/popup/UserSearchPop.vue";
import QrCodePop from "@/components/popup/QrCodePop.vue";

// ================ Options ================
defineOptions({ name: "ChkLst_01" });

// ================ Props & Emits ================
const props = defineProps({
  title: String,
  buttons: Object,
});

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ================ Refs (Variables) ================
const chkptList = ref([]);
const systCodeArr = ref([]);
const baseCodeArr = ref({});
const SiteSearchPopOpen = ref(false);

// 조회조건 변수
const sr_chkptNm = ref("");
const sr_chkLstType = ref();
const sr_useYn = ref("Y");
const sr_siteCd = ref("");
const sr_siteNo = ref("");
const sr_siteNm = ref("");

// 화면 제어 변수
const headChk = ref(false);
const siteDisabled = ref(false);

// ================ Watchers ================
useFieldWatcher(
  chkptList,
  (item) => {
    item.chk = true;
  },
  ["chk"]
);

// ================ Life Cycle Functions ================
onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
  await fnGetBaseinfoList();
  await fnSearch();
});

// ================ API Functions ================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-list", {
      params: {
        systCodeList: ["SYS002", "SYS003"],
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
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/base-info-list", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        baseCodeList: ["COM001"],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.baseInfoList || [];

      const grouped = {};
      resData.forEach((item) => {
        const key = item.baimValCd;
        if (!grouped[key]) {
          grouped[key] = [];
        }
        grouped[key].push(item);
      });

      baseCodeArr.value = grouped;
      sr_chkLstType.value = baseCodeArr.value.COM001[0].baimValDCd;
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
  try {
    const response = await axios.post("/webApi/chkLst01/getChkptList", {
      sr_siteCd: sr_siteCd.value,
      sr_chkptNm: sr_chkptNm.value,
      sr_chkLstType: sr_chkLstType.value,
      sr_useYn: sr_useYn.value,
    });

    if (response.status === 200) {
      chkptList.value = response.data;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnSave = async () => {
  const filteredData = chkptList.value.filter((chkpt) => chkpt.chk);

  if (filteredData.length == 0) {
    proxy.$alert("저장할 데이터가 없습니다.");
    return;
  }

  if (!fnDataValidationChk(filteredData)) {
    return;
  }

  const ok = await proxy.$confirm("저장하시겠습니까 ?");
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/chkLst01/updateChkptList",
      filteredData
    );

    if (response.status === 200) {
      proxy.$alert("처리되었습니다.");
      fnSearch();
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "저장 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnDelete = async () => {
  const filteredData = chkptList.value.filter(
    (chkpt) => chkpt.chk && chkpt.chkptCd
  );

  if (filteredData.length == 0) {
    proxy.$alert("저장할 데이터가 없습니다.");
    return;
  }

  const ok = await proxy.$confirm("삭제하시겠습니까 ?");
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/chkLst01/deleteChkptList",
      filteredData
    );

    if (response.status === 200) {
      proxy.$alert("처리되었습니다.");
      fnSearch();
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "삭제 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.post("/comApi/baseinfo/getSiteInfoList", {
      cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
      siteNo: sr_siteNo.value,
      siteNm: sr_siteNm.value,
    });

    if (response.status === 200) {
      fnCallback(response);
    }
  } catch (err) {
    alert(err.response.data.message);
  }
};

// ================ Methods/Functions ================
const fnButtonControll = () => {
  // localButtons.value.search = "N";
  // localButtons.value.create = "N";
  // localButtons.value.save = "N";
  // localButtons.value.delete = "N";
  // localButtons.value.excel = "N";
};

const focusKill = (e) => {
  if (e.target.id == "sr_siteNo") {
    if (proxy.$util.isEmpty(sr_siteNo.value)) {
      sr_siteCd.value = "";
      sr_siteNm.value = "";
    } else {
      sr_siteNm.value = "";
      siteFocusKill();
    }
  } else if (e.target.id == "sr_siteNm") {
    if (proxy.$util.isEmpty(sr_siteNm.value)) {
      sr_siteCd.value = "";
      sr_siteNo.value = "";
    } else {
      sr_siteNo.value = "";
      siteFocusKill();
    }
  }
};

const fnDataValidationChk = (filteredData) => {
  let alertMsg = "";
  let retVal = true;

  for (var i = 0; i < filteredData.length; i++) {
    if (proxy.$util.isEmpty(filteredData[i].siteCd)) {
      alertMsg = "사업장은 필수 입력 값 입니다.";

      fnAlertMsg(alertMsg);
      retVal = false;
    } else if (proxy.$util.isEmpty(filteredData[i].chkLstType)) {
      alertMsg = "점검구분은 필수 입력 값 입니다.";

      fnAlertMsg(alertMsg);
      retVal = false;
    } else if (proxy.$util.isEmpty(filteredData[i].chkptNm)) {
      alertMsg = "점검대상명칭은 필수 입력 값 입니다.";

      fnAlertMsg(alertMsg);
      retVal = false;
    } else if (proxy.$util.isEmpty(filteredData[i].mgmtUserId)) {
      alertMsg = "관리자는 필수 입력 값 입니다.";

      fnAlertMsg(alertMsg);
      retVal = false;
    }

    if (!retVal) {
      return retVal;
    }
  }

  return retVal;
};

const fnCallback = (res) => {
  if (proxy.$util.isNotEmpty(res)) {
    const apiId = res.config.url.split("/").pop();
    if (apiId == "getSiteInfoList") {
      if (res.data.length == 1) {
        sr_siteCd.value = res.data[0].SITE_CD;
        sr_siteNo.value = res.data[0].SITE_NO;
        sr_siteNm.value = res.data[0].SITE_NM;
      } else if (res.data.length > 1) {
        //        handleResetSiteSearchPop();
        fnSiteSearchPopOpen("searchForm");
        SiteSearchPopOpen.value = true;
      } else {
        sr_siteCd.value = "";
        sr_siteNo.value = "";
        sr_siteNm.value = "";
      }
    }
  }
};

const fnCreate = () => {
  chkptList.value.push({
    chk: true,
    useYn: "Y",
    siteCd: sr_siteCd.value,
    siteNm: sr_siteNm.value,
  });
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  sr_siteCd.value = siteCdVal;
  sr_siteNo.value = siteNoVal;
  sr_siteNm.value = siteNmVal;
};

const siteFocusKill = async () => {
  await fnSrchSiteInfo();
};

const fnHeadChk = () => {
  headChk.value = !headChk.value;
  chkptList.value.forEach((item) => {
    item.chk = headChk.value;
  });
};

const fnSiteSearchPopOpen = (callPoint) => {
  if (callPoint == "searchForm") {
    openPop(SiteSearchPop, {
      cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
      siteNo_p: sr_siteNo.value,
      siteNm_p: sr_siteNm.value,
      onSelect: onSiteSelected,
    });
  } else {
    openPop(SiteSearchPop, {
      cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
      siteNo_p: sr_siteNo.value,
      siteNm_p: sr_siteNm.value,
      onSelect: (siteCdVal, siteNoVal, siteNmVal) => {
        chkptList.value[callPoint].siteCd = siteCdVal;
        chkptList.value[callPoint].siteNm = siteNmVal;
      },
    });
  }
};

const fnUserSearchPopOpen = (callPoint) => {
  openPop(UserSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    onSelect: (userIdVal, userNmVal) => {
      chkptList.value[callPoint].mgmtUserId = userIdVal;
      chkptList.value[callPoint].mgmtUserNm = userNmVal;
    },
  });
};

const fnQrCodePopOpen = (chkpt) => {
  openPop(QrCodePop, {
    qrValue: JSON.stringify({
      siteCd: chkpt.siteCd,
      chkptCd: chkpt.chkptCd,
      qrTitle: chkpt.chkptNm,
    }),
  });
};

const fnAlertMsg = async (message, afterConfirmCallback) => {
  await proxy.$alert(message);
  if (afterConfirmCallback) {
    afterConfirmCallback();
  }
};
</script>

<style scoped></style>
