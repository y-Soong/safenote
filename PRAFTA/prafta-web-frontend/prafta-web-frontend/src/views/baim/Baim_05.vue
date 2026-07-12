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
        <label>사용자명</label>
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
            <button class="btn btn-primary btn-sm" @click="fnSavePolicy">
              저장
            </button>
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
              :disabled="!siteCd || !dailyUserJoinYn"
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
              :disabled="!siteCd || !dailyUserJoinYn"
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
              :disabled="!siteCd || !dailyUserJoinYn"
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
            <button class="btn btn-custom" @click="fnBulkNodeAssign">
              소속부서 일괄지정
            </button>
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
                  label="사용자"
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
                <th style="width: 160px">소속부서</th>
                <th style="width: 130px">구분</th>
                <th style="width: 100px">상태</th>
                <th style="width: 90px">QRCODE</th>
                <th style="width: 120px">만료일자</th>
                <th style="width: 90px">사용여부</th>
                <th class="editableCell" style="width: 150px">슬롯점유</th>
                <th class="editableCell" style="width: 90px">이력보기</th>
              </tr>
            </thead>
            <tbody>
              <template
                v-if="!DailyUserSlotList || DailyUserSlotList.length === 0"
              >
                <tr>
                  <td colspan="13" class="edu-grid-empty">
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
                    <div class="flex items-center gap-2 w-full">
                      <span class="truncate min-w-0">{{
                        dailyUserSlot.nodeNm
                      }}</span>
                      <button
                        class="ml-auto border rounded node-assign-btn"
                        :disabled="dailyUserSlot.useYn === 'N'"
                        @click="fnSlotNodeSearchPopOpen(dailyUserSlot)"
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
                      :readonly="isSlotTypeLocked(dailyUserSlot)"
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
                        :disabled="
                          dailyUserSlot.useYn === 'N' ||
                          dailyUserSlot.slotStatus !== '02'
                        "
                        @click="fnFixSlot(dailyUserSlot)"
                      >
                        점유 유지
                      </button>
                      <button
                        class="btn btn-custom"
                        :disabled="
                          dailyUserSlot.useYn === 'N' ||
                          dailyUserSlot.slotStatus !== '02'
                        "
                        @click="fnUnfixSlot(dailyUserSlot)"
                      >
                        점유 해지
                      </button>
                      <button
                        class="btn btn-custom-danger"
                        :disabled="dailyUserSlot.useYn === 'N'"
                        @click="fnClearSlot(dailyUserSlot)"
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
                      @click="fnSlotHistory(dailyUserSlot)"
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
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import QrCodePop from "@/components/popup/QrCodePop.vue";
import AdminQrCreatePop from "./popup/AdminQrCreatePop.vue";
import SlotHistoryPop from "./popup/SlotHistoryPop.vue";
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
  userNm: 100,
  mblNo: 130,
  siteNm: 130,
  slotNo: 115,
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
      // T1-07: 계정 등록 토글은 BE 응답 useYn('Y'/'N') 기준. (구 dailyUserJoinYn 필드는 BE 미반환)
      dailyUserJoinYn.value = data.useYn === "Y" || data.useYn === true;
      // T1-06: 활성화 계정 = 현재 점유 중 슬롯 수(BE selectDailyUserLinkPolicyList 에서 집계).
      activeAccountCount.value = data.activeAccountCount ?? 0;
      joinCd.value = data.joinCd ?? "";
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 직접가입 링크/QR 의 공개 base URL 해석.
//   QR/링크는 외부(모바일)에서 접근하므로 관리자 화면 주소(window.location.origin, 예:
//   http://localhost:8081)가 아니라 공개 도메인이어야 한다. Cloudflare 고정 도메인을
//   VITE_PUBLIC_JOIN_BASE_URL(예: https://web.prafta.com)로 주입받아 우선 사용하고,
//   미설정 시에만 현재 origin 으로 폴백한다(순수 로컬 테스트 대비). 끝 슬래시는 제거.
const resolveJoinBaseUrl = () => {
  const base = import.meta.env.VITE_PUBLIC_JOIN_BASE_URL;
  return (base ? String(base) : window.location.origin).replace(/\/+$/, "");
};

const fnCopyJoinLink = async () => {
  if (!joinCd.value) {
    await proxy.$alert("발급된 가입 코드가 없습니다.");
    return;
  }
  // 신규 표준 경로는 SafeNote 서비스 프리픽스('/safenote') 하위. 기존 '/dailyUserJoin'은 라우터 alias로 호환.
  const url = `${resolveJoinBaseUrl()}/safenote/dailyUserJoin/${joinCd.value}`;
  try {
    await navigator.clipboard.writeText(url);
    await proxy.$alert("링크가 복사되었습니다.\n");
  } catch (err) {
    await proxy.$alert(
      "링크 복사에 실패했습니다: " + (err?.message || "알 수 없는 오류")
    );
  }
};
const fnQrCodePopOpen = async () => {
  // T1-03: 직접가입 QR 은 가입 URL 을 인코딩한다(카메라 스캔 시 가입 페이지로 진입).
  //        슬롯 행 QR/관리자 발급 QR(출퇴근/식별용 JSON)과 달리, 직접가입은 링크복사와 동일한 URL 을 사용.
  if (!joinCd.value) {
    await proxy.$alert("발급된 가입 코드가 없습니다.");
    return;
  }
  const url = `${resolveJoinBaseUrl()}/safenote/dailyUserJoin/${joinCd.value}`;
  openPop(QrCodePop, {
    qrValue: url,
    title: siteNm.value + " - 일일계정 발급 QR코드",
  });
};
const fnSlotQrCodePopOpen = (slot) => {
  openPop(QrCodePop, {
    qrValue: JSON.stringify({
      cmpnyCd: slot.cmpnyCd,
      siteCd: slot.siteCd,
      // 슬롯 resultMap(DailyUserInfoMap) 의 사용자코드 프로퍼티는 currUserId 다(이름은 currUserNm).
      // 과거 slot.userCd(존재하지 않는 필드)로 읽어 QR JSON 에서 userCd 가 누락 → 현장 처리(site-ops)
      // 스캔 시 ATTD_400_171 로 거부되던 결함 수정.
      userCd: slot.currUserId,
      qrTitle: siteNm.value + " - " + slot.currUserNm + " QR코드",
    }),
  });
};
const formatMblNo = (val) => proxy.$util.formatPhoneNumber(val) ?? "";
const fnGenerateAdminQr = () => {
  // T1-07: 계정 등록 OFF 면 발급 차단 안내(서버에서도 BAIM_400_004 로 강제 차단). 버튼 비활성과 이중 가드.
  if (!dailyUserJoinYn.value) {
    proxy.$alert(
      "계정 등록이 OFF 상태입니다.\n계정 등록을 ON 으로 변경 후 발급하세요."
    );
    return;
  }
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
const fnSlotHistory = (slot) => {
  openPop(SlotHistoryPop, {
    siteCd: slot.siteCd,
    siteNm: slot.siteNm,
    slotNo: slot.slotNo,
  });
};

// 활성화 계정 카드 저장: 일일계정 링크정책(활성화 계정 한도 dayLimitCnt + 계정 등록 ON/OFF) 저장.
//   계정등록 옆 "저장" 버튼 전용. 공통 헤더 저장(fnSave)과 동작이 다르므로 분리한다.
const fnSavePolicy = async () => {
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

// 통합 저장: 체크된 슬롯 행의 소속부서(nodeCd)/구분(slotType) 변경을 저장한다.
//   - 체크된 행이 없으면 안내 후 중단.
//   - 구분 저장은 점유중(slotStatus='02') 행을 제외(점유중 행은 변경 불가, 서버도 fail-closed).
//   - 소속부서 저장은 체크된 행 전체(빈값=부서 해제 포함).
//   두 API의 권한·점유 검증은 백엔드가 그대로 수행한다(프론트는 호출만 통합).
const fnSave = async () => {
  const checked = collectCheckedSlots();
  if (checked.length === 0) {
    await proxy.$alert("선택된 항목이 없습니다.");
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  // 구분 저장 대상(점유중 행 제외)
  const typeRows = checked.filter((s) => s.slotStatus !== "02");
  // 소속부서 저장 대상(체크된 행 전체)
  const nodeRows = checked;

  // 부분 실패 시 어떤 저장이 실패했는지 안내하기 위한 실패 라벨 수집
  const failedLabels = [];

  // 1) 구분 저장
  if (typeRows.length > 0) {
    try {
      await axios.post("/webApi/baim05/set-daily-user-slot-type", {
        slots: typeRows.map((r) => ({
          siteCd: r.siteCd,
          slotNo: r.slotNo,
          slotType: r.slotType,
        })),
      });
    } catch (err) {
      const msg = resolveApiErrorMessage(
        err,
        "구분 저장 중 오류가 발생했습니다."
      );
      failedLabels.push(`구분 저장 실패: ${msg}`);
    }
  }

  // 2) 소속부서 저장
  if (nodeRows.length > 0) {
    try {
      await axios.post("/webApi/baim05/set-daily-user-slot-node", {
        slots: nodeRows.map((r) => ({
          siteCd: r.siteCd,
          slotNo: r.slotNo,
          nodeCd: r.nodeCd ?? "",
        })),
      });
    } catch (err) {
      const msg = resolveApiErrorMessage(
        err,
        "부서 저장 중 오류가 발생했습니다."
      );
      failedLabels.push(`부서 저장 실패: ${msg}`);
    }
  }

  // 결과 안내: 전부 성공이면 성공 메시지, 일부/전부 실패면 실패 항목 안내
  if (failedLabels.length === 0) {
    await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
  } else {
    await proxy.$alert(failedLabels.join("\n"));
  }

  // 성공/부분실패 무관하게 서버 최신 상태로 1회 재조회(스냅샷 재설정)
  await fnSearch();
};
// 슬롯 식별자 추출(서버는 siteCd/slotNo 만 신뢰, 점유자 식별은 서버 재조회)
const toSlotItem = (slot) => ({ siteCd: slot.siteCd, slotNo: slot.slotNo });

// 체크된(chk=true) 행 수집
const collectCheckedSlots = () =>
  (DailyUserSlotList.value || []).filter((s) => s.chk === true);

// 슬롯 고정여부 토글 공통 호출 (fixedYn: 'Y'=점유 유지, 'N'=점유 해지)
const requestSetFixed = async (slots, fixedYn) => {
  try {
    const response = await axios.post(
      "/webApi/baim05/set-daily-user-slot-fixed",
      {
        slots: slots.map(toSlotItem),
        fixedYn,
      }
    );

    if (response.status === 200) {
      await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      await fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 슬롯 비우기 공통 호출 (점유 계정 비활성 + 슬롯 초기화, 위험 동작)
const requestClear = async (slots) => {
  try {
    const response = await axios.post("/webApi/baim05/clear-daily-user-slots", {
      slots: slots.map(toSlotItem),
    });

    if (response.status === 200) {
      await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      await fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 구분(slotType) 편집 잠금 조건: 점유중(slotStatus='02') 또는 비사용(useYn='N') 슬롯은 변경 불가.
//   서버도 fail-closed 로 점유 슬롯 변경을 거부한다(프론트 잠금 + 서버 가드 이중 방어).
const isSlotTypeLocked = (slot) =>
  slot.slotStatus === "02" || slot.useYn === "N";

// ── 슬롯 소속부서(NODE_CD) 지정/해제 ──────────────────────────────
// 행 단위 부서 검색 팝업 열기. 선택 시 해당 행의 nodeCd/nodeNm 을 갱신(저장은 '부서 저장' 일괄).
const fnSlotNodeSearchPopOpen = (slot) => {
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: slot.siteCd,
    nodeCd_p: "",
    userId_p: slot.slotNo, // 식별 키로 slotNo 사용(콜백에서 대상 행 매칭)
    onSelect: onSlotNodeSelected,
  });
};

const onSlotNodeSelected = (slotNoVal, nodeCdVal, nodeNmVal) => {
  const target = DailyUserSlotList.value.find((s) => s.slotNo === slotNoVal);
  if (target) {
    target.nodeCd = nodeCdVal ?? "";
    target.nodeNm = nodeNmVal ?? "";
  }
};

// 소속부서 일괄지정: 체크된 행에 동일 부서를 지정(사업장이 다른 행은 부서 검색 사업장 기준으로 거름).
const fnBulkNodeAssign = () => {
  const slots = collectCheckedSlots();
  if (slots.length === 0) {
    proxy.$alert("선택된 항목이 없습니다.");
    return;
  }
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
    return;
  }
  // 부서는 사업장에 종속되므로, 선택 행이 모두 조회 사업장(siteCd)에 속해야 일괄지정 가능.
  if (slots.some((s) => s.siteCd !== siteCd.value)) {
    proxy.$alert("같은 사업장의 슬롯만 일괄 부서지정할 수 있습니다.");
    return;
  }
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    nodeCd_p: "",
    onSelect: onBulkNodeAssignSelected,
  });
};

const onBulkNodeAssignSelected = (nodeCdVal, nodeNmVal) => {
  const targets = collectCheckedSlots().filter(
    (s) => s.siteCd === siteCd.value
  );
  targets.forEach((target) => {
    target.nodeCd = nodeCdVal ?? "";
    target.nodeNm = nodeNmVal ?? "";
  });
};

// 행 단위 - 점유 유지 (FIXED_YN='Y')
const fnFixSlot = async (slot) => {
  const ok = await proxy.$confirm(
    "해당 슬롯의 점유를 유지하시겠습니까?\n자정 만료 배치에서 점유자가 만료되지 않습니다."
  );
  if (!ok) return;
  await requestSetFixed([slot], "Y");
};

// 행 단위 - 점유 해지 (FIXED_YN='N'). T1-02: 단일 슬롯 점유 해지 기능 신설(기존 BE 재사용).
//   점유 유지 해제 → 다음 자정 만료 배치에서 점유자가 만료 대상이 된다. 계정은 변경하지 않음.
const fnUnfixSlot = async (slot) => {
  const ok = await proxy.$confirm(
    "해당 슬롯의 점유 유지를 해지하시겠습니까?\n다음 자정 만료 배치에서 점유자가 만료될 수 있습니다."
  );
  if (!ok) return;
  await requestSetFixed([slot], "N");
};

// 행 단위 - 비우기 (위험 동작)
const fnClearSlot = async (slot) => {
  const ok = await proxy.$confirm(
    "해당 슬롯을 비우시겠습니까?\n점유 중인 계정이 비활성화되며 되돌릴 수 없습니다."
  );
  if (!ok) return;
  await requestClear([slot]);
};

// 일괄 - 점유 유지
const fnBulkOccupy = async () => {
  const slots = collectCheckedSlots();
  if (slots.length === 0) {
    await proxy.$alert("선택된 항목이 없습니다.");
    return;
  }
  const ok = await proxy.$confirm(
    `선택한 ${slots.length}건의 슬롯 점유를 유지하시겠습니까?`
  );
  if (!ok) return;
  await requestSetFixed(slots, "Y");
};

// 일괄 - 점유 해지
const fnBulkUnoccupy = async () => {
  const slots = collectCheckedSlots();
  if (slots.length === 0) {
    await proxy.$alert("선택된 항목이 없습니다.");
    return;
  }
  const ok = await proxy.$confirm(
    `선택한 ${slots.length}건의 슬롯 점유를 해지하시겠습니까?`
  );
  if (!ok) return;
  await requestSetFixed(slots, "N");
};

// 일괄 - 비우기 (위험 동작)
const fnBulkClear = async () => {
  const slots = collectCheckedSlots();
  if (slots.length === 0) {
    await proxy.$alert("선택된 항목이 없습니다.");
    return;
  }
  const ok = await proxy.$confirm(
    `선택한 ${slots.length}건의 슬롯을 비우시겠습니까?\n점유 중인 계정이 비활성화되며 되돌릴 수 없습니다.`
  );
  if (!ok) return;
  await requestClear(slots);
};

// ================ Watchers ================
// 소속부서(nodeCd/nodeNm)·상태(slotStatus)·구분(slotType) 값이 바뀌면 해당 행을 자동 체크.
//   (chk 자기재귀 방지 및 원본 스냅샷 _orig* 은 감시 제외)
useFieldWatcher(
  DailyUserSlotList,
  (item) => {
    item.chk = true;
  },
  ["chk", "_origSlotType", "_origNodeCd"]
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

  // 조회 버튼으로 활성화 계정 카드(한도 dayLimitCnt/계정등록 토글/활성 계정 수)도 함께 최신화.
  //   (기존엔 watch(siteCd) 로만 갱신돼 사업장 미변경 조회 시 카드가 갱신되지 않던 결함 수정)
  await fnGetDailyUserLinkPolicies();

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
      // 구분(slotType)/소속부서(nodeCd) 변경 감지를 위해 행마다 원본값 스냅샷 저장(dirty 판정 기준).
      DailyUserSlotList.value.forEach((item) => {
        item._origSlotType = item.slotType;
        item._origNodeCd = item.nodeCd ?? "";
      });
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
  // 공통 헤더 저장 버튼 활성화(@save -> fnSave: 일일계정 링크정책 저장)
  localButtons.value.save = "Y";
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

/* 소속부서 지정 행 검색 버튼 (CSS 변수 기반, 하드코딩 금지) */
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
