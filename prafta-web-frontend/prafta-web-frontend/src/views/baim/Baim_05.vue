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
          <span class="subtitle-text">계정슬롯 리스트</span>
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
                <th class="event_cell" style="width: 10%">사업장</th>
                <th class="event_cell" style="width: 5%">슬롯번호</th>
                <th class="editableCell" style="width: 8%">슬롯구분</th>
                <th style="width: 8%">슬롯상태</th>
                <th style="width: 8%">사용여부</th>
                <th class="editableCell" style="width: 8%">슬롯 점유자</th>
                <th style="width: 8%">슬롯점유 이력보기</th>
              </tr>
            </thead>
            <tbody>
              <template
                v-if="!DailyUserSlotList || DailyUserSlotList.length === 0"
              >
                <tr>
                  <td colspan="9" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(dailyUserSlot, idx) in DailyUserSlotList"
                  :key="dailyUserSlot.id"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input type="checkbox" v-model="dailyUserSlot.chk" />
                  </td>
                  <td>
                    {{ dailyUserSlot.siteNm }}
                  </td>
                  <td>
                    {{ dailyUserSlot.slotNo }}
                  </td>
                  <td
                    :style="{
                      backgroundColor:
                        dailyUserSlot.slotType === '02' ? '#ffe6e6' : '#e9f4f0',
                    }"
                  >
                    <BaseSelect
                      v-model="dailyUserSlot.slotType"
                      :readonly="false"
                      name="useYn"
                    >
                      <option
                        v-for="opt in (systCodeArr['SYS014'] || []).filter(
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
                    <BaseSelect
                      v-model="dailyUserSlot.slotStatus"
                      :readonly="true"
                      name="useYn"
                    >
                      <option
                        v-for="opt in (systCodeArr['SYS015'] || []).filter(
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
                    <BaseSelect
                      v-model="dailyUserSlot.useYn"
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
                    <div class="flex items-center gap-2 w-full">
                      <span class="truncate min-w-0">{{
                        dailyUserSlot.currUserNm
                      }}</span>
                      <button
                        class="ml-auto border rounded"
                        style="
                          background-color: #30796a;
                          border: none;
                          padding: 0.2rem 0.2rem;
                        "
                        @click="fnUserSearchPopOpen(idx)"
                      >
                        <img
                          class="search_icon"
                          :src="search_icon"
                          alt="검색"
                        />
                      </button>
                    </div>
                  </td>
                  <td>
                    <div class="flex items-center gap-2 w-full">
                      <button
                        class="border rounded"
                        style="
                          background-color: #30796a;
                          border: none;
                          padding: 0.2rem 0.5rem;
                          color: #fff; /* ← 글자색 흰색 */
                        "
                        @click="fnQrCodePopOpen(linkPolicy)"
                      >
                        이력보기
                      </button>
                    </div>
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
import BaseSelect from "@/components/common/BaseSelect.vue";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import UserSearchPop from "@/components/popup/UserSearchPop.vue";
// import QrCodePop from "@/components/popup/QrCodePop.vue";

// ================ Options ================
defineOptions({ name: "Baim_05" });

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
const DailyUserSlotList = ref([]);
const systCodeArr = ref({});
const SiteSearchPopOpen = ref(false);

// 조회조건 변수
const useYn = ref();
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const slotType = ref("");
const slotStatus = ref("");
const currUserId = ref("");

// 화면 제어 변수
const headChk = ref(false);
const siteDisabled = ref(false);

// ================ Watchers ================
useFieldWatcher(
  DailyUserSlotList,
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
    const response = await axios.get("/comApi/baseinfo/syst-info-list", {
      params: {
        systCodeList: ["SYS003", "SYS014", "SYS015"],
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
  DailyUserSlotList.value = [];

  try {
    const response = await axios.get("/webApi/baim05/daily-user-slot-lists", {
      params: {
        siteCd: siteCd.value,
        slotType: slotType.value,
        slotStatus: slotStatus.value,
        useYn: useYn.value,
        currUserId: currUserId.value,
      },
    });

    if (response.status === 200) {
      DailyUserSlotList.value = response.data?.dailyUserSlotList || [];
      // // dayLimitCnt 기본값 설정
      // DailyUserSlotList.value.forEach((item) => {
      //   if (item.dayLimitCnt == null || item.dayLimitCnt === "") {
      //     item.dayLimitCnt = 0;
      //   }
      // });
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
  const filteredData = DailyUserSlotList.value.filter(
    (dailyUserSlot) => dailyUserSlot.chk
  );

  if (filteredData.length == 0) {
    proxy.$alert("저장할 데이터가 없습니다.");
    return;
  }

  // dayLimitCnt가 비어있으면 0으로 치환
  filteredData.forEach((item) => {
    if (item.dayLimitCnt == null || item.dayLimitCnt === "") {
      item.dayLimitCnt = 0;
    }
  });

  const ok = await proxy.$confirm("저장하시겠습니까 ?");
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/baim04/save-daily-user-link-policies",
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
  const filteredData = DailyUserSlotList.value.filter(
    (dailyUserSlot) => dailyUserSlot.chk
  );

  if (filteredData.length == 0) {
    proxy.$alert("삭제할 데이터가 없습니다.");
    return;
  }

  const ok = await proxy.$confirm("삭제하시겠습니까 ?");
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/baim04/delete-daily-user-link-policies",
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

const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.post("/comApi/baseinfo/getSiteInfoList", {
      cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
      siteNo: siteNo.value,
      siteNm: siteNm.value,
      useYn: useYn.value,
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

// ================ Methods/Functions ================
const fnButtonControll = () => {
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

const siteFocusKill = async () => {
  await fnSrchSiteInfo();
};

const fnHeadChk = () => {
  headChk.value = !headChk.value;
  DailyUserSlotList.value.forEach((item) => {
    item.chk = headChk.value;
  });
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: siteNo.value,
    siteNm_p: siteNm.value,
    onSelect: onSiteSelected,
  });
};

const fnUserSearchPopOpen = (callPoint) => {
  openPop(UserSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    onSelect: (userIdVal, userNmVal) => {
      DailyUserSlotList.value[callPoint].currUserId = userIdVal;
      DailyUserSlotList.value[callPoint].currUserNm = userNmVal;
    },
  });
};
</script>

<style scoped></style>
