<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @create="fnCreate"
      @save="fnSave"
    />
    <!--
      @delete="fnDelete"
      @excel="fnExcel" -->

    <!-- 검색바(1행): 사업장 / 소속부서 / 하위부서 조회
         (2행): 사용자정보(ID·이름 통합) / 사용여부 -->
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
          @click="fnSiteNodeSearchPopOpenForCondition()"
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
          <input
            type="checkbox"
            v-model="incSubNodeYn"
            :disabled="nodeDisabled"
          />
          하위부서 조회
        </label>
      </div>

      <div>
        <label>사용자정보</label>
        <input
          v-model.trim="userKeyword"
          type="text"
          placeholder="사용자ID 또는 이름"
          style="width: 200px"
          @keyup.enter="fnSearch"
        />
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
        <div class="subtitle-row">
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">사용자 리스트</span>
          </div>
          <div class="custom-btn-area">
            <button class="btn btn-custom" @click="fnUserNodeAllAssign">
              부서 일괄지정
            </button>
            <button class="btn btn-custom" @click="fnDownloadTemplate">
              양식 다운로드
            </button>
            <button
              class="btn btn-custom"
              @click="fnUploadExcelClick"
              :disabled="uploadJobActive"
            >
              엑셀 업로드
            </button>
            <input
              ref="excelFileRef"
              type="file"
              accept=".xlsx"
              style="display: none"
              @change="fnExcelFileChange"
            />
          </div>
        </div>

        <!-- 소정-08: 엑셀 양식 안내(14컬럼). 소정근로시간 컬럼 추가에 따른 구양식 호환 안내. -->
        <p class="excel-guide-hint">
          ⓘ 사용자 생성 엑셀 양식은 <strong>14컬럼</strong>입니다.
          <strong>주소정근로시간(필수, 시간 단위)</strong> 컬럼이 추가되어,
          풀타임은 40, 단시간은 실제 계약 시간을 입력합니다. 값이 비어 있으면 해당
          행은 생성되지 않으니, 이전 13컬럼 양식을 쓰던 경우 양식을 다시 내려받아
          주세요.
        </p>

        <!-- PRAFTA-037-F6: 비동기 업로드 진행률 모달 -->
        <div v-if="uploadJobActive" class="upload-progress-overlay">
          <div class="upload-progress-modal">
            <h3>엑셀 업로드 처리 중</h3>
            <p class="upload-progress-text">
              처리 중 {{ uploadProgress.processedRows }} /
              {{ uploadProgress.totalRows }} ({{ uploadProgress.percent }}%)
            </p>
            <div class="upload-progress-bar">
              <div
                class="upload-progress-fill"
                :style="{ width: uploadProgress.percent + '%' }"
              ></div>
            </div>
            <p class="upload-progress-sub">
              완료까지 잠시만 기다려 주세요. 화면을 닫지 마세요.
            </p>
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
                <th style="width: 4%">
                  <input
                    id="headchk"
                    v-model="headchk"
                    type="checkbox"
                    @click="fnHeadchk"
                  />
                </th>
                <ThSortable
                  label="사용자ID"
                  col-key="userId"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.userId"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="이름"
                  col-key="userNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.userNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="이메일"
                  col-key="email"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.email"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="휴대폰번호"
                  col-key="mblNo"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.mblNo"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th class="editableCell" style="width: 7%">권한</th>
                <th class="editableCell" style="width: 6%">직급</th>
                <th class="editableCell" style="width: 6%">사용여부</th>
                <ThSortable
                  label="소속사업장"
                  col-key="siteNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.siteNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th style="width: 8%">소속부서</th>
                <th class="editableCell" style="width: 6%">계정상태</th>
                <ThSortable
                  label="탈퇴일자"
                  col-key="withdrawalDate"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.withdrawalDate"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="권한 소유 사업장"
                  col-key="siteNmList"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.siteNmList"
                  @sort="onSort"
                  @update:width="onResize"
                />
              </tr>
            </thead>
            <tbody>
              <template v-if="!userActList || userActList.length === 0">
                <tr>
                  <td colspan="14" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(user, idx) in sortedData"
                  :key="user.id"
                  :class="{ 'row-locked': isRowLocked(user) }"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input
                      type="checkbox"
                      v-model="user.chk"
                      :disabled="isRowLocked(user)"
                    />
                  </td>
                  <td @dblclick="!isRowLocked(user) && fnUserInfoPopOpen(user)">
                    {{ user.userId }}
                  </td>
                  <td @dblclick="!isRowLocked(user) && fnUserInfoPopOpen(user)">
                    {{ user.userNm }}
                  </td>
                  <td @dblclick="!isRowLocked(user) && fnUserInfoPopOpen(user)">
                    {{ user.email }}
                  </td>
                  <td @dblclick="!isRowLocked(user) && fnUserInfoPopOpen(user)">
                    {{ proxy.$util.formatPhoneNumber(user.mblNo) }}
                  </td>
                  <td @dblclick="!isRowLocked(user) && fnUserInfoPopOpen(user)">
                    <BaseSelect
                      v-model="user.authCd"
                      :disabled="isRowLocked(user)"
                    >
                      <option
                        v-for="opt in (baseInfoArr['COM005'] || []).filter(
                          (o) => o.baimValDCd != null
                        )"
                        :key="opt.baimValDCd"
                        :value="opt.baimValDCd"
                      >
                        {{ opt.baimValDNm }}
                      </option>
                    </BaseSelect>
                  </td>
                  <td @dblclick="!isRowLocked(user) && fnUserInfoPopOpen(user)">
                    <BaseSelect
                      v-model="user.rankCd"
                      :disabled="isRowLocked(user)"
                    >
                      <option :value="null">-</option>
                      <option
                        v-for="opt in (baseInfoArr['COM007'] || []).filter(
                          (o) => o.baimValDCd != null
                        )"
                        :key="opt.baimValDCd"
                        :value="opt.baimValDCd"
                      >
                        {{ opt.baimValDNm }}
                      </option>
                    </BaseSelect>
                  </td>
                  <td @dblclick="!isRowLocked(user) && fnUserInfoPopOpen(user)">
                    <BaseSelect
                      v-model="user.useYn"
                      :disabled="isRowLocked(user)"
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
                  <td @dblclick="!isRowLocked(user) && fnUserInfoPopOpen(user)">
                    {{ user.siteNm }}
                  </td>
                  <td @dblclick="!isRowLocked(user) && fnUserInfoPopOpen(user)">
                    <div class="flex items-center gap-2 w-full">
                      <span class="truncate min-w-0">{{ user.nodeNm }}</span>
                      <button
                        class="ml-auto border rounded node-assign-btn"
                        :disabled="isRowLocked(user)"
                        @click="fnSiteNodeSearchPopOpen(user)"
                      >
                        <img
                          class="search_icon"
                          :src="search_icon"
                          alt="검색"
                        />
                      </button>
                    </div>
                  </td>
                  <td @dblclick="!isRowLocked(user) && fnUserInfoPopOpen(user)">
                    <BaseSelect v-model="user.accountStatus" disabled>
                      <option
                        v-for="opt in (systCodeArr['SYS013'] || []).filter(
                          (o) => o.systValDCd != null
                        )"
                        :key="opt.systValDCd"
                        :value="opt.systValDCd"
                      >
                        {{ opt.systValDNm }}
                      </option>
                    </BaseSelect>
                  </td>
                  <td @dblclick="!isRowLocked(user) && fnUserInfoPopOpen(user)">
                    {{ user.withdrawalDate }}
                  </td>
                  <td
                    @dblclick="!isRowLocked(user) && fnUserInfoPopOpen(user)"
                    style="
                      max-width: 0;
                      overflow: hidden;
                      text-overflow: ellipsis;
                      white-space: nowrap;
                    "
                    :title="user.siteNmList"
                  >
                    {{ user.siteNmList }}
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
/* eslint-disable */
import {
  ref,
  defineProps,
  onMounted,
  onBeforeUnmount,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useModal } from "@/utils/useModal";
import { useFieldWatcher } from "@/utils/useFieldWatcher";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import BaseSelect from "@/components/common/BaseSelect.vue";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import UserInfoPop from "./popup/UserInfoPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import BatchResultPop from "@/components/popup/BatchResultPop.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import { useTableSort, useColumnResize } from "@/composables/useTableFeatures.js";

// =========================== Define ===========================
defineOptions({ name: "User_01" });
const props = defineProps({
  title: String,
  buttons: Object,
});

// =========================== Ref ===========================
const localButtons = ref({ ...props.buttons });
const userActList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(userActList);
const { colWidths, onResize } = useColumnResize({
  userId: 100,
  userNm: 90,
  email: 140,
  mblNo: 120,
  siteNm: 110,
  withdrawalDate: 110,
  siteNmList: 200,
});
const systCodeArr = ref([]);
const baseInfoArr = ref([]);
const SiteSearchPopOpen = ref(false);
// 사용자정보 통합 검색어(사용자ID·사용자명 동시 부분일치). 기존 userId/userNm 분리 조건 대체.
const userKeyword = ref("");
const useYn = ref();
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true);
// 소속부서 하위부서 포함 조회 여부(근무계획관리 Attd_05 동일 패턴)
const incSubNodeYn = ref(false);
const p_userId = ref("");
const headchk = ref(false);
const siteDisabled = ref(false);
const authLevel = ref(sessionStorage.getItem('gv_authLevel'));

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// =========================== Life Cycle ===========================
const getSession = (key) => {
  const v = sessionStorage.getItem(key);
  return v && v !== "null" && v !== "undefined" ? v : "";
};

const fnInit = () => {
  siteCd.value = getSession("gv_siteCd");
  siteNo.value = getSession("gv_siteNo");
  siteNm.value = getSession("gv_siteNm");
  if (siteCd.value) {
    nodeDisabled.value = false;
    nodeCd.value = getSession("gv_nodeCd");
    nodeNm.value = getSession("gv_nodeNm");
  }
};

onMounted(async () => {
  fnInit();
  fnButtonControll();
  await fnGetSystinfoList();
  await fnGetBaseinfoList();
  await fnSearch();
});

// =========================== Watch, Watcher ===========================
useFieldWatcher(
  userActList,
  (item) => {
    item.chk = true;
  },
  ["chk"]
);

// =========================== Methods ===========================
const fnGetBaseinfoList = async () => {

  try {
    const response = await axios.get("/comApi/baseinfo/base-info-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        baseCodeList: ["COM005", "COM007"],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.baseInfoList || [];

      const grouped = {};
      resData.forEach((item) => {
        const key = item.baimValCd;
        if (item.baimValDCd == null) return;
        if (!grouped[key]) {
          grouped[key] = [];
        }
        grouped[key].push(item);
      });
      baseInfoArr.value = grouped;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS003", "SYS013"],
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
  userActList.value = [];

  try {
    const response = await axios.get("/webApi/user01/user-info-lists", {
      params: {
        userKeyword: userKeyword.value,
        useYn: useYn.value,
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
      },
    });

    if (response.status === 200) {
      userActList.value = response.data.userInfoList;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSave = async () => {
  const filteredUsers = userActList.value.filter((user) => user.chk);

  if (filteredUsers.length == 0) {
    proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/user01/update-user-infos",
      filteredUsers
    );

    if (response.status === 200) {
      if (response.data.failCount > 0) {
        openPop(BatchResultPop, {
          totalCount: response.data.totalCount,
          successCount: response.data.successCount,
          failCount: response.data.failCount,
          identifierLabel: "사용자ID",
          dataList: response.data.fails,
        });
      } else {
        proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      }

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

const focusKill = (e) => {
  if (e.target.id == "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    } else {
      siteNm.value = "";
      siteFocusKill();
    }
  } else if (e.target.id == "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    } else {
      siteNo.value = "";
      siteFocusKill();
    }
  } else if (e.target.id == "nodeCd") {
    if (proxy.$util.isEmpty(nodeCd.value)) {
      nodeCd.value = "";
      nodeNm.value = "";
      return;
    } else {
      nodeNm.value = "";
      nodeFocusKill();
    }
  } else if (e.target.id == "nodeNm") {
    if (proxy.$util.isEmpty(nodeNm.value)) {
      nodeCd.value = "";
      nodeNm.value = "";
      return;
    } else {
      nodeCd.value = "";
      nodeFocusKill();
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
        nodeDisabled.value = false;
        // 사업장 변경 시 소속부서 초기화(부서는 사업장 종속 — 팝업 선택 경로와 정합)
        nodeCd.value = "";
        nodeNm.value = "";
      } else if (siteList.length > 1) {
        fnSiteSearchPopOpen();
        SiteSearchPopOpen.value = true;
      } else {
        siteCd.value = "";
        siteNo.value = "";
        siteNm.value = "";
        nodeDisabled.value = true;
        nodeCd.value = "";
        nodeNm.value = "";
      }
    } else if (apiId == "site-node-lists") {
      const list = res.data?.siteNodeInfoList || [];
      if (list.length === 0) {
        nodeCd.value = "";
        nodeNm.value = "";
      } else if (list.length === 1) {
        nodeCd.value = list[0].nodeCd ?? "";
        nodeNm.value = list[0].nodeNm ?? "";
      } else {
        fnSiteNodeSearchPopOpenForCondition();
      }
    }
  }
};

const fnButtonControll = () => {
  // 관리자 단건/엑셀 일괄 사용자 생성(prafta-036)을 위해 ViewHeader 생성 버튼 활성화.
  localButtons.value.create = "Y";
  localButtons.value.delete = "N";
};

const excelFileRef = ref(null);

// PRAFTA-037-F6: 비동기 업로드 진행률 상태
const uploadJobActive = ref(false);
const uploadProgress = ref({
  jobId: "",
  totalRows: 0,
  processedRows: 0,
  successCount: 0,
  failCount: 0,
  percent: 0,
});
// 서식 유실 보정 내역 — [{ rowNo, columnNm, before, after }].
//   업로드 시작 응답으로 즉시 받아 두었다가 처리 완료 시점에 함께 안내한다.
const uploadAdjustments = ref([]);
let uploadPollTimer = null;
const POLL_INTERVAL_MS = 1500;

const fnCreate = () => {
  // UserInfoPop 을 'C'(생성) 모드로 재사용. 저장 성공 시 onSearch 콜백으로 리스트 갱신.
  openPop(UserInfoPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    userId_p: "",
    callmethod_p: "C",
    onSearch: fnSearch,
  });
};

const fnDownloadTemplate = async () => {
  try {
    const response = await axios.get("/webApi/user01/user-create-template", {
      responseType: "blob",
    });

    if (response.status === 200) {
      const blob = new Blob([response.data], {
        type:
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "사용자생성양식.xlsx";
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "양식 다운로드 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

const fnUploadExcelClick = () => {
  if (excelFileRef.value) {
    excelFileRef.value.value = "";
    excelFileRef.value.click();
  }
};

// PRAFTA-037-F6: 비동기 업로드 — 즉시 jobId 응답 + 1.5초 간격 폴링.
const fnExcelFileChange = async (event) => {
  const file = event.target.files && event.target.files[0];
  if (!file) return;

  try {
    const formData = new FormData();
    formData.append("file", file);

    const response = await axios.post(
      "/webApi/user01/upload-user-creates-async",
      formData,
      { headers: { "Content-Type": "multipart/form-data" } }
    );

    if (response.status === 200) {
      const { jobId, totalRows, adjustments } = response.data || {};
      if (!jobId) {
        await proxy.$alert("업로드 작업을 시작하지 못했습니다.");
        return;
      }
      // 엑셀 서식 유실로 앞자리 0 이 떨어진 값을 서버가 복원한 내역.
      //   조용히 고치지 않고 처리 결과와 함께 보여준다(사용자가 눈으로 검증할 수 있도록).
      uploadAdjustments.value = Array.isArray(adjustments) ? adjustments : [];
      // 진행률 초기화 + 모달 노출 + 폴링 시작.
      uploadJobActive.value = true;
      uploadProgress.value = {
        jobId,
        totalRows: totalRows || 0,
        processedRows: 0,
        successCount: 0,
        failCount: 0,
        percent: 0,
      };
      fnStartUploadJobPolling(jobId);
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "엑셀 업로드 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    // 같은 파일 재선택을 위해 input 초기화 (폴링은 별개 흐름).
    if (excelFileRef.value) {
      excelFileRef.value.value = "";
    }
  }
};

const fnStartUploadJobPolling = (jobId) => {
  fnStopUploadJobPolling();
  uploadPollTimer = setInterval(() => fnPollUploadJob(jobId), POLL_INTERVAL_MS);
  // 첫 폴링은 즉시 1회.
  fnPollUploadJob(jobId);
};

const fnStopUploadJobPolling = () => {
  if (uploadPollTimer) {
    clearInterval(uploadPollTimer);
    uploadPollTimer = null;
  }
};

// 서식 유실 보정 내역 안내. 건수가 많을 수 있어 앞 10건만 나열하고 나머지는 건수로 요약한다.
const MAX_ADJUST_PREVIEW = 10;
const fnAlertAdjustments = async () => {
  const list = uploadAdjustments.value || [];
  if (list.length === 0) return;

  const lines = list
    .slice(0, MAX_ADJUST_PREVIEW)
    .map((a) => `· ${a.rowNo}행 ${a.columnNm}: ${a.before} → ${a.after}`)
    .join("\n");
  const more =
    list.length > MAX_ADJUST_PREVIEW
      ? `\n외 ${list.length - MAX_ADJUST_PREVIEW}건`
      : "";

  await proxy.$alert(
    `엑셀 서식 때문에 앞자리 0 이 빠진 값 ${list.length}건을 보정해 저장했습니다.\n` +
      `값이 맞는지 확인해 주세요.\n\n${lines}${more}`
  );
  uploadAdjustments.value = [];
};

const fnPollUploadJob = async (jobId) => {
  try {
    const response = await axios.get(`/webApi/user01/upload-job/${jobId}`);
    if (response.status !== 200 || !response.data) return;

    const {
      status,
      totalRows,
      processedRows,
      successCount,
      failCount,
      fails,
      errorMsg,
    } = response.data;

    const percent = totalRows > 0
      ? Math.min(100, Math.floor((processedRows / totalRows) * 100))
      : 0;
    uploadProgress.value = {
      jobId,
      totalRows,
      processedRows,
      successCount,
      failCount,
      percent,
    };

    if (status === "SUCCESS" || status === "PARTIAL" || status === "FAILED") {
      // 최종 상태 — 폴링 종료.
      fnStopUploadJobPolling();
      uploadJobActive.value = false;

      if (status === "FAILED") {
        await proxy.$alert(errorMsg || "엑셀 업로드 처리 중 오류가 발생했습니다.");
        return;
      }
      if (failCount > 0) {
        openPop(BatchResultPop, {
          totalCount: totalRows,
          successCount,
          failCount,
          identifierLabel: "사용자ID",
          dataList: fails || [],
        });
      } else {
        await proxy.$alert(`총 ${totalRows}건 모두 생성 완료되었습니다.`);
      }
      // 서식 유실 보정이 있었다면 결과 안내 뒤에 이어서 알린다.
      //   조용히 고치면 사용자가 잘못된 값이 들어간 줄 모른 채 넘어간다.
      await fnAlertAdjustments();
      fnSearch();
    }
  } catch (err) {
    // 폴링 한 사이클 실패는 무시(다음 사이클 재시도). 단 404 등 명백한 에러면 중단.
    const code = err?.response?.status;
    if (code === 404 || code === 403) {
      fnStopUploadJobPolling();
      uploadJobActive.value = false;
      await proxy.$alert(
        resolveApiErrorMessage(err, "업로드 작업 상태를 조회할 수 없습니다.")
      );
    }
  }
};

onBeforeUnmount(() => {
  // 페이지 이탈 시 폴링 정리.
  fnStopUploadJobPolling();
});

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
  nodeDisabled.value = false;
  nodeCd.value = "";
  nodeNm.value = "";
};

const onSiteNodeSelected = (userIdVal, nodeCdVal, nodeNmVal) => {
  const target = userActList.value.find((u) => u.userId === userIdVal);
  if (target) {
    target.nodeCd = nodeCdVal;
    target.nodeNm = nodeNmVal;
  }
};

const siteFocusKill = async () => {
  await fnSrchSiteInfo();
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
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

const nodeFocusKill = async () => {
  await fnSrchNodeInfo();
};

const fnSiteNodeSearchPopOpenForCondition = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
    return;
  }
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    nodeCd_p: "",
    userId_p: "",
    onSelect: onSiteNodeSelectedForCondition,
  });
};

const onSiteNodeSelectedForCondition = (nodeCdVal, nodeNmVal) => {
  nodeCd.value = nodeCdVal ?? "";
  nodeNm.value = nodeNmVal ?? "";
};

const fnHeadchk = () => {
  headchk.value = !headchk.value;
  userActList.value.forEach((item) => {
    item.chk = headchk.value;
  });
};

const fnUserInfoPopOpen = (userInfo) => {
  if(userInfo.accountStatus == "03") {    // 회원탈퇴 계정
    proxy.$alert("탈퇴된 계정은 상세보기를 지원하지 않습니다.");
    return;
  }
  
  p_userId.value = userInfo.userId;

  openPop(UserInfoPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    userId_p: p_userId.value,
    callmethod_p: "S",
    onSearch: fnSearch,
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

const fnSiteNodeSearchPopOpen = (user) => {
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: user.siteCd,
    nodeCd_p: "",
    userId_p: user.userId,
    onSelect: onSiteNodeSelected,
  });
};

const fnUserNodeAllAssign = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
    return;
  }

  if (userActList.value.filter((u) => u.chk === true).length === 0) {
    proxy.$alert(getMessage(MSG.USER_SELECT_REQUIRED));
    return;
  } else if (
    userActList.value.filter((u) => u.chk === true && u.siteCd != siteCd.value)
      .length > 0
  ) {
    proxy.$alert(getMessage(MSG.SITE_USER_ONLY, { siteNm: siteNm.value }));
    return;
  }

  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    onSelect: onSiteNodeAllAssignSelected,
  });
};

const onSiteNodeAllAssignSelected = (nodeCdVal, nodeNmVal) => {
  const targets = userActList.value.filter(
    (u) => u.chk === true && u.siteCd === siteCd.value
  );
  targets.forEach((target) => {
    target.nodeCd = nodeCdVal;
    target.nodeNm = nodeNmVal;
  });
};

const isRowLocked = (user) => Number(user.authLevel) < Number(authLevel.value);
</script>

<style scoped>
.row-locked {
  opacity: 0.5;
  pointer-events: none;
}

/* 조회조건이 여러 행으로 줄바꿈될 때 각 행의 왼쪽 끝선을 사업장과 맞춘다.
   (전역 form.css는 첫 항목에만 margin-left를 줘서 두 번째 행이 좌측으로 밀린다.) (Attd_14 패턴) */
.viewSearch {
  padding-left: calc(0.5rem + var(--space-md, 0.75rem));
  /* 행 간 간격 축소(열 간격은 유지) */
  row-gap: 0.5rem;
}
.viewSearch > div:first-child {
  margin-left: 0;
}

/* 하위부서 조회 체크박스 (Attd_14 checkbox-label 패턴 차용) */
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

/* 소정-08: 엑셀 양식(14컬럼) 안내 문구 */
.excel-guide-hint {
  margin: 0 0 0.5rem;
  padding: 0.5rem 0.75rem;
  border-radius: var(--input-radius, 10px);
  background: var(--color-info-bg, #eff6ff);
  color: var(--color-info-text, #1d4ed8);
  font-size: 0.75rem;
  line-height: 1.5;
}

.excel-guide-hint strong {
  font-weight: 600;
}

/* PRAFTA-037-F6: 비동기 업로드 진행률 모달 */
.upload-progress-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
}
.upload-progress-modal {
  width: 22rem;
  max-width: 90vw;
  background: var(--color-surface, #ffffff);
  border-radius: var(--btn-radius, 8px);
  padding: 1.25rem 1.5rem;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
  text-align: center;
}
.upload-progress-modal h3 {
  margin: 0 0 0.75rem;
  font-size: 1rem;
  color: var(--color-text, #1f2937);
}
.upload-progress-text {
  margin: 0 0 0.5rem;
  font-size: 0.9rem;
  color: var(--color-text, #1f2937);
  font-variant-numeric: tabular-nums;
}
.upload-progress-bar {
  width: 100%;
  height: 0.5rem;
  background: var(--color-bg, #f3f4f6);
  border-radius: 999px;
  overflow: hidden;
  margin-bottom: 0.5rem;
}
.upload-progress-fill {
  height: 100%;
  background: var(--color-primary, #2563eb);
  transition: width 0.25s ease;
}
.upload-progress-sub {
  margin: 0;
  font-size: 0.75rem;
  color: var(--color-text-muted, #6b7280);
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
