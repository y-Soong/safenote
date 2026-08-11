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
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">점검대상 리스트</span>
          <!-- PRAFTA-SUBCON-T2-09: 미러(연동) 사업장 안내 — 편집/신규는 차단(서버 T2-04 가 최종 강제),
               목록·이력 조회는 정상. -->
          <span v-if="isMirrorSite" class="mirror-guide"
            >연동 사업장의 근무타입은 제공 회사에서 관리합니다.</span
          >
        </div>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="
            --box-h: calc(70vh - 3.5rem);
            --box-sticky-top: 1px;
            --box-ox: auto;
          "
        >
          <table
            class="data-grid w-full table-fixed text-sm text-left rtl:text-right"
          >
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 2%">
                  No
                </th>
                <ThSortable
                  label="근무코드"
                  col-key="schNo"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.schNo"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="근무구간"
                  col-key="schTypeNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.schTypeNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="1 근무시간"
                  col-key="fstSchTime"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.fstSchTime"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="1 휴게시간"
                  col-key="fstSchBrkMin"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.fstSchBrkMin"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="2 근무시간"
                  col-key="secSchTime"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.secSchTime"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="2 휴게시간"
                  col-key="secSchBrkMin"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.secSchBrkMin"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="등록 사용자 수"
                  col-key="regUserCnt"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.regUserCnt"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="사용여부"
                  col-key="useYnNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.useYnNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th class="editableCell" style="width: 8%">변경이력</th>
                <th class="editableCell" style="width: 8%">배정현황</th>
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
                  v-for="(sch, idx) in sortedData"
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
                  <td style="text-align: center" @click.stop>
                    <button
                      type="button"
                      class="btn-history-icon"
                      title="배정현황"
                      @click="fnAssignedUsersPopOpen(sch)"
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
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                        <circle cx="9" cy="7" r="4" />
                        <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                        <path d="M16 3.13a4 4 0 0 1 0 7.75" />
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
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SchInfoPop from "./popup/SchInfoPop.vue";
import SchInfoHistPop from "./popup/SchInfoHistPop.vue";
import SchAssignedUsersPop from "./popup/SchAssignedUsersPop.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

defineOptions({ name: "Attd_01_1" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const localButtons = ref({ ...props.buttons });
const schList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(schList);
const { colWidths, onResize } = useColumnResize({
  schNo: 140,
  schTypeNm: 110,
  fstSchTime: 130,
  fstSchBrkMin: 110,
  secSchTime: 130,
  secSchBrkMin: 110,
  regUserCnt: 110,
  useYnNm: 80,
});
const systCodeArr = ref([]);

const schNo = ref("");
const useYn = ref();
const schType = ref("Y");
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteDisabled = ref(false);

// PRAFTA-SUBCON-T2-09: 선택 사업장의 미러(연동) 여부 — 근무타입 편집/신규 차단 판정.
const isMirrorSite = ref(false);

const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
};

onMounted(async () => {
  fnInit();
  fnButtonControll();
  await fnGetSystinfoList();
  // 세션 기본 사업장이 있으면 미러 여부 판정(응답 linkSrcCmpnyCd — T2-04 확정 필드).
  await fnResolveMirrorFlag(siteCd.value);
});

// PRAFTA-SUBCON-T2-09: 사업장 미러 여부 조회 — 사업장 목록 응답의 linkSrcCmpnyCd 로 판정.
//   판정 실패(조회 오류)는 편집 차단하지 않는다(서버 T2-04 가드가 최종 강제).
const fnResolveMirrorFlag = async (siteCdVal) => {
  isMirrorSite.value = false;
  if (proxy.$util.isEmpty(siteCdVal)) return;

  try {
    const response = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
      },
    });
    if (response.status === 200) {
      const found = (response.data?.siteInfoResultList ?? []).find(
        (s) => s.siteCd === siteCdVal
      );
      isMirrorSite.value = !!found?.linkSrcCmpnyCd;
    }
  } catch (err) {
    // 판정 실패는 안내 배지만 미표시 — 우회 시도는 서버가 403 거부.
    console.warn("미러 여부 판정 실패:", err);
  }
};

const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
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
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// PRAFTA-FIXEDOT-2(표기): 근무시간 셀에 고정연장(전방·후방)을 구분 표기(라벨 "고정연장").
//   표시 전용 필드(fstSchTime/secSchTime)만 데코레이션 — 원시 시각(fstSchStrTime 등)은 불변이라
//   수정 팝업/저장 흐름에 영향 없다. 고정연장 없는 타입은 기존 표기 그대로(무회귀).
const decorateFixedOt = (s) => {
  const out = { ...s };
  if (s.preFixedOtStrTime && s.preFixedOtEndTime) {
    out.fstSchTime = `고정연장 ${s.preFixedOtStrTime}-${s.preFixedOtEndTime} + ${
      s.fstSchTime ?? ""
    }`;
  }
  if (s.fixedOtStrTime && s.fixedOtEndTime) {
    const rearText = `고정연장 ${s.fixedOtStrTime}-${s.fixedOtEndTime}`;
    // 후방은 소정 마지막 구간 뒤 — 2구간 타입이면 2구간 셀, 아니면 1구간 셀에 덧붙인다.
    if (s.secSchTime) {
      out.secSchTime = `${s.secSchTime} + ${rearText}`;
    } else {
      out.fstSchTime = `${out.fstSchTime ?? ""} + ${rearText}`;
    }
  }
  return out;
};

const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
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
      schList.value = (response.data.schInfoResultList || []).map(
        decorateFixedOt
      );
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

const fnSave = async () => {
  const filteredData = schList.value.filter((chkpt) => chkpt.chk);
  if (filteredData.length == 0) {
    proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }
  if (!fnDataValidationChk(filteredData)) return;
  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;
  try {
    await axios.post("/webApi/chkLst01/updateschList", filteredData);
    proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
    fnSearch();
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

const fnDelete = async () => {
  const filteredData = schList.value.filter(
    (chkpt) => chkpt.chk && chkpt.chkptCd
  );
  if (filteredData.length == 0) {
    proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }
  const ok = await proxy.$confirm(getMessage(MSG.DELETE_CONFIRM), {
    variant: "danger",
  });
  if (!ok) return;
  try {
    await axios.post("/webApi/chkLst01/deleteschList", filteredData);
    proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
    fnSearch();
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
        siteNo: siteNo.value,
        siteNm: siteNm.value,
      },
    });
    if (response.status === 200) fnCallback(response);
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
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
    if (apiId == "site-lists") {
      const siteList = res.data?.siteInfoResultList ?? [];
      if (siteList.length === 1) {
        siteCd.value = siteList[0].siteCd;
        siteNo.value = siteList[0].siteNo;
        siteNm.value = siteList[0].siteNm;
        // PRAFTA-SUBCON-T2-09: 응답에 미러 여부가 포함되어 있어 즉시 판정.
        isMirrorSite.value = !!siteList[0].linkSrcCmpnyCd;
      } else if (siteList.length > 1) {
        fnSiteSearchPopOpen("searchForm");
      } else {
        siteCd.value = "";
        siteNo.value = "";
        siteNm.value = "";
        isMirrorSite.value = false;
      }
    }
  }
};

const fnCreate = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
    return;
  }
  // PRAFTA-SUBCON-T2-09: 미러 사업장은 근무타입 신규 생성 차단(서버 T2-04 가 최종 강제).
  if (isMirrorSite.value) {
    proxy.$alert("연동 사업장의 근무타입은 제공 회사에서 관리합니다.");
    return;
  }
  fnSchInfoPopOpen(null);
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
  // PRAFTA-SUBCON-T2-09: 팝업 선택 콜백은 코드/명만 전달 — 미러 여부는 별도 판정.
  fnResolveMirrorFlag(siteCdVal);
};

const siteFocusKill = async () => {
  await fnSrchSiteInfo();
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

// F-12-2: 근무타입별 배정현황 조회 팝업(정책 §3.3 삭제/사용중지 사전 확인 지원).
const fnAssignedUsersPopOpen = (sch) => {
  openPop(SchAssignedUsersPop, {
    schData_p: sch,
  });
};

const fnSchInfoPopOpen = (sch) => {
  // PRAFTA-SUBCON-T2-09: 미러 사업장은 근무타입 편집 팝업 진입 차단(이력 조회 버튼은 정상).
  if (isMirrorSite.value) {
    proxy.$alert("연동 사업장의 근무타입은 제공 회사에서 관리합니다.");
    return;
  }
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
/* PRAFTA-SUBCON-T2-09: 연동(미러) 사업장 안내 문구 — Subcon_02 배지 톤 정합 */
.mirror-guide {
  display: inline-block;
  margin-left: 0.5rem;
  padding: 0.05rem 0.4rem;
  border-radius: var(--btn-radius, 8px);
  background: var(--color-primary-bg, #dcfce7);
  color: var(--color-primary, #16a34a);
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.4;
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
