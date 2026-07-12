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
        <input v-model.trim="chkptNm" type="text" />
      </div>

      <div>
        <label>순회회점검구분</label>
        <select v-model="chkLstType" name="combo">
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
                      class="ml-auto border rounded node-assign-btn"
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
                      class="ml-auto border rounded node-assign-btn"
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
                <td style="text-align: center">
                  <button
                    v-if="chkpt.chkptCd"
                    class="btn btn-custom"
                    @click="fnQrCodePopOpen(chkpt)"
                  >
                    QRCODE
                  </button>
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
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
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
const chkptNm = ref("");
const chkLstType = ref();
const useYn = ref("Y");
const siteCd = ref("");
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
const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  sr_siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  sr_siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
};

onMounted(async () => {
  fnInit();
  fnButtonControll();
  await fnGetSystinfoList();
  await fnGetBaseinfoList();
  await fnSearch();
});

// ================ API Functions ================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
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
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/base-info-lists", {
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
      chkLstType.value = baseCodeArr.value.COM001[0].baimValDCd;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  chkptList.value = [];

  try {
    const response = await axios.get("/webApi/chkLst01/chkpt-lists", {
      params: {
        siteCd: siteCd.value,
        chkptNm: chkptNm.value,
        chkLstType: chkLstType.value,
        useYn: useYn.value,
      },
    });

    if (response.status === 200) {
      chkptList.value = response.data?.chkptResultList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSave = async () => {
  const filteredData = chkptList.value.filter((chkpt) => chkpt.chk);

  if (filteredData.length == 0) {
    proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }

  if (!fnDataValidationChk(filteredData)) {
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/chkLst01/update-chkpt-lists",
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
  const filteredData = chkptList.value.filter(
    (chkpt) => chkpt.chk && chkpt.chkptCd
  );

  if (filteredData.length == 0) {
    proxy.$alert(getMessage(MSG.DELETE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.DELETE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/chkLst01/deleteChkptList",
      filteredData
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "삭제 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: sr_siteNo.value,
        siteNm: sr_siteNm.value,
      },
    });

    if (response.status === 200) {
      fnCallback(response);
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
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
      siteCd.value = "";
      sr_siteNm.value = "";
    } else {
      sr_siteNm.value = "";
      siteFocusKill();
    }
  } else if (e.target.id == "sr_siteNm") {
    if (proxy.$util.isEmpty(sr_siteNm.value)) {
      siteCd.value = "";
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
    } else if (proxy.$util.isEmpty(filteredData[i].mgmtUserCd)) {
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
    if (apiId == "site-lists") {
      const siteList = res.data?.siteInfoResultList ?? [];
      if (siteList.length === 1) {
        siteCd.value = siteList[0].siteCd;
        sr_siteNo.value = siteList[0].siteNo;
        sr_siteNm.value = siteList[0].siteNm;
      } else if (siteList.length > 1) {
        //        handleResetSiteSearchPop();
        fnSiteSearchPopOpen("searchForm");
        SiteSearchPopOpen.value = true;
      } else {
        siteCd.value = "";
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
    siteCd: siteCd.value,
    siteNm: sr_siteNm.value,
  });
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
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
      siteNo_p: "",
      siteNm_p: "",
      onSelect: onSiteSelected,
    });
  } else {
    openPop(SiteSearchPop, {
      cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
      siteNo_p: "",
      siteNm_p: "",
      onSelect: (siteCdVal, siteNoVal, siteNmVal) => {
        chkptList.value[callPoint].siteCd = siteCdVal;
        chkptList.value[callPoint].siteNm = siteNmVal;
        // 팝업 콜백으로 인한 변경은 watcher가 미감지할 수 있어 직접 체크 보강
        chkptList.value[callPoint].chk = true;
      },
    });
  }
};

const fnUserSearchPopOpen = (callPoint) => {
  openPop(UserSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    // 관리자 지정은 정규직(REGULAR)만 조회 (공용 팝업이므로 prop 전달 시에만 필터 적용)
    employmentType_p: "REGULAR",
    onSelect: (userIdVal, userNmVal) => {
      chkptList.value[callPoint].mgmtUserCd = userIdVal;
      chkptList.value[callPoint].mgmtUserNm = userNmVal;
      // 팝업 콜백으로 인한 변경은 watcher가 미감지할 수 있어 직접 체크 보강
      chkptList.value[callPoint].chk = true;
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

<style scoped>
/* table.css 의 .data-grid button 전역 border 가 .btn-custom 색을 덮으므로
   테이블 내부에서도 동일한 border 가 유지되도록 specificity 보강 */
.data-grid .btn.btn-custom {
  border-color: var(--color-primary, #16a34a);
}

/* 테이블 내 검색(아이콘) 버튼 — Baim_05 기준 통일(CSS 변수 색·라운드·disabled 처리) */
.node-assign-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.2rem;
  border: none;
  border-radius: 4px;
  background-color: var(--color-primary, #16a34a);
  cursor: pointer;
}
.node-assign-btn:disabled {
  background-color: var(--color-border, #d1d5db);
  cursor: not-allowed;
}
</style>
