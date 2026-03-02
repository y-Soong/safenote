<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @save="fnSave"
      @create="fnCreate"
      @delete="fnDelete"
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
          @click="fnSiteSearchPopOpen('searchForm')"
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
        <label>근무코드</label>
        <input v-model.trim="schNo" type="text" />
      </div>

      <div>
        <label>근무구간</label>
        <select v-model.trim="schType" name="combo">
          <option
            v-for="opt in systCodeArr['SYS019'] || []"
            :key="opt.systValDCd"
            :value="opt.systValDCd"
          >
            {{ opt.systValDNm }}
          </option>
        </select>
      </div>

      <div>
        <label>사용유무</label>
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

    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle-row">
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">점검대상 리스트</span>
          </div>
          <div class="subtitle" v-if="siteNm">
            <span class="subtitle-text">[{{ siteNm }}]</span>
          </div>
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
                <th class="editableCell" style="width: 15%">근무코드</th>
                <th class="editableCell" style="width: 10%">근무구간</th>
                <th class="editableCell" style="width: 13%">1 근무시간</th>
                <th class="editableCell" style="width: 10%">1 휴게시간</th>
                <th class="editableCell" style="width: 13%">2 근무시간</th>
                <th class="editableCell" style="width: 10%">2 휴게시간</th>
                <th class="editableCell">등록 사용자 수</th>
                <th style="width: 8%">사용여부</th>
                <th style="width: 8%">기본근무여부</th>
                <th class="editableCell" style="width: 8%">변경이력</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!schList || schList.length === 0">
                <tr>
                  <td colspan="11" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(sch, idx) in schList"
                  :key="sch.schCd || idx"
                  class="row-clickable"
                  @dblclick="fnSchInfoPopOpen(sch)"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ sch.schNo }}</td>
                  <td>{{ sch.schTypeNm }}</td>
                  <td>{{ sch.fstSchTime }}</td>
                  <td>{{ sch.fstSchBrkMin }}</td>
                  <td>{{ sch.secSchTime }}</td>
                  <td>{{ sch.secSchBrkMin }}</td>
                  <td>{{ sch.regUserCnt }}</td>
                  <td>{{ sch.useYnNm }}</td>
                  <td>{{ sch.baseYnNm }}</td>
                  <td style="text-align: center" @click.stop>
                    <button
                      type="button"
                      class="btn-history-icon"
                      title="변경이력"
                      @click="fnChangeHistOpen(sch)"
                    >
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="1.5"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <path
                          d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"
                        />
                        <polyline points="14,2 14,8 20,8" />
                        <path d="M8 12h8" />
                        <path d="M8 16h8" />
                        <path d="M8 10h4" />
                      </svg>
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
import {
  ref,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SchInfoPop from "./popup/SchInfoPop.vue";
import SchInfoHistPop from "./popup/SchInfoHistPop.vue";

defineOptions({ name: "Attd_01_1" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const localButtons = ref({ ...props.buttons });
const schList = ref([]);
const systCodeArr = ref([]);

const schNo = ref("");
const useYn = ref();
const schType = ref("Y");
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteDisabled = ref(false);

onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
});

const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-list", {
      params: {
        systCodeList: ["SYS003", "SYS019"],
      },
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
      useYn.value = systCodeArr.value.SYS003[1].systValDCd;
      schType.value = systCodeArr.value.SYS019[0].systValDCd;
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
    proxy.$alert("사업장을 선택해주세요.");
    return;
  }
  schList.value = [];
  try {
    const response = await axios.get("/webApi/attd01/sch-info-lists", {
      params: {
        siteCd: siteCd.value,
        schNo: schNo.value,
        schType: schType.value,
        useYn: useYn.value,
      },
    });
    if (response.status === 200) {
      schList.value = response.data.schInfoList;
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
  const filteredData = schList.value.filter((chkpt) => chkpt.chk);
  if (filteredData.length == 0) {
    proxy.$alert("저장할 데이터가 없습니다.");
    return;
  }
  if (!fnDataValidationChk(filteredData)) return;
  const ok = await proxy.$confirm("저장하시겠습니까 ?");
  if (!ok) return;
  try {
    await axios.post("/webApi/chkLst01/updateschList", filteredData);
    proxy.$alert("처리되었습니다.");
    fnSearch();
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "저장 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};

const fnDelete = async () => {
  const filteredData = schList.value.filter(
    (chkpt) => chkpt.chk && chkpt.chkptCd
  );
  if (filteredData.length == 0) {
    proxy.$alert("저장할 데이터가 없습니다.");
    return;
  }
  const ok = await proxy.$confirm("삭제하시겠습니까 ?");
  if (!ok) return;
  try {
    await axios.post("/webApi/chkLst01/deleteschList", filteredData);
    proxy.$alert("처리되었습니다.");
    fnSearch();
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
      siteNo: siteNo.value,
      siteNm: siteNm.value,
    });
    if (response.status === 200) fnCallback(response);
  } catch (err) {
    alert(err.response?.data?.message);
  }
};

const fnButtonControll = () => {
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
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
    if (!retVal) return retVal;
  }
  return retVal;
};

const fnCallback = (res) => {
  if (proxy.$util.isNotEmpty(res)) {
    const apiId = res.config.url.split("/").pop();
    if (apiId == "getSiteInfoList") {
      if (res.data.length == 1) {
        siteCd.value = res.data[0].SITE_CD;
        siteNo.value = res.data[0].SITE_NO;
        siteNm.value = res.data[0].SITE_NM;
      } else if (res.data.length > 1) {
        fnSiteSearchPopOpen("searchForm");
      } else {
        siteCd.value = "";
        siteNo.value = "";
        siteNm.value = "";
      }
    }
  }
};

const fnCreate = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert("사업장을 선택해주세요.");
    return;
  }
  fnSchInfoPopOpen(null);
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
};

const siteFocusKill = async () => {
  await fnSrchSiteInfo();
};

const fnSiteSearchPopOpen = (callPoint) => {
  if (callPoint == "searchForm") {
    openPop(SiteSearchPop, {
      cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
      siteNo_p: siteNo.value,
      siteNm_p: siteNm.value,
      onSelect: onSiteSelected,
    });
  } else {
    openPop(SiteSearchPop, {
      cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
      siteNo_p: siteNo.value,
      siteNm_p: siteNm.value,
      onSelect: (siteCdVal, siteNoVal, siteNmVal) => {
        schList.value[callPoint].siteCd = siteCdVal;
        schList.value[callPoint].siteNm = siteNmVal;
      },
    });
  }
};

const fnAlertMsg = async (message, afterConfirmCallback) => {
  await proxy.$alert(message);
  if (afterConfirmCallback) afterConfirmCallback();
};

const fnChangeHistOpen = (sch) => {
  openPop(SchInfoHistPop, {
    schData_p: sch,
  });
};

const fnSchInfoPopOpen = (sch) => {
  openPop(SchInfoPop, {
    schData_p: sch,
    siteCd_p: siteCd.value,
    siteNm_p: siteNm.value,
    systCodeArr_p: systCodeArr.value,
    onSave: fnSearch,
  });
};
</script>

<style scoped>
.row-clickable {
  cursor: pointer;
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
  transition: color 0.2s, background 0.2s, border-color 0.2s;
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
