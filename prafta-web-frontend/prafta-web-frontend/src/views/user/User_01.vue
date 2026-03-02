<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="props.buttons"
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
                <th class="event_cell" style="width: 10%">사용자ID</th>
                <th class="event_cell" style="width: 10%">이름</th>
                <th style="width: 15%">이메일</th>
                <th style="width: 15%">휴대폰번호</th>
                <th class="editableCell" style="width: 8%">권한</th>
                <th class="editableCell" style="width: 8%">사용여부</th>
                <th style="width: 10%">소속사업장</th>
                <th style="width: 10%">소속부서</th>
                <th>권한 소유 사업장</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!userActList || userActList.length === 0">
                <tr>
                  <td colspan="11" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(user, idx) in userActList" :key="user.id">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input type="checkbox" v-model="user.chk" />
                  </td>
                  <td @dblclick="fnUserInfoPopOpen(user)">
                    {{ user.userId }}
                  </td>
                  <td @dblclick="fnUserInfoPopOpen(user)">
                    {{ user.userNm }}
                  </td>
                  <td @dblclick="fnUserInfoPopOpen(user)">{{ user.email }}</td>
                  <td @dblclick="fnUserInfoPopOpen(user)">
                    {{ proxy.$util.formatPhoneNumber(user.mblNo) }}
                  </td>
                  <td @dblclick="fnUserInfoPopOpen(user)">
                    <BaseSelect v-model="user.authCd">
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
                  <td @dblclick="fnUserInfoPopOpen(user)">
                    <BaseSelect v-model="user.useYn">
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
                  <td @dblclick="fnUserInfoPopOpen(user)">
                    {{ user.siteNm }}
                  </td>
                  <td @dblclick="fnUserInfoPopOpen(user)">
                    <div class="flex items-center gap-2 w-full">
                      <span class="truncate min-w-0">{{ user.nodeNm }}</span>
                      <button
                        class="ml-auto border rounded"
                        style="
                          background-color: #30796a;
                          border: none;
                          padding: 0.2rem 0.2rem;
                        "
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
                  <td @dblclick="fnUserInfoPopOpen(user)">
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
} from 'vue';
import { useModal } from '@/utils/useModal';
import { useFieldWatcher } from '@/utils/useFieldWatcher';
import axios from '@/api/axios';
import ViewHeader from '@/components/common/ViewHeader.vue';
import BaseSelect from '@/components/common/BaseSelect.vue';
import search_icon from '@/assets/img/search_icon.png';
import SiteSearchPop from '@/components/popup/SiteSearchPop.vue';
import UserInfoPop from './popup/UserInfoPop.vue';
import SiteNodeSearchPop from '@/components/popup/SiteNodeSearchPop.vue';

// =========================== Define ===========================
defineOptions({ name: 'User_01' });
const props = defineProps({
  title: String,
  buttons: Object,
});

// =========================== Ref ===========================
const userActList = ref([]);
const systCodeArr = ref([]);
const baseInfoArr = ref([]);
const SiteSearchPopOpen = ref(false);
const userId = ref('');
const userNm = ref('');
const useYn = ref();
const siteCd = ref('');
const siteNo = ref('');
const siteNm = ref('');
const p_userId = ref('');
const headchk = ref(false);
const siteDisabled = ref(false);

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// =========================== Life Cycle ===========================
onMounted(async () => {
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
  ['chk']
);

// =========================== Methods ===========================
const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get('/comApi/baseinfo/base-info-list', {
      params: {
        cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
        baseCodeList: ['COM005'],
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
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '조회 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-list", {
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
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  try {
    const response = await axios.get("/webApi/user01/user-info-lists", {
      params: {
        userId: userId.value,
        userNm: userNm.value,
        useYn: useYn.value,
        siteCd: siteCd.value,
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
  // const dataList = proxy.$util.toCamelCaseKeys(filteredUsers);

  if (filteredUsers.length == 0) {
    proxy.$alert("저장할 데이터가 없습니다.");
    return;
  }

  const ok = await proxy.$confirm("저장하시겠습니까 ?");
  if (!ok) return;

  console.log(filteredUsers);

  try {
    const response = await axios.post(
      "/webApi/user01/update-user-infos",
      filteredUsers
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

const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.post("/comApi/baseinfo/getSiteInfoList", {
      cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
      siteNo: siteNo.value,
      siteNm: siteNm.value,
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

    if (apiId == "getSiteInfoList") {
      if (res.data.length == 1) {
        siteCd.value = res.data[0].SITE_CD;
        siteNo.value = res.data[0].SITE_NO;
        siteNm.value = res.data[0].SITE_NM;
      } else if (res.data.length > 1) {
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

const fnHeadchk = () => {
  headchk.value = !headchk.value;
  userActList.value.forEach((item) => {
    item.chk = headchk.value;
  });
};

const fnUserInfoPopOpen = (userInfo) => {
  p_userId.value = userInfo.userId;

  openPop(UserInfoPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    userId_p: p_userId.value,
    callmethod_p: "S",
    onSearch: fnSearch,
  });
};

const fnBatchRegister = () => {
  proxy.$alert('준비 중입니다.');
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: siteNo.value,
    siteNm_p: siteNm.value,
    onSelect: onSiteSelected,
  });
};

const fnSiteNodeSearchPopOpen = (user) => {
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: user.siteCd,
    nodeCd_p: user.nodeCd,
    userId_p: user.userId,
    onSelect: onSiteNodeSelected,
  });
};

const fnUserNodeAllAssign = () => {
  if(proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert('사업장을 선택해주세요.');
    return;
  }

  if(userActList.value.filter((u) => u.chk === true).length === 0) {
    proxy.$alert('지정할 사용자를 선택해주세요.');
    return;
  } else if(userActList.value.filter((u) => u.chk === true && u.siteCd != siteCd.value).length > 0) {
    proxy.$alert('[' + siteNm.value + '] 사업장에 속한\n사용자만 지정할 수 있습니다.');
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
</script>

<style scoped></style>
