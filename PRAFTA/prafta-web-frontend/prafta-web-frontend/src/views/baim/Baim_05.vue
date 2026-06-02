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
          ref="siteNoFcs"
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
        <label>슬롯 점유 상태</label>
        <select v-model.trim="slotStatus" name="combo">
          <option
            v-for="opt in systCodeArr['SYS015'] || []"
            :key="opt.systValDCd"
            :value="opt.systValDCd"
          >
            {{ opt.systValDNm }}
          </option>
        </select>
      </div>

      <div>
        <label>슬롯 구분</label>
        <select v-model.trim="slotType" name="combo">
          <option
            v-for="opt in systCodeArr['SYS014'] || []"
            :key="opt.systValDCd"
            :value="opt.systValDCd"
          >
            {{ opt.systValDNm }}
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

      <div>
        <label>근로자명</label>
        <input v-model.trim="currUserNm" type="text" />
      </div>
    </div>

    <div class="viewBody">
      <!-- 액션 카드 행 -->
      <div class="action-cards">
        <!-- 1) 활성화 계정 / 계정 등록 -->
        <div class="action-card">
          <div class="action-card-head">
            <span class="action-card-icon" aria-hidden="true">
              <svg
                viewBox="0 0 24 24"
                width="16"
                height="16"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M20 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M4 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2" />
                <circle cx="10" cy="7" r="4" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
              </svg>
            </span>
            <span class="action-card-title">활성화 계정</span>
            <span class="action-card-dash">—</span>
            <span class="action-card-count">
              {{ activeAccountCount }} /
              <input
                type="number"
                class="max-count-input"
                v-model.number="dayLimitCnt"
                min="0"
              />
            </span>
            <span class="account-register-group">
              <span class="account-register-label">계정 등록</span>
              <span class="radio-group">
                <label class="radio-item">
                  <input type="radio" v-model="dailyUserJoinYn" :value="true" />
                  ON
                </label>
                <label class="radio-item">
                  <input
                    type="radio"
                    v-model="dailyUserJoinYn"
                    :value="false"
                  />
                  OFF
                </label>
              </span>
            </span>
          </div>
          <div class="action-card-body">
            <button class="btn btn-primary btn-sm" @click="fnSave">저장</button>
          </div>
        </div>

        <!-- 2) 직접가입 -->
        <div class="action-card">
          <div class="action-card-head">
            <span class="action-card-icon" aria-hidden="true">
              <svg
                viewBox="0 0 24 24"
                width="16"
                height="16"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path
                  d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.72"
                />
                <path
                  d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.72-1.72"
                />
              </svg>
            </span>
            <span class="action-card-title">직접가입</span>
            <span
              class="action-card-help"
              title="사용자가 링크/QR로 직접 가입할 수 있습니다."
              >?</span
            >
          </div>
          <div class="action-card-body">
            <button
              class="btn btn-primary btn-sm"
              :disabled="!siteCd"
              @click="fnCopyJoinLink"
            >
              <svg
                viewBox="0 0 24 24"
                width="13"
                height="13"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path
                  d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.72"
                />
                <path
                  d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.72-1.72"
                />
              </svg>
              링크 복사
            </button>
            <button
              class="btn btn-primary btn-sm"
              :disabled="!siteCd"
              @click="fnQrCodePopOpen"
            >
              QRCODE
            </button>
          </div>
        </div>

        <!-- 3) 관리자 QR 발급 -->
        <div class="action-card">
          <div class="action-card-head">
            <span class="action-card-icon" aria-hidden="true">
              <svg
                viewBox="0 0 24 24"
                width="16"
                height="16"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <rect x="3" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="3" width="7" height="7" rx="1" />
                <rect x="3" y="14" width="7" height="7" rx="1" />
                <path d="M14 14h3v3h-3zM18 18h3v3h-3z" />
              </svg>
            </span>
            <span class="action-card-title">관리자 QR 발급</span>
            <span
              class="action-card-help"
              title="관리자가 직접 QR을 발급합니다."
              >?</span
            >
          </div>
          <div class="action-card-body">
            <button
              class="btn btn-primary btn-sm"
              :disabled="!siteCd"
              @click="fnGenerateAdminQr"
            >
              + QR 생성
            </button>
          </div>
        </div>
      </div>

      <div class="table-wrapper subtitle-pane">
        <div class="subtitle-row">
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

          <div class="custom-btn-area">
            <button class="btn btn-custom" @click="fnBulkOccupy">
              일괄 점유 유지
            </button>
            <button class="btn btn-custom" @click="fnBulkUnoccupy">
              일괄 점유 해지
            </button>
            <button class="btn btn-custom-danger" @click="fnBulkClear">
              일괄 비우기
            </button>
          </div>
        </div>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="
            --box-h: calc(70vh - 80px);
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
                <th style="width: 4%">
                  <input
                    id="headChk"
                    v-model="headChk"
                    type="checkbox"
                    @click="fnHeadChk"
                  />
                </th>
                <ThSortable
                  label="근로자"
                  col-key="userNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.userNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="연락처"
                  col-key="mblNo"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.mblNo"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="사업장"
                  col-key="siteNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.siteNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="슬롯번호"
                  col-key="slotNo"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.slotNo"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th style="width: 130px">구분</th>
                <th style="width: 100px">상태</th>
                <th style="width: 90px">QRCODE</th>
                <th style="width: 120px">만료일자</th>
                <th style="width: 90px">사용여부</th>
                <th class="editableCell" style="width: 150px">슬롯점유</th>
                <th class="editableCell" style="width: 8%">이력보기</th>
              </tr>
            </thead>
            <tbody>
              <template
                v-if="!DailyUserSlotList || DailyUserSlotList.length === 0"
              >
                <tr>
                  <td colspan="12" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(dailyUserSlot, idx) in sortedData"
                  :key="dailyUserSlot.id"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input
                      v-if="dailyUserSlot.useYn !== 'N'"
                      type="checkbox"
                      v-model="dailyUserSlot.chk"
                    />
                  </td>
                  <td>
                    {{ dailyUserSlot.currUserNm }}
                  </td>
                  <td>
                    {{ formatMblNo(dailyUserSlot.mblNo) }}
                  </td>
                  <td>
                    {{ dailyUserSlot.siteNm }}
                  </td>
                  <td>
                    {{ dailyUserSlot.slotNo }}
                  </td>
                  <td>
                    <BaseSelect
                      v-model="dailyUserSlot.slotStatus"
                      :readonly="true"
                      name="slotStatus"
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
                      v-model="dailyUserSlot.slotType"
                      :readonly="true"
                      name="slotType"
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
                  <td style="text-align: center">
                    <button
                      v-if="dailyUserSlot.slotStatus === '02'"
                      class="btn btn-custom"
                      @click="fnSlotQrCodePopOpen(dailyUserSlot)"
                    >
                      QRCODE
                    </button>
                  </td>
                  <td>
                    {{ dailyUserSlot.expired }}
                  </td>
                  <td style="text-align: center">
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
                      <button
                        class="btn btn-custom"
                        :disabled="dailyUserSlot.useYn === 'N'"
                        @click="fnQrCodePopOpen(linkPolicy)"
                      >
                        점유 유지
                      </button>
                      <button
                        class="btn btn-custom-danger"
                        :disabled="dailyUserSlot.useYn === 'N'"
                        @click="fnQrCodePopOpen(linkPolicy)"
                      >
                        비우기
                      </button>
                    </div>
                  </td>
                  <td style="text-align: center" @click.stop>
                    <button
                      type="button"
                      class="btn-history-icon"
                      title="변경이력"
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
// ================ Imports ================
import {
  ref,
  watch,
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
import AdminQrCreatePop from "./popup/AdminQrCreatePop.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

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
const { sortKey, sortOrder, sortedData, onSort } =
  useTableSort(DailyUserSlotList);
const { colWidths, onResize } = useColumnResize({
  userNm: 200,
  mblNo: 130,
  siteNm: 200,
  slotNo: 100,
});
const systCodeArr = ref({});
const SiteSearchPopOpen = ref(false);

// 조회조건 변수
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const slotType = ref("");
const slotStatus = ref("");
const useYn = ref("");
const currUserNm = ref("");

// 화면 제어 변수
const headChk = ref(false);
const siteDisabled = ref(false);

// 액션 카드 상태
const dayLimitCnt = ref(20);
const activeAccountCount = ref(0);
const dailyUserJoinYn = ref(false);
const joinCd = ref("");

// 포커스 변수
const siteNoFcs = ref(null);

// siteCd 변경 감시: 비면 초기화, 값이 있으면 링크 정책 조회
watch(siteCd, (newVal) => {
  if (!newVal) {
    DailyUserSlotList.value = [];
    dayLimitCnt.value = 0;
    dailyUserJoinYn.value = false;
    joinCd.value = "";
    return;
  }
  fnGetDailyUserLinkPolicies();
});

const fnGetDailyUserLinkPolicies = async () => {
  try {
    const response = await axios.get(
      "/webApi/baim05/daily-user-link-policies",
      {
        params: { siteCd: siteCd.value },
      }
    );

    if (response.status === 200) {
      const data = response.data.dailyUserLinkPolicy ?? {};
      dayLimitCnt.value = data.dayLimitCnt ?? 0;
      dailyUserJoinYn.value =
        data.dailyUserJoinYn === "Y" || data.dailyUserJoinYn === true;
      activeAccountCount.value = data.activeAccountCount ?? 0;
      joinCd.value = data.joinCd ?? "";
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

const fnCopyJoinLink = async () => {
  if (!joinCd.value) {
    await proxy.$alert("발급된 가입 코드가 없습니다.");
    return;
  }
  // 신규 표준 경로는 SafeNote 서비스 프리픽스('/safenote') 하위. 기존 '/dailyUserJoin'은 라우터 alias로 호환.
  const url = `${window.location.origin}/safenote/dailyUserJoin/${joinCd.value}`;
  try {
    await navigator.clipboard.writeText(url);
    await proxy.$alert("링크가 복사되었습니다.\n");
  } catch (err) {
    await proxy.$alert(
      "링크 복사에 실패했습니다: " + (err?.message || "알 수 없는 오류")
    );
  }
};
const fnQrCodePopOpen = () => {
  openPop(QrCodePop, {
    qrValue: JSON.stringify({
      cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
      siteCd: siteCd.value,
      qrTitle: siteNm.value + " - 일일계정 발급 QR코드",
    }),
  });
};
const fnSlotQrCodePopOpen = (slot) => {
  openPop(QrCodePop, {
    qrValue: JSON.stringify({
      cmpnyCd: slot.cmpnyCd,
      siteCd: slot.siteCd,
      userCd: slot.userCd,
      qrTitle: siteNm.value + " - " + slot.currUserNm + " QR코드",
    }),
  });
};
const formatMblNo = (val) => proxy.$util.formatPhoneNumber(val) ?? "";
const fnGenerateAdminQr = () => {
  openPop(AdminQrCreatePop, {
    slotList: DailyUserSlotList.value,
    siteCd: siteCd.value,
    onSaved: ({ cmpnyCd, siteCd: savedSiteCd, userCd, userNm }) => {
      openPop(QrCodePop, {
        qrValue: JSON.stringify({
          cmpnyCd,
          siteCd: savedSiteCd,
          userCd,
          qrTitle: siteNm.value + " - 관리자 발급 QR코드(" + userNm + ")",
        }),
      });
      fnSearch();
    },
  });
};

const fnSave = async () => {
  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/baim05/save-daily-user-link-policies",
      {
        siteCd: siteCd.value,
        dayLimitCnt: dayLimitCnt.value,
        useYn: dailyUserJoinYn.value ? "Y" : "N",
      }
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
const fnBulkOccupy = () => {
  proxy.$alert("준비중입니다.");
};
const fnBulkUnoccupy = () => {
  proxy.$alert("준비중입니다.");
};
const fnBulkClear = () => {
  proxy.$alert("준비중입니다.");
};

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
  fnInit();
  await fnGetSystinfoList();
  await fnSearch();
});

// ================ API Functions ================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
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
      useYn.value = systCodeArr.value.SYS003[1].systValDCd;
      slotType.value = systCodeArr.value.SYS014[0].systValDCd;
      slotStatus.value = systCodeArr.value.SYS015[0].systValDCd;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(
      getMessage(MSG.REQUIRED_FIELD_MISSING, { fieldLabel: "사업장" })
    );
    siteNoFcs.value?.focus();
    return;
  }

  DailyUserSlotList.value = [];

  try {
    const response = await axios.get("/webApi/baim05/daily-user-slot-lists", {
      params: {
        siteCd: siteCd.value,
        slotType: slotType.value,
        slotStatus: slotStatus.value,
        useYn: useYn.value,
        currUserNm: currUserNm.value,
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
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

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

// ================ Methods/Functions ================
const fnButtonControll = () => {
  // localButtons.value.search = "N";
  localButtons.value.create = "N";
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
  DailyUserSlotList.value.forEach((item) => {
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

const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
};
</script>

<style scoped>
.btn-custom-danger {
  padding: 0 var(--btn-padding-sm, 10px);
  min-height: var(--btn-height-sm, 26px);
  border: 1px solid #4b5563;
  color: #4b5563;
  background: transparent;
}

.btn-custom-danger:hover {
  color: #ffffff;
  background: #374151;
  border-color: #374151;
}

/* table.css 의 .data-grid button 전역 border 가 .btn-custom 색을 덮으므로
   테이블 내부에서도 동일한 border 가 유지되도록 specificity 보강 */
.data-grid .btn.btn-custom {
  border-color: var(--color-primary, #16a34a);
}
.data-grid .btn.btn-custom-danger {
  border-color: #4b5563;
}

/* 슬롯점유 컬럼 버튼: dailyUserJoinYn === 'N' 일 때 연한 회색 비활성 상태 */
.btn.btn-custom:disabled,
.btn.btn-custom:disabled:hover,
.btn.btn-custom-danger:disabled,
.btn.btn-custom-danger:disabled:hover,
.data-grid .btn.btn-custom:disabled,
.data-grid .btn.btn-custom:disabled:hover,
.data-grid .btn.btn-custom-danger:disabled,
.data-grid .btn.btn-custom-danger:disabled:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
  color: #9ca3af;
  cursor: not-allowed;
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

/* ── 액션 카드 행 ─────────────────────────────────────────────── */
.action-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
  width: 100%;
  box-sizing: border-box;
}

.action-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  background: var(--color-surface, #ffffff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  min-height: 48px;
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
}

.action-card-head {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.action-card-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary, #16a34a);
}

.action-card-title {
  font-weight: 600;
  font-size: 0.8125rem;
  color: var(--color-text-strong, #111827);
  white-space: nowrap;
}

.action-card-help {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 0.6875rem;
  font-weight: 700;
  cursor: help;
  user-select: none;
}

.action-card-dash {
  color: var(--color-primary, #16a34a);
  font-weight: 700;
}

.action-card-count {
  font-size: 0.8125rem;
  color: var(--color-text, #374151);
  white-space: nowrap;
}

.action-card-body {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.action-card-body .btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* ── 활성화 계정 입력 / 계정 등록 라디오 ──────────────────────── */
.max-count-input {
  width: 48px;
  height: 24px;
  padding: 0 6px;
  margin-left: 4px;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 4px;
  font-size: 0.8125rem;
  color: var(--color-text-strong, #111827);
  background: #ffffff;
  text-align: center;
  font-family: "Pretendard", sans-serif;
}

.max-count-input:focus {
  outline: none;
  border-color: var(--color-primary, #16a34a);
  box-shadow: 0 0 0 2px var(--color-focus-ring);
}

.account-register-group {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-left: 16px;
  padding-left: 16px;
  border-left: 1px solid var(--color-border, #e5e7eb);
}

.account-register-label {
  font-size: 0.8125rem;
  color: var(--color-text, #374151);
  font-weight: 600;
  white-space: nowrap;
}

.radio-group {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.radio-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.8125rem;
  color: var(--color-text, #374151);
  cursor: pointer;
  user-select: none;
}

.radio-item input[type="radio"] {
  margin: 0;
  accent-color: var(--color-primary, #16a34a);
  cursor: pointer;
}
</style>
