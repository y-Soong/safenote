<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @create="fnOpenRegPop"
    />

    <!-- 검색바: 전화번호 / 사용여부(전체·사용·해제)
         스코프는 회사(gv_cmpnyCd) — 사업장 선택 없음(블랙리스트는 회사 단위). -->
    <div class="viewSearch">
      <div>
        <label>전화번호</label>
        <input
          v-model.trim="mblNo"
          type="text"
          placeholder="휴대폰번호"
          style="width: 160px"
          @keyup.enter="fnSearch"
        />
      </div>

      <div>
        <label>사용여부</label>
        <select v-model="useYn" class="use-yn-select">
          <option value="">전체</option>
          <option value="Y">사용</option>
          <option value="N">해제</option>
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
            <span class="subtitle-text">일일계정 블랙리스트</span>
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
                <th class="event_cell" style="text-align: center; width: 4%">
                  No
                </th>
                <ThSortable
                  label="휴대폰번호"
                  col-key="mblNo"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.mblNo"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="사유"
                  col-key="reason"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.reason"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="사용여부"
                  col-key="useYn"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.useYn"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="등록자"
                  col-key="insertNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.insertNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="등록일시"
                  col-key="insertDate"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.insertDate"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th class="event_cell" style="text-align: center; width: 90px">
                  관리
                </th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!blacklist || blacklist.length === 0">
                <tr>
                  <td colspan="7" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(row, idx) in sortedData" :key="row.blacklistId">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ row.mblNo }}</td>
                  <td class="reason-cell">{{ row.reason }}</td>
                  <td style="text-align: center">
                    <span
                      class="status-badge"
                      :class="row.useYn === 'Y' ? 'is-active' : 'is-released'"
                    >
                      {{ row.useYn === "Y" ? "사용" : "해제" }}
                    </span>
                  </td>
                  <td>{{ row.insertNm }}</td>
                  <td>{{ row.insertDate }}</td>
                  <td style="text-align: center">
                    <button
                      v-if="canDelete && row.useYn === 'Y'"
                      class="btn btn-sm btn-primary"
                      @click="fnRelease(row)"
                    >
                      해제
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
import { resolveApiErrorMessage } from "@/utils/apiError";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import DailyBlacklistRegPop from "@/views/user/popup/DailyBlacklistRegPop.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

// =========================== Define ===========================
defineOptions({ name: "User_06" });
const props = defineProps({
  title: String,
  buttons: Object,
});

// =========================== Ref ===========================
const localButtons = ref({ ...props.buttons });
const blacklist = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(blacklist);
const { colWidths, onResize } = useColumnResize({
  mblNo: 150,
  reason: 280,
  useYn: 90,
  insertNm: 120,
  insertDate: 170,
});

// 조회조건
const mblNo = ref("");
const useYn = ref("");

// 삭제(해제) 권한 — 메뉴 버튼 권한(BTN_DELT)으로 노출 제어.
const canDelete = computed(() => localButtons.value?.delete === "Y");

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// =========================== Life Cycle ===========================
onMounted(async () => {
  await fnSearch();
});

// =========================== Methods ===========================
// 목록 조회 — GET /webApi/user06/blacklist-lists.
//   파라미터: { mblNo, useYn } (cmpnyCd 는 서버 JWT 클레임 사용 — body 전달 안 함).
//   휴대폰은 서버에서 마스킹(010-****-1234)되어 내려온다.
const fnSearch = async () => {
  blacklist.value = [];

  try {
    const response = await axios.get("/webApi/user06/blacklist-lists", {
      params: {
        mblNo: mblNo.value,
        useYn: useYn.value,
      },
    });

    if (response.status === 200) {
      blacklist.value = response.data?.blacklist || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 등록 팝업 오픈 — 저장 성공 시 onSaved 콜백으로 목록 갱신.
const fnOpenRegPop = () => {
  openPop(DailyBlacklistRegPop, {
    onSaved: fnSearch,
  });
};

// 해제 — POST /webApi/user06/blacklist-release. 확인 후 진행, 성공 시 목록 재조회.
//   body 는 { blacklistId } 만(cmpnyCd 는 서버 JWT).
const fnRelease = async (row) => {
  const ok = await proxy.$confirm("블랙리스트를 해제하시겠습니까?");
  if (!ok) return;

  try {
    const response = await axios.post("/webApi/user06/blacklist-release", {
      blacklistId: row.blacklistId,
    });

    if (response.status === 200) {
      await proxy.$alert("해제되었습니다.");
      await fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "해제 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};
</script>

<style scoped>
/* 검색바 좌측 정렬(User_05/User_01 패턴) */
.viewSearch {
  padding-left: calc(0.5rem + var(--space-md, 0.75rem));
  row-gap: 0.5rem;
}
.viewSearch > div:first-child {
  margin-left: 0;
}

.use-yn-select {
  width: 120px;
}

/* 사유 셀 — 길면 줄바꿈 허용 */
.reason-cell {
  white-space: pre-wrap;
  word-break: break-word;
}

/* 사용여부 배지 */
.status-badge {
  display: inline-block;
  padding: 0.1rem 0.5rem;
  border-radius: var(--btn-radius, 8px);
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.4;
}
.status-badge.is-active {
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
}
.status-badge.is-released {
  background: var(--color-border, #e5e7eb);
  color: var(--color-text-muted, #4b5563);
}
</style>
