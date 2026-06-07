<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @create="fnOpenCreatePop"
    />

    <!-- 검색바: 사업장 / 소속부서 / 제목 / 팝업여부 / 고정여부 / 등록월 -->
    <div class="viewSearch">
      <!-- 049-01: 사업장 / 소속부서(노드) 조회조건 (코드/돋보기/명칭, 조회팝업 선택 전용) -->
      <div>
        <label>사업장</label>
        <input
          id="noticeSiteCd"
          type="text"
          v-model="siteCd"
          placeholder="사업장코드"
          readonly
          @click="fnOpenSiteSearch"
        />
        <button class="search-btn" @click="fnOpenSiteSearch">
          <img class="search_icon" :src="search_icon" alt="사업장 조회" />
        </button>
        <input
          id="noticeSiteNm"
          type="text"
          v-model="siteNm"
          placeholder="사업장명"
          readonly
          @click="fnOpenSiteSearch"
        />
      </div>

      <div>
        <label>소속부서</label>
        <input
          id="noticeNodeCd"
          type="text"
          v-model="nodeCd"
          placeholder="부서코드"
          :disabled="!siteCd"
          readonly
          @click="fnOpenNodeSearch"
        />
        <button class="search-btn" :disabled="!siteCd" @click="fnOpenNodeSearch">
          <img class="search_icon" :src="search_icon" alt="부서 조회" />
        </button>
        <input
          id="noticeNodeNm"
          type="text"
          v-model="nodeNm"
          placeholder="부서명"
          :disabled="!siteCd"
          readonly
          @click="fnOpenNodeSearch"
        />
      </div>

      <div>
        <label>등록월</label>
        <CalendarSrch v-model="registMonth" month />
      </div>

      <div>
        <label>팝업여부</label>
        <select v-model="popupYn" name="combo">
          <option value="">전체</option>
          <option value="Y">팝업 ON</option>
          <option value="N">팝업 OFF</option>
        </select>
      </div>

      <div>
        <label>고정여부</label>
        <select v-model="pinYn" name="combo">
          <option value="">전체</option>
          <option value="Y">고정</option>
          <option value="N">일반</option>
        </select>
      </div>

      <div>
        <label>제목/내용</label>
        <input
          id="titleKeyword"
          type="text"
          v-model="titleKeyword"
          placeholder="제목 또는 내용 키워드"
          style="width: 540px"
        />
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
          <span class="subtitle-text">공지 리스트</span>
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
                <ThSortable
                  label="상태"
                  col-key="ackStatusNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.ackStatusNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th style="text-align: center; width: 6%">팝업여부</th>
                <th style="text-align: center; width: 6%">고정여부</th>
                <ThSortable
                  label="제목"
                  col-key="title"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.title"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th>내용</th>
                <th style="text-align: center; width: 5%">첨부</th>
                <ThSortable
                  label="대상"
                  col-key="targetSummary"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.targetSummary"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="등록자"
                  col-key="insertUserNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.insertUserNm"
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
              </tr>
            </thead>
            <tbody>
              <template v-if="!noticeResultList || noticeResultList.length === 0">
                <tr>
                  <td colspan="10" class="edu-grid-empty">
                    등록된 공지가 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(item, idx) in sortedData"
                  :key="item.noticeId"
                  class="notice-row"
                  :class="{ 'notice-row--pinned': item.pinYn === 'Y' }"
                  @dblclick="fnOpenInfoPop(item)"
                  style="cursor: pointer"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td style="text-align: center">
                    <!-- 내 읽음 상태: 미확인(안 읽음 or 읽은 뒤 수정됨) / 확인 -->
                    <span
                      class="notice-badge"
                      :class="
                        item.ackStatusNm === '미확인'
                          ? 'notice-badge--unread'
                          : 'notice-badge--read'
                      "
                      >{{ item.ackStatusNm }}</span
                    >
                  </td>
                  <td style="text-align: center">
                    {{ item.popupYn === "Y" ? "ON" : "OFF" }}
                  </td>
                  <td style="text-align: center">
                    {{ item.pinYn === "Y" ? "고정" : "일반" }}
                  </td>
                  <td class="notice-title">{{ item.title }}</td>
                  <td class="notice-content-ellipsis">{{ fnContentPreview(item.content) }}</td>
                  <td style="text-align: center">
                    <!-- 049-02: 첨부 아이콘 → 개수 표시(0건은 -) -->
                    <span
                      v-if="item.fileCnt > 0"
                      class="file-cnt"
                      :aria-label="`첨부 ${item.fileCnt}건`"
                    >
                      {{ item.fileCnt }}
                    </span>
                    <span v-else class="file-cnt file-cnt--none">-</span>
                  </td>
                  <td>{{ item.targetSummary }}</td>
                  <td>{{ item.insertUserNm }}</td>
                  <td>{{ item.insertDate }}</td>
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
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import ViewHeader from "@/components/common/ViewHeader.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import { useTableSort, useColumnResize } from "@/composables/useTableFeatures.js";
import NoticeCreatePop from "./popup/NoticeCreatePop.vue";
import NoticeInfoPop from "./popup/NoticeInfoPop.vue";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import search_icon from "@/assets/img/search_icon.png";

defineOptions({ name: "Notice_01" });
const props = defineProps({
  title: String,
  buttons: Object,
});

const localButtons = ref({ ...props.buttons });
const { open: openPop } = useModal();
const { proxy } = getCurrentInstance();

// 목록/정렬 상태
const noticeResultList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } =
  useTableSort(noticeResultList);
const { colWidths, onResize } = useColumnResize({
  ackStatusNm: 80,
  title: 240,
  targetSummary: 160,
  insertUserNm: 100,
  insertDate: 150,
});

// 검색 조건 (식별자 cmpnyCd/userCd는 서버 JWT 도출 → 전송하지 않음)
const titleKeyword = ref("");
const popupYn = ref("");
const pinYn = ref("");
// 등록월(YYYY-MM). 조회 시 해당 월 1일~말일로 변환하여 서버 startDate/endDate 계약에 매핑.
const registMonth = ref("");

// 049-01: 사업장/소속부서(노드) 검색 조건. cmpnyCd 는 서버 JWT 도출 → 전송하지 않음.
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const nodeCd = ref("");
const nodeNm = ref("");

// 사업장 조회 팝업 → 선택 시 코드/명칭 세팅(사업장 변경 시 노드 초기화)
const fnOpenSiteSearch = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    // SiteSearchPop onSelect 인자 순서 = (siteCd, siteNo, siteNm)
    onSelect: (siteCdVal, siteNoVal, siteNmVal) => {
      siteCd.value = siteCdVal ?? "";
      siteNo.value = siteNoVal ?? "";
      siteNm.value = siteNmVal ?? "";
      // 사업장 변경 시 노드 초기화(노드는 사업장 종속)
      nodeCd.value = "";
      nodeNm.value = "";
    },
  });
};

// 노드 조회 팝업 (사업장 선택 후에만 활성화)
const fnOpenNodeSearch = () => {
  if (!siteCd.value) {
    return proxy.$alert("사업장을 먼저 조회하여 선택해 주세요.");
  }
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    nodeCd_p: "",
    userId_p: "",
    // SiteNodeSearchPop onSelect 인자 순서 = (nodeCd, nodeNm)
    onSelect: (nodeCdVal, nodeNmVal) => {
      nodeCd.value = nodeCdVal ?? "";
      nodeNm.value = nodeNmVal ?? "";
    },
  });
};

onMounted(async () => {
  fnButtonControll();
  await fnSearch();
});

// 신규(생성) 버튼만 노출. 저장/삭제/엑셀은 팝업에서 처리.
// create(생성) 버튼 노출 여부는 메뉴 프레임워크가 props.buttons(BTN_NEW)로 내려준 값을 그대로 따른다.
const fnButtonControll = () => {
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

// 목록 내용 미리보기(말줄임은 CSS가 처리하므로 줄바꿈만 공백으로 치환)
const fnContentPreview = (content) => {
  if (!content) return "";
  return String(content).replace(/\s+/g, " ").trim();
};

// 공지 목록 조회 (식별자 cmpnyCd/userCd 는 서버 JWT 도출 → 전송하지 않음)
//   - 등록월(YYYY-MM)을 startDate/endDate(YYYY-MM-DD)로 변환해 보내며 목록 API 도 동일 포맷을 받는다.
//   - 정렬(고정 우선·순번·최신순)은 서버가 §5 로 내려주므로 클라 재정렬하지 않는다.
const fnSearch = async () => {
  noticeResultList.value = [];

  // 등록월(YYYY-MM) → 해당 월 1일 ~ 말일(YYYY-MM-DD)로 변환. 미선택 시 빈 값(전체 기간).
  let startDate = "";
  let endDate = "";
  if (registMonth.value) {
    const [year, month] = registMonth.value.split("-").map(Number);
    const lastDay = new Date(year, month, 0).getDate(); // 해당 월 말일
    const mm = String(month).padStart(2, "0");
    startDate = `${year}-${mm}-01`;
    endDate = `${year}-${mm}-${String(lastDay).padStart(2, "0")}`;
  }

  try {
    const response = await axios.get("/webApi/notice01/notice-lists", {
      params: {
        titleKeyword: titleKeyword.value,
        popupYn: popupYn.value,
        pinYn: pinYn.value,
        startDate,
        endDate,
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
      },
    });

    if (response.status === 200) {
      // 상태 정렬/표시용 파생 필드: 미확인(안 읽음 or 읽은 뒤 수정됨) / 확인
      noticeResultList.value = (response.data?.noticeList || []).map((it) => ({
        ...it,
        ackStatusNm: it.isUnread || it.isUpdated ? "미확인" : "확인",
      }));
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 신규 공지 생성 팝업
const fnOpenCreatePop = () => {
  openPop(NoticeCreatePop, {
    onSave: () => {
      fnSearch();
    },
  });
};

// 조회/수정 팝업 (행 더블클릭)
const fnOpenInfoPop = (item) => {
  openPop(NoticeInfoPop, {
    noticeData: {
      noticeId: item.noticeId || "",
    },
    onSave: () => {
      fnSearch();
    },
  });
};
</script>

<style scoped>
/* 조회조건 행 간 세로 간격만 절반으로(전역 gap 2rem → row-gap 1rem).
   가로(필드 간) 간격은 column-gap 2rem 그대로 유지 */
.viewSearch {
  row-gap: 1rem;
}

/* 049-02: 첨부 개수 표시 */
.file-cnt {
  font-size: var(--font-size-sm, 0.875rem);
  color: var(--color-text, #374151);
  white-space: nowrap;
}

.file-cnt--none {
  color: var(--color-text-muted, #4b5563);
}

/* 고정 공지 row 배경 (연한 붉은색) — §12-1 */
.notice-row--pinned {
  background: var(--color-pinned-bg, #fef2f2);
}

.notice-row--pinned:hover {
  background: var(--color-pinned-bg-hover, #fee2e2);
}

.notice-title {
  font-weight: 600;
  color: var(--color-text-strong, #111827);
}

/* 내용 말줄임 (...) */
.notice-content-ellipsis {
  max-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-muted, #4b5563);
}

/* NEW / UPDATE 뱃지 — 본인 기준 (§7) */
.notice-badge {
  display: inline-block;
  padding: var(--space-xxs, 0.125rem) var(--space-sm, 0.5rem);
  border-radius: var(--radius-pill, 999px);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
}

/* 미확인: 강조(읽지 않았거나 읽은 뒤 수정됨) */
.notice-badge--unread {
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
}

/* 확인: 차분한 색(이미 읽음) */
.notice-badge--read {
  background: var(--color-surface-muted, #f3f4f6);
  color: var(--color-text-muted, #4b5563);
}
</style>
