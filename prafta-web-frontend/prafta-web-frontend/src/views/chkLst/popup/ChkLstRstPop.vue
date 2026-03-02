<template>
  <Transition name="fade">
    <div class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 헤더 -->
        <div class="modal-header" @mousedown="startDrag">
          <span>점검결과 확인서</span>
          <button class="icon-button" @click="$emit('close')">✕</button>
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
                      {{ formData.workMonth }} 점검결과 확인서
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
                          ) in inspectionInfo.inspectItemSubjList"
                          :key="'daily-' + idx"
                        >
                          <td class="col-no">{{ idx + 1 }}</td>
                          <td class="col-item">{{ item.itemNm || "-" }}</td>
                          <td v-for="day in 31" :key="day" class="day-cell">
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

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnPrint">프린트</button>
            <button class="btn btn-secondary" @click="$emit('close')">
              닫기
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>

  <!-- 사진 팝업 -->
  <Transition name="fade">
    <div
      v-if="showImagePopup"
      class="image-popup-overlay"
      @click="closeImagePopup"
    >
      <div class="image-popup-content" @click.stop>
        <div class="image-popup-header">
          <span>첨부사진</span>
          <button class="icon-button" @click="closeImagePopup">✕</button>
        </div>
        <div class="image-popup-body">
          <img :src="imageUrl" alt="첨부사진" class="popup-image" />
        </div>
      </div>
    </div>
  </Transition>
</template>
<script setup>
/* eslint-disable */
import {
  ref,
  reactive,
  onMounted,
  watch,
  getCurrentInstance,
  defineProps,
  defineEmits,
  computed,
} from 'vue';
import { useDraggable } from '@/composables/useDraggable';
import axios from '@/api/axios';

const props = defineProps({
  cmpnyCd: { type: String, required: true },
  chkptInfo: { type: Array, required: true }, // [{ siteCd, siteNm, chkptCd, chkptNm, chkLstType }]
});

// const emit =
defineEmits(['close']);

const { position, startDrag } = useDraggable(
  window.innerWidth / 2 - 600,
  window.innerHeight / 2 - 400
);

const { proxy } = getCurrentInstance();
const printArea = ref(null);
const modalRef = ref(null);
const workMonthIdx = ref(0);

const formData = reactive({
  siteNm: '',
  chkptNm: '',
  chkptTypeNm: '',
  chkLstTypeNm: '',
  siteAdminNm: '',
  chkptDesc: '',
});

const dailyInspectionItems = ref([]);
const inspectionData = ref({}); // { siteCd_chkLstType: { itemIdx_day: result } }
const showRightPanel = ref(false);
const showImagePopup = ref(false);
const imageUrl = ref('');

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

  inspectionInfo.inspectItemSubjList = [];
  inspectionInfo.dailyResults = [];

  try {
    const response = await axios.get(
      '/webApi/chkLst03/inspect-result-details',
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
      const inspectItemSubjList = resData.inspectItemSubjList;
      const inspectAnswerList = resData.inspectAnswerList;

      inspectionInfo.inspectItemSubjList = inspectItemSubjList.map((item) => {
        return { itemNm: item.inspectItemSubj };
      });

      if (
        proxy.$util.isNotEmpty(inspectAnswerList) &&
        inspectAnswerList.length > 0
      ) {
        inspectionInfo.dailyResults = inspectAnswerList.map((item) => {
          const inspectIdx = inspectItemSubjList
            .filter(
              (subjItem, index) => subjItem.inspectItemCd === item.inspectItemCd
            )
            .map((subjItem, index, arr) => {
              const originalIndex = inspectItemSubjList.indexOf(subjItem);
              return {
                inspectItemSubj: subjItem.inspectItemSubj,
                index: originalIndex,
              };
            });

          if (proxy.$util.isNotEmpty(inspectIdx) && inspectIdx.length == 1) {
            return {
              inspectDay: item.workDate,
              itemIdx: inspectIdx[0].index,
              result: item.inspectAnswerType,
              answerDesc: item.answerDesc,
              inspectItemSubj: inspectIdx[0].inspectItemSubj,
              fileMgmtCd: item.fileMgmtCd,
              filePath: item.filePath,
            };
          }
        });
      }
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '조회 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

const inspectionInfo = reactive({
  equipmentInfo: {
    chkptNm: '',
    chkptTypeNm: '',
    department: '',
    inspector: '',
    siteNm: '',
  },
  inspectItemSubjList: [],
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

// 점검 결과 가져오기
const getInspectionResult = (idx, day) => {
  return (
    inspectionInfo.dailyResults.find(
      (result) =>
        Number(result.inspectDay) === Number(day) &&
        Number(result.itemIdx) === Number(idx)
    )?.result || ''
  );
};

// 사진 팝업 열기
const openImagePopup = (filePath, fileMgmtCd) => {
  if (filePath && fileMgmtCd) {
    // API_BASE_URL 환경 변수 가져오기 (Vue CLI: VUE_APP_ 접두사 필요)
    // .env.development에 VUE_APP_API_BASE_URL로 설정되어 있어야 함
    const apiBaseUrl = process.env.VUE_APP_API_BASE_URL || '';

    // 파일 경로 구성: API_BASE_URL + filePath + "/" + fileMgmtCd
    let fullPath = `${filePath}/${fileMgmtCd}`;

    // API_BASE_URL이 있고 상대 경로인 경우에만 추가
    if (
      apiBaseUrl &&
      !filePath.startsWith('http://') &&
      !filePath.startsWith('https://')
    ) {
      // API_BASE_URL 끝에 슬래시가 있으면 제거
      const baseUrl = apiBaseUrl.endsWith('/')
        ? apiBaseUrl.slice(0, -1)
        : apiBaseUrl;
      // filePath 시작에 슬래시가 있으면 제거
      const cleanFilePath = filePath.startsWith('/')
        ? filePath.slice(1)
        : filePath;
      fullPath = `${baseUrl}/${cleanFilePath}/${fileMgmtCd}`;
    }

    imageUrl.value = fullPath;
    showImagePopup.value = true;
  }
};

// 사진 팝업 닫기
const closeImagePopup = () => {
  showImagePopup.value = false;
  imageUrl.value = '';
};

// 프린트 기능
const fnPrint = () => {
  if (!printArea.value) return;

  const printWindow = window.open('', '_blank', 'width=1200,height=800');
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
            background-color: #30796a;
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
.content-wrapper {
  flex: 1;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.main-content {
  display: flex;
  flex: 1;
  position: relative;
  min-height: 0;
}

.left-panel {
  flex: 1;
  padding: 20px;
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
  border-color: #30796a;
  color: #30796a;
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
  background-color: #30796a;
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
  background-color: #256b5a;
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
  background-color: #30796a;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background-color 0.2s ease;
}

.view-image-btn:hover {
  background-color: #256b5a;
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
  z-index: 1000;
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
