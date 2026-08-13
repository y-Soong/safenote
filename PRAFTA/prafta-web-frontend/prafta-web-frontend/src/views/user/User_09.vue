<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 검색바: 사업장 / 소속부서(+하위) / 상태(승인대기·거부) / 사용자정보
         스코프는 서버가 강제한다(사업장 인가 + 부서 관리 권한). 전사 권한이 아니면
         부서를 지정해야 조회된다. -->
    <div class="viewSearch">
      <div>
        <label>사업장</label>
        <input
          id="siteNo"
          type="text"
          v-model="siteNo"
          placeholder="사업장코드"
          @blur="focusKill"
        />
        <button class="search-btn" @click="fnSiteSearchPopOpen()">
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="siteNm"
          type="text"
          v-model="siteNm"
          placeholder="사업장명"
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
          @click="fnSiteNodeSearchPopOpen()"
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
          <input type="checkbox" v-model="incSubNodeYn" :disabled="nodeDisabled" />
          하위부서 조회
        </label>
      </div>

      <div>
        <label>상태</label>
        <select v-model="accountStatus" class="status-select" @change="fnSearch">
          <option value="06">승인 대기</option>
          <option value="07">거부</option>
        </select>
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
            <span class="subtitle-text">셀프가입 신청</span>
          </div>
        </div>

        <p class="guide-text">
          ⓘ 회원가입 신청은 관리자 승인 후 계정이 활성화됩니다. 승인 시 입사일·고용형태·소정근로시간을
          함께 등록하며, 거부한 신청도 동일한 아이디·휴대폰으로 다시 가입할 수 있습니다.
        </p>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="--box-h: 70vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid w-full table-fixed text-sm text-left rtl:text-right">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 4%">No</th>
                <ThSortable
                  label="신청일시"
                  col-key="applyDtime"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.applyDtime"
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
                  label="아이디"
                  col-key="userId"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.userId"
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
                  label="부서"
                  col-key="nodeNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.nodeNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="휴대폰"
                  col-key="mblNo"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.mblNo"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th class="event_cell" style="text-align: center; width: 90px">상태</th>
                <th class="event_cell" style="text-align: center; width: 140px">관리</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="loading">
                <tr>
                  <td colspan="9" class="edu-grid-empty">조회 중입니다...</td>
                </tr>
              </template>
              <template v-else-if="!selfJoinList || selfJoinList.length === 0">
                <tr>
                  <td colspan="9" class="edu-grid-empty">
                    {{
                      accountStatus === "06"
                        ? "승인 대기 중인 가입 신청이 없습니다."
                        : "거부한 가입 신청이 없습니다."
                    }}
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(row, idx) in sortedData" :key="row.userCd">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ row.applyDtime }}</td>
                  <td>{{ row.userNm }}</td>
                  <td>{{ row.userId }}</td>
                  <td>{{ row.siteNm }}</td>
                  <td>{{ row.nodeNm }}</td>
                  <td>{{ row.mblNo }}</td>
                  <td style="text-align: center">
                    <span
                      class="status-badge"
                      :class="row.accountStatus === '06' ? 'is-pending' : 'is-rejected'"
                    >
                      {{ row.accountStatus === "06" ? "승인 대기" : "거부" }}
                    </span>
                  </td>
                  <td style="text-align: center">
                    <div class="row-btn-area" v-if="canSave && row.accountStatus === '06'">
                      <button class="btn btn-sm btn-primary" @click="fnOpenApprovePop(row)">
                        승인
                      </button>
                      <button class="btn btn-sm btn-second" @click="fnReject(row)">
                        거부
                      </button>
                    </div>
                    <span v-else>-</span>
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
  computed,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useModal } from "@/utils/useModal";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import ReasonInputModal from "@/components/modal/ReasonInputModal.vue";
import SelfJoinApprovePop from "./popup/SelfJoinApprovePop.vue";
import { useTableSort, useColumnResize } from "@/composables/useTableFeatures.js";

// =========================== Define ===========================
defineOptions({ name: "User_09" });
const props = defineProps({
  title: String,
  buttons: Object,
});

// =========================== Ref ===========================
const localButtons = ref({ ...props.buttons });
const selfJoinList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(selfJoinList);
const { colWidths, onResize } = useColumnResize({
  applyDtime: 150,
  userNm: 100,
  userId: 120,
  siteNm: 140,
  nodeNm: 120,
  mblNo: 130,
});

// 조회조건
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true);
const incSubNodeYn = ref(false);
const accountStatus = ref("06");
const userKeyword = ref("");
const loading = ref(false);

// 승인/거부 버튼 노출 — 메뉴 버튼 권한(BTN_SAVE). 실제 인가는 서버가 강제한다(부서 관리 권한).
const canSave = computed(() => localButtons.value?.save === "Y");

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { open: openPop, close: closePop } = useModal();

// =========================== Life Cycle ===========================
const getSession = (key) => {
  const v = sessionStorage.getItem(key);
  return v && v !== "null" && v !== "undefined" ? v : "";
};

// 세션 사업장/부서 프리필 — 비전사 권한자는 서버 게이트가 부서 지정을 요구하므로
//   본인 소속을 기본값으로 채워 첫 조회가 바로 성공하게 한다(Attd_16 동일 패턴).
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
  if (siteCd.value) {
    await fnSearch();
  }
});

// =========================== Methods ===========================
// 목록 조회 — GET /webApi/user09/self-join-lists.
//   cmpnyCd/권한은 서버 JWT 클레임 사용(파라미터 전달 금지).
const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(getMessage(MSG.SITE_REQUIRED));
    return;
  }

  loading.value = true;
  selfJoinList.value = [];

  try {
    const response = await axios.get("/webApi/user09/self-join-lists", {
      params: {
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        accountStatus: accountStatus.value,
        userKeyword: userKeyword.value,
      },
    });

    if (response.status === 200) {
      selfJoinList.value = response.data?.selfJoinList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    loading.value = false;
  }
};

// 승인 시트 오픈 — 승인 확정은 팝업이 수행하고, 성공 시 목록을 재조회한다.
const fnOpenApprovePop = (row) => {
  openPop(SelfJoinApprovePop, {
    userCd_p: row.userCd,
    userId_p: row.userId,
    userNm_p: row.userNm,
    // 통상 기준값(풀타임 라벨)은 대상자 소속 사업장 기준이다.
    siteCd_p: row.siteCd,
    siteNm_p: row.siteNm,
    nodeNm_p: row.nodeNm,
    onSaved: fnSearch,
  });
};

// 거부 — 사유 입력(필수) 후 POST /webApi/user09/self-join-reject.
//   사유는 감사 로그에만 남는다(계정 행에는 저장하지 않음 — 재가입 시 행이 재활용되므로).
const fnReject = (row) => {
  openPop(ReasonInputModal, {
    title: "가입 거부 사유 입력",
    message: `${row.userNm}(${row.userId}) 님의 가입 신청을 거부합니다.\n사유는 감사 기록으로 보관됩니다.`,
    placeholder: "거부 사유를 입력해 주세요. (200자 이내)",
    maxLength: 200,
    required: true,
    onConfirm: async (reason) => {
      closePop();
      await fnRejectSubmit(row, reason);
    },
    onCancel: () => {
      closePop();
    },
  });
};

const fnRejectSubmit = async (row, reason) => {
  try {
    const response = await axios.post("/webApi/user09/self-join-reject", {
      userCd: row.userCd,
      rejectReason: reason,
    });

    if (response.status === 200) {
      await proxy.$alert("가입 신청을 거부했습니다.");
      await fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "거부 처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 사업장/부서 검색 팝업 (User_01 조회조건 패턴 동일)
const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal ?? "";
  siteNo.value = siteNoVal ?? "";
  siteNm.value = siteNmVal ?? "";
  // 사업장이 바뀌면 이전 부서 조건은 무효(부서코드는 사업장별로 중복 사용된다).
  nodeCd.value = "";
  nodeNm.value = "";
  nodeDisabled.value = proxy.$util.isEmpty(siteCd.value);
};

const fnSiteNodeSearchPopOpen = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
    return;
  }
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    nodeCd_p: "",
    userId_p: "",
    onSelect: onSiteNodeSelected,
  });
};

const onSiteNodeSelected = (nodeCdVal, nodeNmVal) => {
  nodeCd.value = nodeCdVal ?? "";
  nodeNm.value = nodeNmVal ?? "";
};

// 코드/명 입력을 직접 지웠을 때 짝 값도 정리한다(검색 팝업 선택값과 어긋나지 않게).
const focusKill = (e) => {
  const id = e?.target?.id;
  if (id === "siteNo" || id === "siteNm") {
    if (proxy.$util.isEmpty(siteNo.value) && proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      nodeCd.value = "";
      nodeNm.value = "";
      nodeDisabled.value = true;
    }
  } else if (id === "nodeCd" || id === "nodeNm") {
    if (proxy.$util.isEmpty(nodeCd.value)) {
      nodeNm.value = "";
    }
  }
};
</script>

<style scoped>
/* 검색바 좌측 정렬(User_01/User_06 패턴) */
.viewSearch {
  padding-left: calc(0.5rem + var(--space-md, 0.75rem));
  row-gap: 0.5rem;
}
.viewSearch > div:first-child {
  margin-left: 0;
}

.status-select {
  width: 120px;
}

/* 하위부서 조회 체크박스 — Attd_07 규격과 동일(검색바 표기 통일).
   스타일이 없으면 검색바의 일반 input 규칙이 적용돼 체크박스가 과도하게 커진다. */
.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.85rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  user-select: none;
  /* 앞 항목(소속부서) 쪽으로 당겨 붙인다(Attd_07 규격) */
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

/* 안내문 — 팝업 안내(reg-guide)와 동일 톤 */
.guide-text {
  margin: 0 0 0.5rem 0;
  padding: 0.5rem 0.75rem;
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.5;
  color: var(--color-text-muted, #4b5563);
  background: var(--color-surface-muted, #f3f4f6);
  border-radius: var(--btn-radius, 8px);
}

/* 상태 배지 */
.status-badge {
  display: inline-block;
  padding: 0.1rem 0.5rem;
  border-radius: var(--btn-radius, 8px);
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.4;
}
.status-badge.is-pending {
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
}
.status-badge.is-rejected {
  background: var(--color-border, #e5e7eb);
  color: var(--color-text-muted, #4b5563);
}

/* 행 관리 버튼 */
.row-btn-area {
  display: flex;
  gap: 0.25rem;
  justify-content: center;
}
</style>
