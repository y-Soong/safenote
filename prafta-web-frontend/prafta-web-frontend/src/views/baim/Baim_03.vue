<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!--
    @search="fnSearch"
    @create="fnCreate"
    @save="fnSave"
    -->

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
            <!-- <button class="btn btn-custom" @click="saveRow()">저장</button> -->
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
                  <!-- <td>{{ termsDetail.strDate }}</td> -->
                  <td>
                    {{ proxy.$util.formatDateString(termsDetail.strDate) }}
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
import TermsInfoPop from "@/components/popup/TermsInfoPop.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

defineOptions({ name: "Baim_02" });

const props = defineProps({
  title: String,
  buttons: Object,
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
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  termsList.value = [];
  termsDetaillList.value = [];

  try {
    const response = await axios.get("/webApi/baim03/terms-info-lists", {
      params: {
        termsNm: termsNm.value,
      },
    });

    if (response.status === 200) {
      termsList.value = response.data.termsInfoList;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

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
    const response = await axios.get("/webApi/baim03/terms-detail-info-list", {
      params: {
        termsId: termsId.value,
      },
    });

    if (response.status === 200) {
      termsDetaillList.value = response.data.termsDetailInfoList;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

/* user function */
function fnButtonControll() {
  // localButtons.value.search = "N";
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
}

function fnAddRow(terms) {
  if (proxy.$util.isNotEmpty(termsId.value)) {
    if (proxy.$util.isEmpty(terms)) {
      openPop(TermsInfoPop, {
        termsId_p: termsId.value,
        termsNm_p: termsNm.value,
        onSearch: fnSubSearch,
      });
    } else {
      openPop(TermsInfoPop, {
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
