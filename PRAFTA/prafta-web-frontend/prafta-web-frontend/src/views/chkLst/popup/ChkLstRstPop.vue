<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 헤더 -->
        <div class="modal-header" @mousedown="startDrag">
          <span>점검결과 확인서</span>
          <button
            type="button"
            class="icon-button"
            @mousedown.stop
            @click.stop="$emit('close')"
          >
            ✕
          </button>
        </div>

        <div class="content-wrapper">
          <div class="main-content">
            <!-- 왼쪽 영역: 설비 점검표 -->
            <div class="left-panel">
              <div class="panel-content">
                <!-- 프린트 영역 (버튼 제외) -->
                <div ref="printArea" class="print-area">
                  <!-- 제목 -->
                  <div class="form-title-wrapper">
                    <div class="month-index">
                      {{ workMonthIdx + 1 }} / {{ props.chkptInfo.length }}
                    </div>
                    <button
                      class="month-nav-button prev-button"
                      @click="changeMonth(-1)"
                      :disabled="workMonthIdx === 0"
                    >
                      ◀
                    </button>
                    <h1 class="form-title">
                      {{ formatWorkMonthTitle(formData.workMonth) }} 점검결과
                      확인서
                    </h1>
                    <button
                      class="month-nav-button next-button"
                      @click="changeMonth(1)"
                      :disabled="workMonthIdx >= props.chkptInfo.length - 1"
                    >
                      ▶
                    </button>
                  </div>

                  <!-- 헤더 정보 -->
                  <div class="form-header">
                    <div class="header-row">
                      <div class="header-left">
                        <div class="header-item">
                          <label>점검대상명칭:</label>
                          <span>{{ formData.chkptNm || "-" }}</span>
                        </div>
                        <div class="header-item">
                          <label>점검구분분:</label>
                          <span>{{ formData.chkLstTypeNm || "-" }}</span>
                        </div>
                      </div>
                      <div class="header-right">
                        <div class="header-item">
                          <label>사업장:</label>
                          <span>{{ formData.siteNm || "-" }}</span>
                        </div>
                        <div class="header-item">
                          <label>관리자:</label>
                          <span>{{ formData.siteAdminNm || "-" }}</span>
                        </div>
                      </div>
                    </div>
                    <!-- 비고 -->
                    <div class="header-item full-width">
                      <label>비고:</label>
                      <span>{{ formData.chkptDesc || "-" }}</span>
                    </div>
                  </div>

                  <!-- 범례 -->
                  <div class="legend">
                    <span>범례: </span>
                    <span class="legend-item">O 양호</span>
                    <span class="legend-item">X 불량</span>
                  </div>

                  <!-- 메인 테이블 -->
                  <div class="table-wrapper inspection-table-wrapper">
                    <table class="inspection-table">
                      <thead>
                        <tr>
                          <th rowspan="2" class="col-no">No.</th>
                          <th rowspan="2" class="col-item">점검항목</th>
                          <th colspan="31">순회점검결과</th>
                        </tr>
                        <tr>
                          <th v-for="day in 31" :key="day" class="day-cell">
                            {{ day }}
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        <!-- 순회점검 섹션 -->
                        <tr
                          v-for="(
                            item, idx
                          ) in inspectionInfo.inspectItemSubjResultList"
                          :key="'daily-' + idx"
                        >
                          <td class="col-no">{{ idx + 1 }}</td>
                          <td class="col-item">{{ item.itemNm || "-" }}</td>
                          <td
                            v-for="day in 31"
                            :key="day"
                            class="day-cell"
                            :class="{ 'cell-disabled': isCellGrayed(idx, day) }"
                            :title="getInspectionTooltip(idx, day)"
                          >
                            {{ getInspectionResult(idx, day) }}
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>

            <!-- 화살표 버튼 -->
            <button
              class="toggle-button"
              :class="{ active: showRightPanel }"
              @click="toggleRightPanel"
            >
              <span v-if="!showRightPanel">◀</span>
              <span v-else>▶</span>
            </button>

            <!-- 오른쪽 영역: 불량 항목 그리드 -->
            <div class="right-panel" :class="{ show: showRightPanel }">
              <div class="right-panel-content">
                <h2 class="right-panel-title">불량 항목</h2>
                <div class="right-table-wrapper">
                  <table class="anomaly-table">
                    <thead>
                      <tr>
                        <th>No.</th>
                        <th>점검항목</th>
                        <th>점검일</th>
                        <th>결과</th>
                        <th>비고</th>
                        <th>첨부사진</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="(
                          item, idx
                        ) in inspectionInfo.dailyResults.filter(
                          (item) => item.result !== 'O'
                        )"
                        :key="idx"
                        class="anomaly-row"
                      >
                        <td>{{ idx + 1 }}</td>
                        <td>{{ item.inspectItemSubj }}</td>
                        <td>{{ item.inspectDay }}일</td>
                        <td
                          class="result-cell"
                          :class="{
                            'result-error': item.result === 'X',
                          }"
                        >
                          {{ item.result }}
                        </td>
                        <td>{{ item.answerDesc || "-" }}</td>
                        <td>
                          <button
                            v-if="item.fileMgmtCd"
                            class="view-image-btn"
                            @click="
                              openImagePopup(item.filePath, item.fileMgmtCd)
                            "
                          >
                            보기
                          </button>
                          <span v-else>-</span>
                        </td>
                      </tr>
                      <tr
                        v-if="
                          inspectionInfo.dailyResults.filter(
                            (item) => item.result !== 'O'
                          ).length === 0
                        "
                      >
                        <td colspan="6" class="empty-message">
                          이상 항목이 없습니다.
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer: SiteInfoPop과 동일 가이드(구분선 + 우측 정렬 버튼) -->
        <div class="modal-footer">
          <div class="btn-group">
            <button type="button" class="btn btn-primary" @click="fnPrint">
              프린트
            </button>
            <button
              type="button"
              class="btn btn-primary"
              @click="$emit('close')"
            >
              닫기
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>

  <!-- 사진 팝업: body로 Teleport — prafta-modal-open pointer-events 대응 + 메인 모달 드래그와 분리 -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="showImagePopup"
        ref="imagePopupOverlayRef"
        class="image-popup-overlay prafta-nested-modal-overlay"
        role="dialog"
        aria-modal="true"
        aria-label="첨부사진"
        tabindex="-1"
        @click="closeImagePopup"
      >
        <div class="image-popup-content" @mousedown.stop @click.stop>
          <div class="image-popup-header">
            <span>첨부사진</span>
            <button
              type="button"
              class="icon-button"
              @mousedown.stop
              @click.stop="closeImagePopup"
            >
              ✕
            </button>
          </div>
          <div class="image-popup-body">
            <img :src="imageUrl" alt="첨부사진" class="popup-image" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
<script setup>
/* eslint-disable */
import {
  ref,
  reactive,
  onMounted,
  watch,
  nextTick,
  getCurrentInstance,
  defineProps,
  defineEmits,
  computed,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { buildFileServingUrl } from "@/utils/fileUrl";

const props = defineProps({
  cmpnyCd: { type: String, required: true },
  chkptInfo: { type: Array, required: true }, // [{ siteCd, siteNm, chkptCd, chkptNm, chkLstType }]
});

// const emit =
defineEmits(["close"]);

const modalRef = ref(null);
const { position, startDrag, stopDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const { proxy } = getCurrentInstance();

/** inspect-result-details: 항목 마스터는 PascalCase·답변은 camelCase로 올 수 있음 */
const subjRowItemCd = (row) => row?.inspectItemCd ?? row?.InspectItemCd;
const subjRowItemSubj = (row) =>
  row?.inspectItemSubj ?? row?.InspectItemSubj;

const printArea = ref(null);
const workMonthIdx = ref(0);

const formData = reactive({
  siteNm: "",
  chkptNm: "",
  chkptTypeNm: "",
  chkLstTypeNm: "",
  siteAdminNm: "",
  chkptDesc: "",
});

const dailyInspectionItems = ref([]);
const inspectionData = ref({}); // { siteCd_chkLstType: { itemIdx_day: result } }
const showRightPanel = ref(false);
const showImagePopup = ref(false);
const imageUrl = ref("");
const imagePopupOverlayRef = ref(null);

// 오른쪽 패널 토글
const toggleRightPanel = () => {
  showRightPanel.value = !showRightPanel.value;
};

// 월 변경 함수
const changeMonth = async (direction) => {
  const newIdx = workMonthIdx.value + direction;
  if (newIdx >= 0 && newIdx < props.chkptInfo.length) {
    workMonthIdx.value = newIdx;
    await initializePopup();
  }
};

onMounted(async () => {
  // 최초 마운트 시 초기화
  await initializePopup();
});

// 팝업이 열릴 때마다 호출되는 함수
const initializePopup = async () => {
  formData.chkptNm = props.chkptInfo[workMonthIdx.value].chkptNm;
  formData.chkptTypeNm = props.chkptInfo[workMonthIdx.value].chkptTypeNm;
  formData.siteNm = props.chkptInfo[workMonthIdx.value].siteNm;
  formData.siteAdminNm = props.chkptInfo[workMonthIdx.value].siteAdminNm;
  formData.chkLstTypeNm = props.chkptInfo[workMonthIdx.value].chkLstTypeNm;
  formData.chkptDesc = props.chkptInfo[workMonthIdx.value].chkptDesc;
  formData.workMonth = props.chkptInfo[workMonthIdx.value].workMonth;
  // 점검 항목 데이터 조회
  await fnGetInspectionInfo();
};

// 점검 항목 데이터 조회
const fnGetInspectionInfo = async () => {
  const chkptInfo = props.chkptInfo[workMonthIdx.value];

  inspectionInfo.inspectItemSubjResultList = [];
  inspectionInfo.dailyResults = [];

  try {
    const response = await axios.get(
      "/webApi/chkLst03/inspect-result-details",
      {
        params: {
          siteCd: chkptInfo.siteCd,
          chkLstType: chkptInfo.chkLstType,
          workMonth: chkptInfo.workMonth,
          chkptCd: chkptInfo.chkptCd,
        },
      }
    );

    if (response.status === 200) {
      const resData = response.data;
      const rawSubjList = resData.inspectItemSubjResultList ?? [];
      const inspectAnswerResultList = resData.inspectAnswerResultList ?? [];
      const inspectItemHistResultList = resData.inspectItemHistResultList ?? [];

      // 문항 변경이력을 항목별 상태 스냅샷 목록으로 재구성한다(변경일 오름차순, 서버 정렬 유지).
      //   각 이력 행은 변경 '후' USE_YN 을 담고 있어, 임의 일자의 사용/중지 상태를
      //   "해당 일자 이전 마지막 이력의 USE_YN" 으로 복원할 수 있다 (사용중지↔재사용 반복 대응).
      const histMap = {};
      inspectItemHistResultList.forEach((h) => {
        const cd = h?.inspectItemCd ?? h?.InspectItemCd;
        if (!cd) return;
        if (!histMap[cd]) histMap[cd] = [];
        histMap[cd].push({
          useYn: h?.useYn ?? h?.UseYn ?? "Y",
          chgYmd: String(h?.chgYmd ?? h?.ChgYmd ?? ""),
        });
      });

      // 확인서에서 점검항목 행을 노출 월(workMonth) 기준으로 필터링한다.
      //   분기한 배열을 표시·dailyResults 인덱스 계산에 공통 사용해 getInspectionResult 의
      //   itemIdx 매핑이 어긋나지 않도록 한다.
      const displayYm = String(chkptInfo.workMonth ?? "");
      const monthOf = (v) => {
        const s = String(v ?? "");
        return s.length >= 6 ? s.slice(0, 6) : s;
      };
      const inspectItemSubjResultList = rawSubjList.filter((row) => {
        if (displayYm.length !== 6) return true;
        // (1) 시행일(STR_DATE, YYYYMMDD)의 월이 표시월보다 미래인 항목은 행 자체를 제외.
        const sm = monthOf(row?.strDate ?? row?.StrDate);
        if (sm && displayYm < sm) return false;
        // (2) 미사용(USE_YN='N') 항목: 사용중지된 '월'의 다음 월부터 행 제외.
        //     중지 시점은 변경이력의 마지막 상태(USE_YN='N') 변경일을 우선 사용하고,
        //     이력이 없으면 UPDATE_DATE(updateYmd) 로 폴백한다.
        const useYn = row?.useYn ?? row?.UseYn ?? "Y";
        if (useYn === "N") {
          const cd = subjRowItemCd(row);
          const events = histMap[cd] ?? [];
          const lastOff = [...events].reverse().find((e) => e.useYn === "N");
          const offMonth = monthOf(
            lastOff?.chgYmd || (row?.updateYmd ?? row?.UpdateYmd)
          );
          if (offMonth && displayYm > offMonth) return false;
        }
        return true;
      });

      inspectionInfo.inspectItemSubjResultList =
        inspectItemSubjResultList.map((item) => ({
          itemNm: subjRowItemSubj(item) || "-",
          // PRAFTA_COM_001-T5-11.1.1: 셀 회색 처리용 게이팅 기준값 (이력 기반으로 전환)
          strDate: item.strDate ?? item.StrDate ?? "",
          useYn: item.useYn ?? item.UseYn ?? "Y",
          updateYmd: item.updateYmd ?? item.UpdateYmd ?? "",
          histEvents: histMap[subjRowItemCd(item)] ?? [],
        }));

      if (
        proxy.$util.isNotEmpty(inspectAnswerResultList) &&
        inspectAnswerResultList.length > 0
      ) {
        const answerItemCd = (row) =>
          row?.inspectItemCd ?? row?.InspectItemCd;

        inspectionInfo.dailyResults = inspectAnswerResultList
          .map((item) => {
            const targetCd = answerItemCd(item);
            const inspectIdx = inspectItemSubjResultList
              .filter((subjItem) => subjRowItemCd(subjItem) === targetCd)
              .map((subjItem) => {
                const originalIndex =
                  inspectItemSubjResultList.indexOf(subjItem);

                return {
                  inspectItemSubj: subjRowItemSubj(subjItem),
                  index: originalIndex,
                };
              });

            if (proxy.$util.isNotEmpty(inspectIdx) && inspectIdx.length === 1) {
              return {
                inspectDay: item.workDate,
                itemIdx: inspectIdx[0].index,
                result: item.inspectAnswerType,
                answerDesc: item.answerDesc,
                inspectItemSubj: inspectIdx[0].inspectItemSubj,
                fileMgmtCd: item.fileMgmtCd,
                filePath: item.filePath,
                // PRAFTA-SUBCON-T6-07: 수행 주체(스냅샷) — 연동 회사가 점검한 셀도 수행자를 표시한다
                performCmpnyNm: item.performCmpnyNm,
                performUserNm: item.performUserNm,
                performUserCd: item.performUserCd,
              };
            }
            return null;
          })
          .filter(Boolean);
      }
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

const inspectionInfo = reactive({
  equipmentInfo: {
    chkptNm: "",
    chkptTypeNm: "",
    department: "",
    inspector: "",
    siteNm: "",
  },
  inspectItemSubjResultList: [],
  dailyResults: [],
});

// chkptInfo가 변경될 때마다 (팝업이 열릴 때마다) 호출
watch(
  () => props.chkptInfo,
  async () => {
    await initializePopup();
  },
  { deep: true }
);

// PRAFTA_COM_001-T5-11.1.2: 제목 시행월 표기 YYYYMM -> "YYYY년 MM월"
const formatWorkMonthTitle = (workMonth) => {
  const ym = String(workMonth ?? "");
  if (ym.length !== 6) return ym;
  return `${ym.slice(0, 4)}년 ${ym.slice(4, 6)}월`;
};

// PRAFTA_COM_001-T5-11.1.1: 셀 회색 처리 판정 (시행일 + 변경이력 기반)
//  - 셀 일자 < 항목 시행일(STR_DATE, YYYYMMDD)      -> 시행 이전(일 단위) 회색
//  - 변경이력상 해당 일자에 사용중지(N) 상태          -> 미사용 구간(일 단위) 회색
//    상태 판정: 셀 일자 '이전'의 마지막 이력 스냅샷 USE_YN (변경 당일 셀은 회색 아님,
//    기존 UPDATE_DATE 기준 동작과 동일 경계). 사용중지↔재사용 반복 이력도 구간별로 복원.
//  - 이력이 없는 항목은 기존 규칙(USE_YN='N' 이고 셀 일자 > UPDATE_DATE)으로 폴백.
const isCellGrayed = (idx, day) => {
  const item = inspectionInfo.inspectItemSubjResultList[idx];
  if (!item) return false;

  const ym = String(formData.workMonth ?? "");
  if (ym.length !== 6) return false;

  const ymd = ym + String(day).padStart(2, "0"); // YYYYMMDD (해당 셀 일자)

  // 1) 시행일 이전 (일 단위; 구값 YYYYMM 이 남아있으면 월 단위 비교)
  const strDate = String(item.strDate ?? "");
  if (strDate.length === 8 && ymd < strDate) return true;
  if (strDate.length === 6 && ym < strDate) return true;

  // 2) 변경이력 기반 미사용 구간
  const events = item.histEvents ?? [];
  if (events.length > 0) {
    let state = "Y"; // 이력상 첫 변경 이전은 사용중으로 간주(시행일 규칙이 앞 구간을 커버)
    for (const e of events) {
      if (e.chgYmd && e.chgYmd < ymd) state = e.useYn;
      else break;
    }
    return state === "N";
  }

  // 3) 이력 부재 폴백: 비활성(USE_YN='N') 이후 (UPDATE_DATE 기준)
  if (item.useYn === "N" && item.updateYmd && ymd > String(item.updateYmd)) {
    return true;
  }

  return false;
};

// 점검 결과 가져오기
const getInspectionResult = (idx, day) => {
  return (
    inspectionInfo.dailyResults.find(
      (result) =>
        result != null &&
        Number(result.inspectDay) === Number(day) &&
        Number(result.itemIdx) === Number(idx)
    )?.result || ""
  );
};

// PRAFTA-SUBCON-T6-07: 응답 셀 hover 툴팁 — 수행 주체(회사/성명/ID) 표시.
//   회사명은 자기 테넌트에 저장된 인접 1차 회사(relabel 값)이며, 성명은 저장 시점 스냅샷이다.
const getInspectionTooltip = (idx, day) => {
  const cell = inspectionInfo.dailyResults.find(
    (result) =>
      result != null &&
      Number(result.inspectDay) === Number(day) &&
      Number(result.itemIdx) === Number(idx)
  );
  if (!cell) return "";

  const cmpnyNm = cell.performCmpnyNm || "";
  const userNm = cell.performUserNm || "";
  const userCd = cell.performUserCd || "";
  if (!cmpnyNm && !userNm) return "";

  const who = userCd ? `${userNm}(${userCd})` : userNm;
  return cmpnyNm ? `수행: ${cmpnyNm} / ${who}` : `수행: ${who}`;
};

// 사진 팝업 열기
const openImagePopup = (filePath, fileMgmtCd) => {
  if (filePath && fileMgmtCd) {
    // 첨부 서빙 URL 은 공통 유틸로 조립 (백슬래시 정규화 + 동일 출처 상대경로)
    const url = buildFileServingUrl(filePath, fileMgmtCd);

    stopDrag();
    imageUrl.value = url;
    showImagePopup.value = true;
    nextTick(() => {
      imagePopupOverlayRef.value?.focus({ preventScroll: true });
    });
  }
};

// 사진 팝업 닫기
const closeImagePopup = () => {
  stopDrag();
  showImagePopup.value = false;
  imageUrl.value = "";
};

// 프린트 기능
const fnPrint = () => {
  if (!printArea.value) return;

  const printWindow = window.open("", "_blank", "width=1200,height=800");
  const printContent = printArea.value.innerHTML;

  printWindow.document.write(`
    <html>
      <head>
        <title>설비 점검표</title>
        <style>
          * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
          }
          body {
            font-family: "Pretendard", sans-serif;
            font-size: 12px;
            padding: 20px;
          }
          .form-title-wrapper {
            display: block;
            text-align: center;
            position: relative;
          }
          .month-nav-button {
            display: none !important;
          }
          .month-index {
            display: none !important;
          }
          .form-title {
            text-align: center;
            font-size: 24px;
            font-weight: bold;
            margin-bottom: 20px;
          }
          .form-header {
            display: flex;
            flex-direction: column;
            gap: 10px;
            margin-bottom: 15px;
          }
          .header-row {
            display: flex;
            justify-content: space-between;
            gap: 20px;
          }
          .header-left, .header-right {
            display: flex;
            flex-direction: column;
            gap: 8px;
            flex: 1;
          }
          .header-item {
            display: flex;
            gap: 10px;
            align-items: center;
          }
          .header-item.full-width {
            width: 100%;
          }
          .header-item label {
            font-weight: bold;
            min-width: 100px;
            background-color: #f0f0f0;
            border: 1px solid #ccc;
            padding: 6px 10px;
            text-align: center;
            font-size: 13px;
          }
          .header-item span {
            flex: 1;
            border: 1px solid #ccc;
            padding: 6px 10px;
            font-size: 13px;
            background-color: #fff;
          }
          .legend {
            margin-bottom: 10px;
            font-size: 12px;
          }
          .legend-item {
            margin-left: 15px;
          }
          .table-wrapper {
            overflow-x: auto;
            margin-bottom: 20px;
          }
          .inspection-table {
            width: 100%;
            border-collapse: collapse;
            border: 1px solid #000;
            font-size: 11px;
          }
          .inspection-table th,
          .inspection-table td {
            border: 1px solid #000;
            padding: 4px;
            text-align: center;
          }
          .col-no {
            width: 40px;
            min-width: 40px;
          }
          .col-item {
            width: 200px;
            min-width: 200px;
            text-align: left;
            padding-left: 10px;
          }
          .day-cell {
            width: 28px;
            min-width: 28px;
          }
          .cell-disabled {
            background-color: #e5e7eb !important;
            -webkit-print-color-adjust: exact;
            print-color-adjust: exact;
          }
          .button-area {
            display: flex;
            justify-content: center;
            gap: 10px;
            padding-top: 20px;
            border-top: 1px solid #ddd;
          }
          .btn {
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
            font-weight: 500;
          }
          .btn-primary {
            background-color: #16a34a;
            color: white;
          }
          .btn-secondary {
            background-color: #6c757d;
            color: white;
          }
          .month-nav-button {
            display: none !important;
          }
          .month-index {
            display: none !important;
          }
          .form-title-wrapper {
            display: block;
            text-align: center;
          }
          @media print {
            body {
              padding: 10px;
            }
            .month-nav-button {
              display: none !important;
            }
            .month-index {
              display: none !important;
            }
            .form-title-wrapper {
              display: block;
              text-align: center;
            }
            .inspection-table {
              font-size: 10px;
            }
            .day-cell {
              width: 25px;
              min-width: 25px;
            }
          }
        </style>
      </head>
      <body>
        ${printContent}
      </body>
    </html>
  `);

  printWindow.document.close();
  printWindow.print();
};
</script>

<style scoped>
/* 본문 영역: SiteInfoPop.content-wrapper 와 동일하게 flex:1 + 패딩 */
.content-wrapper {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 1.2rem;
  gap: 0;
}

/* 하단 버튼 바: 모달 본문에 붙이고 축소되지 않도록 (prafta-modal-footer 가이드 보강) */
.modal-footer {
  flex-shrink: 0;
  position: relative;
  z-index: 2;
}

.modal-footer .btn-group {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  width: 100%;
}

.main-content {
  display: flex;
  flex: 1;
  position: relative;
  min-height: 0;
}

.left-panel {
  flex: 1;
  padding: 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;
}

.panel-content {
  background: white;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  min-height: 0;
}

.print-area {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  max-height: 100%;
}

.form-title-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-bottom: 20px;
  flex-shrink: 0;
  position: relative;
}

.month-index {
  position: absolute;
  top: -30px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.form-title {
  text-align: center;
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin: 0;
  flex: 1;
}

.month-nav-button {
  width: 40px;
  height: 40px;
  border: 1px solid #ccc;
  background-color: #fff;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #333;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.month-nav-button:hover:not(:disabled) {
  background-color: #f0f0f0;
  border-color: #16a34a;
  color: #16a34a;
}

.month-nav-button:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.form-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 15px;
  border-radius: 4px;
  flex-shrink: 0;
}

.header-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.header-left,
.header-right {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.header-item {
  display: flex;
  gap: 10px;
  align-items: center;
}

.header-item.full-width {
  width: 100%;
}

.header-item label {
  font-weight: bold;
  min-width: 100px;
  background-color: #f0f0f0;
  border: 1px solid #ccc;
  padding: 6px 10px;
  text-align: center;
  font-size: 13px;
  border-radius: 4px;
}

.header-item span {
  flex: 1;
  border: 1px solid #ccc;
  padding: 6px 10px;
  font-size: 13px;
  background-color: #fff;
  border-radius: 4px;
}

.legend {
  margin-bottom: 10px;
  font-size: 12px;
  color: #666;
  flex-shrink: 0;
}

.legend-item {
  margin-left: 15px;
}

.table-wrapper.inspection-table-wrapper {
  flex: 1 1 0;
  overflow-y: auto !important;
  overflow-x: auto !important;
  margin-bottom: 20px;
  min-height: 0;
  max-height: 100%;
  height: 0; /* flex: 1과 함께 사용하여 높이 제한 강제 */
}

.inspection-table {
  width: 100%;
  border-collapse: collapse;
  border: 1px solid #ccc;
  font-size: 11px;
}

.inspection-table th,
.inspection-table td {
  border: 1px solid #ccc;
  padding: 4px;
  text-align: center;
}

.inspection-table thead {
  position: sticky;
  top: 0;
  background-color: #f0f0f0;
  z-index: 1;
}

.col-no {
  width: 40px;
  min-width: 40px;
}

.col-item {
  width: 200px;
  min-width: 200px;
  text-align: left;
  padding-left: 10px;
}

.day-cell {
  width: 28px;
  min-width: 28px;
  font-size: 10px;
}

/* PRAFTA_COM_001-T5-11.1.1: 시행일 이전/미사용 구간(변경이력 기반) 셀 회색 처리 */
.cell-disabled {
  background-color: #e5e7eb;
}

.button-area {
  display: flex;
  justify-content: center;
  gap: 10px;
  padding-top: 20px;
  border-top: 1px solid #ddd;
  flex-shrink: 0;
}

.toggle-button {
  position: absolute;
  right: 0;
  top: 90%;
  transform: translateY(-50%);
  z-index: 50;
  width: 26px;
  height: 60px;
  background-color: #16a34a;
  color: white;
  border: none;
  border-radius: 4px 0 0 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.3s ease-in-out;
  box-shadow: -2px 0 4px rgba(0.1, 0.1, 0, 0);
}

.toggle-button:hover {
  background-color: #15803d;
}

.toggle-button.active {
  right: 1000px;
  border-radius: 4px 0px 0px 4px;
}

.right-panel {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 0;
  overflow: hidden;
  background-color: #f9f9f9;
  border-left: 2px solid #30796a;
  transition: width 0.3s ease-in-out;
  display: flex;
  flex-direction: column;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.1);
  z-index: 40;
}

.right-panel.show {
  width: 1000px;
}

.right-panel-content {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.right-panel-title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 2px solid #30796a;
}

.right-table-wrapper {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #ccc;
  border-radius: 4px;
  background-color: white;
}

.anomaly-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.anomaly-table thead {
  position: sticky;
  top: 0;
  background-color: #30796a;
  color: white;
  z-index: 1;
}

.anomaly-table th {
  padding: 10px 8px;
  text-align: center;
  font-weight: bold;
  border: 1px solid #256b5a;
}

.anomaly-table td {
  padding: 8px;
  text-align: center;
  border: 1px solid #ccc;
}

.anomaly-row:nth-child(even) {
  background-color: #f9f9f9;
}

.anomaly-row:hover {
  background-color: #f0f0f0;
}

.result-cell {
  font-weight: bold;
  font-size: 14px;
}

.result-warning {
  color: #ff9800;
}

.result-error {
  color: #f44336;
}

.empty-message {
  text-align: center;
  padding: 40px;
  color: #999;
  font-style: italic;
}

.modal-content-wide {
  width: 1200px;
  max-width: 95vw;
  max-height: 90vh;
  height: 90vh;
  position: fixed;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.btn-secondary:hover {
  background-color: #5a6268;
}

.view-image-btn {
  padding: 4px 12px;
  background-color: #16a34a;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background-color 0.2s ease;
}

.view-image-btn:hover {
  background-color: #15803d;
}

.image-popup-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  /* 메인 모달(z-index 1000) 위 + body.prafta-modal-open 시 클릭 수신 */
  z-index: 10050;
  pointer-events: auto;
  outline: none;
}

.image-popup-content {
  background: white;
  border-radius: 8px;
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.image-popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #ddd;
  background-color: #b0e1d6;
}

.image-popup-header span {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.image-popup-body {
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
  max-height: calc(90vh - 60px);
}

.popup-image {
  max-width: 100%;
  max-height: calc(90vh - 100px);
  object-fit: contain;
  border-radius: 4px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
