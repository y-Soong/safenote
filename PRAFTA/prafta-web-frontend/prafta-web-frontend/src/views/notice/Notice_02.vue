<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @create="fnCreate"
    />

    <!-- 검색바: 자료타입 / 등록월 / 제목·내용 (회사 전체 공통 — 사업장/소속부서 없음) -->
    <div class="viewSearch">
      <div>
        <label>자료타입</label>
        <!-- TODO(developer): /webApi/notice02/archive-types 조회 결과를 archiveTypeList 에 바인딩.
             자료타입 코드그룹(BAIM_VAL_CD) 미주입 시 빈 목록(전체 옵션만 노출). -->
        <select v-model="archiveTypeCd" name="combo">
          <option value="">전체</option>
          <option
            v-for="t in archiveTypeList"
            :key="t.archiveTypeCd"
            :value="t.archiveTypeCd"
          >
            {{ t.archiveTypeNm }}
          </option>
        </select>
      </div>

      <div>
        <label>등록월</label>
        <CalendarSrch v-model="registMonth" month />
      </div>

      <div>
        <label>제목/내용</label>
        <input
          id="archiveTitleKeyword"
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
          <span class="subtitle-text">자료 리스트</span>
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
                  label="자료타입"
                  col-key="archiveTypeNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.archiveTypeNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
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
              <template
                v-if="!archiveResultList || archiveResultList.length === 0"
              >
                <tr>
                  <td colspan="7" class="edu-grid-empty">
                    등록된 자료가 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(item, idx) in sortedData"
                  :key="item.noticeId"
                  class="archive-row"
                  @dblclick="fnOpenInfoPop(item)"
                  style="cursor: pointer"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ item.archiveTypeNm }}</td>
                  <td class="archive-title">{{ item.title }}</td>
                  <td class="archive-content-ellipsis">
                    {{ fnContentPreview(item.content) }}
                  </td>
                  <td style="text-align: center">
                    <span
                      v-if="item.fileCnt > 0"
                      class="file-cnt"
                      :aria-label="`첨부 ${item.fileCnt}건`"
                    >
                      {{ item.fileCnt }}
                    </span>
                    <span v-else class="file-cnt file-cnt--none">-</span>
                  </td>
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
import ArchiveCreatePop from "./popup/ArchiveCreatePop.vue";
import ArchiveInfoPop from "./popup/ArchiveInfoPop.vue";

defineOptions({ name: "Notice_02" });
const props = defineProps({
  title: String,
  buttons: Object,
});

const localButtons = ref({ ...props.buttons });
const { open: openPop } = useModal();
const { proxy } = getCurrentInstance();

// 목록/정렬 상태
const archiveResultList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } =
  useTableSort(archiveResultList);
const { colWidths, onResize } = useColumnResize({
  archiveTypeNm: 140,
  title: 280,
  insertUserNm: 100,
  insertDate: 150,
});

// 자료타입 드롭다운 목록 (TODO(developer): archive-types API 로 채움)
const archiveTypeList = ref([]);

// 검색 조건 (식별자 cmpnyCd 는 서버 JWT 도출 → 전송하지 않음)
const archiveTypeCd = ref("");
const titleKeyword = ref("");
// 등록월(YYYY-MM). 조회 시 해당 월 1일~말일(YYYY-MM-DD)로 변환하여 서버 startDate/endDate 계약에 매핑.
const registMonth = ref("");

onMounted(async () => {
  fnButtonControll();
  // 자료타입 드롭다운 먼저 채운 뒤 목록 조회(조회조건 드롭다운 표시용)
  await fnLoadArchiveTypes();
  await fnSearch();
});

// 신규(생성) 버튼만 노출. 저장/삭제/엑셀은 팝업/미사용.
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

// 자료타입 드롭다운 조회 (코드그룹 BAIM_VAL_CD 미주입 시 빈 목록 → 전체 옵션만)
const fnLoadArchiveTypes = async () => {
  try {
    const response = await axios.get("/webApi/notice02/archive-types");
    if (response.status === 200) {
      archiveTypeList.value = response.data?.typeList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(
      err,
      "자료타입 조회 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  }
};

// 자료 목록 조회 (식별자 cmpnyCd 는 서버 JWT 도출 → 전송하지 않음)
//   - 등록월(YYYY-MM) → 해당 월 1일~말일(YYYY-MM-DD) 변환(Notice_01 검증 패턴).
const fnSearch = async () => {
  archiveResultList.value = [];

  // 등록월(YYYY-MM) → 해당 월 1일 ~ 말일(YYYY-MM-DD). 미선택 시 빈 값(전체 기간).
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
    const response = await axios.get("/webApi/notice02/archive-lists", {
      params: {
        archiveTypeCd: archiveTypeCd.value,
        titleKeyword: titleKeyword.value,
        startDate,
        endDate,
      },
    });
    if (response.status === 200) {
      archiveResultList.value = response.data?.archiveList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 신규 자료 생성 팝업
const fnCreate = () => {
  openPop(ArchiveCreatePop, {
    onSave: () => {
      fnSearch();
    },
  });
};

// 조회/수정 팝업 (행 더블클릭)
const fnOpenInfoPop = (item) => {
  openPop(ArchiveInfoPop, {
    archiveData: {
      noticeId: item.noticeId || "",
    },
    onSave: () => {
      fnSearch();
    },
  });
};
</script>

<style scoped>
/* 조회조건 행 간 세로 간격만 절반으로(전역 gap 2rem → row-gap 1rem). */
.viewSearch {
  row-gap: 1rem;
}

/* 첨부 개수 표시 */
.file-cnt {
  font-size: var(--font-size-sm, 0.875rem);
  color: var(--color-text, #374151);
  white-space: nowrap;
}

.file-cnt--none {
  color: var(--color-text-muted, #4b5563);
}

.archive-title {
  font-weight: 600;
  color: var(--color-text-strong, #111827);
}

/* 내용 말줄임 (...) */
.archive-content-ellipsis {
  max-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-muted, #4b5563);
}
</style>
