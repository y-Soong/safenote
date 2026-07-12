<!--
  Platform_02.vue — 이용약관 관리 (플랫폼 운영자 전용 콘솔)
  - 메뉴: tb_syst_menu_d MENU_D_ID='Platform_02', MENU_VIEW='platform/Platform_02.vue'
  - 접근: CMPNY_CD='prafta_system_admin' 운영자만(서버 /platformApi 게이트가 강제. 메뉴 숨김은 보조).
  - 원본: src/views/baim/Baim_03.vue 를 포팅. 약관(TB_TERMS)은 글로벌 데이터라 회사 스코프 없음.
  - 차이점: API 호출 경로를 /webApi/baim03/* → /platformApi/terms/* 로 변경,
            저장 팝업은 운영자 전용 PlatformTermsInfoPop 사용.
-->
<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 검색 영역 -->
    <div class="viewSearch">
      <div>
        <label>약관명</label>
        <input v-model.trim="termsNm" type="text" />
      </div>
    </div>

    <!-- ✅ 테이블 2개 나란히 -->
    <div class="viewBody tables-row">
      <!-- LEFT TABLE -->
      <div class="table-wrapper subtitle-pane" style="flex: 0 0 25%">
        <!-- ⬇️ 소제목 바 -->
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <!-- 단순 마크 아이콘 (SVG) -->
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">이용약관</span>
        </div>

        <div
          class="table-box"
          style="--box-h: 65vh; --box-sticky-top: 1px; --box-ox: auto"
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
                  label="약관명"
                  col-key="termsNm"
                  :sort-key="termsSortKey"
                  :sort-order="termsSortOrder"
                  :width="termsColWidths.termsNm"
                  @sort="termsOnSort"
                  @update:width="termsOnResize"
                />
                <ThSortable
                  label="시행버전"
                  col-key="termsVersion"
                  :sort-key="termsSortKey"
                  :sort-order="termsSortOrder"
                  :width="termsColWidths.termsVersion"
                  @sort="termsOnSort"
                  @update:width="termsOnResize"
                />
              </tr>
            </thead>
            <tbody>
              <template v-if="!termsList || termsList.length === 0">
                <tr>
                  <td colspan="3" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(terms, idx) in termsSortedData"
                  :key="terms.termsId"
                  @dblclick="fnSubSearch(terms)"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ terms.termsNm }}</td>
                  <td>{{ terms.termsVersion }}</td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>

      <!-- RIGHT TABLE -->
      <div class="table-wrapper subtitle-pane" style="flex: 1">
        <div class="subtitle-row">
          <!-- ⬇️ 소제목 바 -->
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <!-- 단순 마크 아이콘 (SVG) -->
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">약관상세</span>
          </div>

          <!-- ⬇️ 소제목 바 -->
          <div class="subtitle">
            <span class="subtitle-text">[{{ termsNm }}]</span>
          </div>

          <div class="custom-btn-area">
            <button class="btn btn-custom" @click="fnAddRow()">생성</button>
          </div>
        </div>

        <div
          class="table-box"
          style="
            --box-h: 65vh;
            --box-sticky-top: 1px;
            --box-ox: auto;
            width: 100%;
          "
        >
          <table class="data-grid w-full table-fixed border-collapse text-sm">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 2%">
                  No
                </th>
                <ThSortable
                  label="약관버전"
                  col-key="termsVersion"
                  :sort-key="detailSortKey"
                  :sort-order="detailSortOrder"
                  :width="detailColWidths.termsVersion"
                  @sort="detailOnSort"
                  @update:width="detailOnResize"
                />
                <ThSortable
                  label="필수여부"
                  col-key="requiredYn"
                  :sort-key="detailSortKey"
                  :sort-order="detailSortOrder"
                  :width="detailColWidths.requiredYn"
                  @sort="detailOnSort"
                  @update:width="detailOnResize"
                />
                <ThSortable
                  label="시행일자"
                  col-key="strDate"
                  :sort-key="detailSortKey"
                  :sort-order="detailSortOrder"
                  :width="detailColWidths.strDate"
                  @sort="detailOnSort"
                  @update:width="detailOnResize"
                />
                <ThSortable
                  label="비고"
                  col-key="termsDesc"
                  :sort-key="detailSortKey"
                  :sort-order="detailSortOrder"
                  :width="detailColWidths.termsDesc"
                  @sort="detailOnSort"
                  @update:width="detailOnResize"
                />
              </tr>
            </thead>
            <tbody>
              <template
                v-if="!termsDetaillList || termsDetaillList.length === 0"
              >
                <tr>
                  <td colspan="5" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(termsDetail, idx) in detailSortedData"
                  :key="termsDetail.termsId"
                  @dblclick="fnAddRow(termsDetail)"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ termsDetail.termsVersion }}</td>
                  <td>{{ termsDetail.requiredYn }}</td>
                  <td>
                    {{ formatYmdDot(termsDetail.strDate) }}
                  </td>
                  <td>{{ termsDetail.termsDesc }}</td>
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
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatYmdDot } from "@/utils/dateFormat";
import PlatformTermsInfoPop from "@/views/platform/popup/PlatformTermsInfoPop.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

// keep-alive 매칭용 컴포넌트 이름 = 라우트 이름(MENU_D_ID)
defineOptions({ name: "Platform_02" });

// MainLayout 이 주입하는 공통 props(탭 제목/버튼 권한)
const props = defineProps({
  title: { type: String, default: "이용약관 관리" },
  buttons: { type: Object, default: () => ({}) },
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const termsList = ref([]);
const termsDetaillList = ref([]);
const {
  sortKey: termsSortKey,
  sortOrder: termsSortOrder,
  sortedData: termsSortedData,
  onSort: termsOnSort,
} = useTableSort(termsList);
const { colWidths: termsColWidths, onResize: termsOnResize } = useColumnResize({
  termsNm: 160,
  termsVersion: 90,
});
const {
  sortKey: detailSortKey,
  sortOrder: detailSortOrder,
  sortedData: detailSortedData,
  onSort: detailOnSort,
} = useTableSort(termsDetaillList);
const { colWidths: detailColWidths, onResize: detailOnResize } =
  useColumnResize({
    termsVersion: 100,
    requiredYn: 80,
    strDate: 130,
    termsDesc: 200,
  });
const systCodeArr = ref({});
const localButtons = ref({ ...props.buttons });

/* 조회조건 변수 세팅 */
const termsId = ref("");
const termsNm = ref("");

onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
  await fnSearch();
});

useFieldWatcher(
  termsDetaillList,
  (item) => {
    item.chk = true;
  },
  ["chk"]
);

// API 호출
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
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
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  termsList.value = [];
  termsDetaillList.value = [];

  try {
    const response = await axios.get("/platformApi/terms/terms-info-lists", {
      params: {
        termsNm: termsNm.value,
      },
    });

    if (response.status === 200) {
      termsList.value = response.data.termsInfoList;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSubSearch = async (terms) => {
  if (proxy.$util.isNotEmpty(terms)) {
    termsId.value = terms.termsId;
    termsNm.value = terms.termsNm;
  }

  termsDetaillList.value = [];

  try {
    const response = await axios.get(
      "/platformApi/terms/terms-detail-info-list",
      {
        params: {
          termsId: termsId.value,
        },
      }
    );

    if (response.status === 200) {
      termsDetaillList.value = response.data.termsDetailInfoList;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

/* user function */
function fnButtonControll() {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
}

function fnAddRow(terms) {
  if (proxy.$util.isNotEmpty(termsId.value)) {
    if (proxy.$util.isEmpty(terms)) {
      openPop(PlatformTermsInfoPop, {
        termsId_p: termsId.value,
        termsNm_p: termsNm.value,
        onSearch: fnSubSearch,
      });
    } else {
      openPop(PlatformTermsInfoPop, {
        termsId_p: termsId.value,
        termsNm_p: termsNm.value,
        termsVersion_p: terms.termsVersion,
        onSearch: fnSearch,
        onSubSearch: fnSubSearch,
      });
    }
  } else {
    fnAlertMsg(getMessage(MSG.TERMS_SELECT_REQUIRED));
  }
}

/* User Function */
async function fnAlertMsg(message, afterConfirmCallback) {
  await proxy.$alert(message);
  if (afterConfirmCallback) {
    afterConfirmCallback();
  }
}
</script>

<style scoped></style>
