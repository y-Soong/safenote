<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @save="fnSave"
    />
    <!-- 
      @create="fnCreate"
      @save="fnSave"
      @delete="fnDelete"
      @excel="fnExcel" -->

    <div class="viewSearch">
      <div>
        <label>사용자ID</label>
        <input v-model.trim="userId" type="text" />
      </div>
      <div>
        <label>사용자명</label>
        <input v-model.trim="userNm" type="text" />
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
                <th class="event_cell" style="width: 8%">사용자ID</th>
                <th class="event_cell" style="width: 8%">이름</th>
                <th style="width: 8%">이메일</th>
                <th style="width: 8%">휴대폰번호</th>
                <th class="editableCell" style="width: 7%">권한</th>
                <th class="editableCell" style="width: 6%">사용여부</th>
                <th style="width: 8%">소속사업장</th>
                <th style="width: 8%">소속부서</th>
                <th class="editableCell" style="width: 6%">계정상태</th>
                <th style="width: 8%">탈퇴일자</th>
                <th style="width: 25%">권한 소유 사업장</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!userActList || userActList.length === 0">
                <tr>
                  <td colspan="13" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(user, idx) in userActList"
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
                        :key="opt.baimValCd"
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
                        class="ml-auto border rounded"
                        style="
                          background-color: #30796a;
                          border: none;
                          padding: 0.2rem 0.2rem;
                        "
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
                    <BaseSelect
                      v-model="user.accountStatus"
                      disabled
                    >
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
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useModal } from "@/utils/useModal";
import { useFieldWatcher } from "@/utils/useFieldWatcher";
import { getMessage, MSG } from "@/messages";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import BaseSelect from "@/components/common/BaseSelect.vue";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import UserInfoPop from "./popup/UserInfoPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import BatchResultPop from "@/components/popup/BatchResultPop.vue";

// =========================== Define ===========================
defineOptions({ name: "User_01" });
const props = defineProps({
  title: String,
  buttons: Object,
});

// =========================== Ref ===========================
const localButtons = ref({ ...props.buttons });
const userActList = ref([]);
const systCodeArr = ref([]);
const baseInfoArr = ref([]);
const SiteSearchPopOpen = ref(false);
const userId = ref("");
const userNm = ref("");
const useYn = ref();
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true);
const p_userId = ref("");
const headchk = ref(false);
const siteDisabled = ref(false);
const authLevel = ref(sessionStorage.getItem('gv_authLevel'));

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// =========================== Life Cycle ===========================
onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
  await fnGetBaseinfoList();
  await fnSearch();
  console.log(sessionStorage.getItem('gv_authLevel'));
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
        baseCodeList: ["COM005"],
      },
    });

    console.log(response);

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
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

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
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  userActList.value = [];

  try {
    const response = await axios.get("/webApi/user01/user-info-lists", {
      params: {
        userId: userId.value,
        userNm: userNm.value,
        useYn: useYn.value,
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
      },
    });

    if (response.status === 200) {
      userActList.value = response.data.userInfoList;
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
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "저장 중 오류가 발생했습니다.";

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
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

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
  localButtons.value.delete = "N";
};

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
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";
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
  if(userInfo.accountStatus == "04") {    // 회원탈퇴 계정
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
</style>
