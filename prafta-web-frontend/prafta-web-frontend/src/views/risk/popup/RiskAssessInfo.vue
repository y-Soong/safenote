<template>
  <Transition name="fade">
    <div class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 🔹 Title  v-if="visible" -->
        <div class="modal-header" @mousedown="startDrag">
          <span>위험성 평가 정보</span>
          <button class="icon-button" @click="$emit('close')">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              class="w-6 h-6"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <!-- 메인 컨텐츠 -->
        <div class="risk-assess-content">
          <!-- 왼쪽: 개선 전 -->
          <div class="improvement-section before-section">
            <div class="section-header">개선 전</div>
            <div class="form-container">
              <div class="form-row">
                <label>작업명</label>
                <input v-model="formData.processNm" readonly />
              </div>
              <div class="form-row">
                <label>위험성구분</label>
                <input v-model="formData.processNm" readonly />
              </div>
              <div class="form-row">
                <label>위험성분류</label>
                <input v-model="formData.riskTypeNm" readonly />
              </div>
              <div class="form-row">
                <label>평가요청일자</label>
                <input v-model="formData.initAssessDate" readonly />
              </div>
              <div class="form-row">
                <label>평가요청자</label>
                <input v-model="formData.initAssessorNm" readonly />
              </div>
              <div class="form-row">
                <label>유해요인명</label>
                <input v-model="formData.hazardNm" readonly />
              </div>
              <div class="form-row">
                <label>유해요인설명</label>
                <textarea
                  v-model="formData.initDesc"
                  readonly
                  rows="3"
                ></textarea>
              </div>
              <div class="form-row">
                <label>사진</label>
                <div class="photo-container">
                  <img
                    v-if="beforePhotoUrl"
                    :src="beforePhotoUrl"
                    alt="개선 전 사진"
                    class="photo-preview"
                  />
                  <div v-else class="photo-placeholder">사진 없음</div>
                </div>
              </div>
              <div class="form-row risk-assessment">
                <label>개선 전 위험성 평가</label>
                <div class="risk-evaluation-group">
                  <div class="risk-input-item">
                    <label>빈도</label>
                    <select
                      v-model="formData.initLikelihoodScore"
                      :disabled="
                        props.riskAssessmentData.assessmentStatus != '001'
                      "
                    >
                      <option value="">선택</option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                      <option value="5">5</option>
                    </select>
                  </div>
                  <div class="risk-input-item">
                    <label>강도</label>
                    <select
                      v-model="formData.initSeverityScore"
                      :disabled="
                        props.riskAssessmentData.assessmentStatus != '001'
                      "
                    >
                      <option value="">선택</option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                    </select>
                  </div>
                  <div class="risk-input-item">
                    <label>위험도</label>
                    <div
                      class="risk-level-display"
                      :class="getRiskLevelClass(formData.initRiskLv)"
                    >
                      {{ formData.initRiskLv || "-" }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 오른쪽: 개선 후 -->
          <div class="improvement-section after-section">
            <div class="section-header">개선 후</div>
            <div class="form-container">
              <div class="form-row">
                <label>진행상태</label>
                <select v-model="formData.assessmentStatus" name="combo">
                  <option
                    v-for="opt in (systCodeArr['SYS011'] || []).filter(
                      (item) => {
                        if (!proxy.$util.isNotEmpty(item.systValDCd))
                          return false;
                        // props.assessmentStatus가 '001'이면 '001' 옵션 제외
                        if (
                          props.riskAssessmentData.assessmentStatus != '001' &&
                          item.systValDCd === '001'
                        ) {
                          return false;
                        }

                        if (
                          props.riskAssessmentData.assessmentStatus == '003' &&
                          (item.systValDCd == '001' ||
                            item.systValDCd == '002' ||
                            item.systValDCd == '004')
                        ) {
                          return false;
                        }

                        if (
                          props.riskAssessmentData.assessmentStatus == '004' &&
                          (item.systValDCd == '001' ||
                            item.systValDCd == '002' ||
                            item.systValDCd == '003')
                        ) {
                          return false;
                        }

                        return true;
                      }
                    )"
                    :key="opt.systValDCd"
                    :value="opt.systValDCd"
                  >
                    {{ opt.systValDNm }}
                  </option>
                </select>
              </div>
              <div
                class="form-row"
                v-if="
                  formData.assessmentStatus == '002' ||
                  formData.assessmentStatus == '003'
                "
              >
                <label>개선예정일자</label>
                <CalendarSrch v-model="formData.revalDate" />
                <!-- :readonly="readOnlyFlg" -->
              </div>
              <div
                class="form-row"
                v-if="
                  formData.assessmentStatus == '002' ||
                  formData.assessmentStatus == '003'
                "
              >
                <label>임시조치 내용</label>
                <textarea
                  v-model="formData.revalBeforeDesc"
                  placeholder="개선완료 전 임시조치 내용을 입력해 주세요"
                  rows="5"
                ></textarea>
              </div>
              <div class="form-row" v-if="formData.assessmentStatus == '003'">
                <label>개선완료일자</label>
                <input v-model="formData.revalAssessDate" readonly />
              </div>
              <div class="form-row" v-if="formData.assessmentStatus == '003'">
                <label>개선관리자</label>
                <input v-model="formData.revalAssessorNm" readonly />
              </div>
              <div class="form-row" v-if="formData.assessmentStatus == '003'">
                <label>개선내용</label>
                <textarea
                  v-model="formData.revalDesc"
                  placeholder="개선 내용을 입력해 주세요"
                  rows="5"
                ></textarea>
              </div>
              <div class="form-row" v-if="formData.assessmentStatus == '003'">
                <label>개선사진</label>
                <div class="file-upload-wrapper">
                  <input
                    type="file"
                    ref="fileInput"
                    accept="image/*"
                    @change="onFileSelected"
                    @input="onFileSelected"
                    style="display: none"
                  />
                  <button
                    type="button"
                    :class="[
                      'upload-button',
                      { 'upload-button-small': previewImage || revalPhotoUrl },
                    ]"
                    @click="fileInput?.click()"
                    style="margin-left: -0.5px"
                  >
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke-width="1.5"
                      stroke="currentColor"
                      class="upload-icon"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5"
                      />
                    </svg>
                    <span>사진 업로드</span>
                  </button>
                  <div
                    v-if="previewImage || revalPhotoUrl"
                    class="uploaded-photo-container"
                  >
                    <div class="photo-container">
                      <img
                        :src="previewImage ? previewImage.url : revalPhotoUrl"
                        alt="개선 후 사진"
                        class="photo-preview"
                      />
                      <button
                        type="button"
                        class="delete-photo-button"
                        @click="removePreview"
                        title="사진 삭제"
                      >
                        <svg
                          xmlns="http://www.w3.org/2000/svg"
                          fill="none"
                          viewBox="0 0 24 24"
                          stroke-width="1.5"
                          stroke="currentColor"
                          class="delete-icon"
                        >
                          <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            d="M6 18L18 6M6 6l12 12"
                          />
                        </svg>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
              <div
                class="form-row risk-assessment"
                v-if="formData.assessmentStatus == '003'"
              >
                <label>개선 후 위험성 평가</label>
                <div class="risk-evaluation-group">
                  <div class="risk-input-item">
                    <label>빈도</label>
                    <select v-model="formData.revalLikelihoodScore">
                      <option value="">선택</option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                      <option value="5">5</option>
                    </select>
                  </div>
                  <div class="risk-input-item">
                    <label>강도</label>
                    <select v-model="formData.revalSeverityScore">
                      <option value="">선택</option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                    </select>
                  </div>
                  <div class="risk-input-item">
                    <label>위험도</label>
                    <div
                      class="risk-level-display"
                      :class="getRiskLevelClass(formData.revalRiskLv)"
                    >
                      {{ formData.revalRiskLv || "-" }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 푸터 버튼 -->
        <div class="modal-footer">
          <div class="footer-buttons-left">
            <button
              class="btn btn-report"
              v-if="formData.assessmentStatus == '002'"
              @click="fnOpenImprovementPlan()"
            >
              개선실행계획서
            </button>
            <button
              class="btn btn-report"
              v-if="formData.assessmentStatus == '003'"
              @click="fnOpenImprovementReport()"
            >
              개선완료보고서
            </button>
          </div>
          <div class="footer-buttons-right">
            <button class="btn btn-cancel" @click="$emit('close')">취소</button>
            <button
              class="btn btn-save"
              v-if="
                props.riskAssessmentData.assessmentStatus != '003' &&
                props.riskAssessmentData.assessmentStatus != '004'
              "
              @click="fnSave()"
            >
              저장
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  watch,
  computed,
  onMounted,
  onBeforeUnmount,
  defineProps,
  defineEmits,
  getCurrentInstance,
} from 'vue';
import { useDraggable } from '@/composables/useDraggable';
import CalendarSrch from '@/components/common/CalendarSrch.vue';
import axios from '@/api/axios';

const systCodeArr = ref([]);
const fileInput = ref(null);
const previewImage = ref(null); // { file, url }

const { proxy } = getCurrentInstance();

onMounted(async () => {
  init();
  await fnGetSystinfoList();
});

const init = () => {
  const revalDate = props.riskAssessmentData.revalDate;

  if (proxy.$util.isEmpty(revalDate)) {
    formData.value.revalDate = proxy.$util.getToday();
  }
};

// API 호출
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get('/comApi/baseinfo/syst-info-list', {
      params: {
        systCodeList: ['SYS011'],
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

      // if (systCodeArr.value.SYS011 && systCodeArr.value.SYS011.length > 0) {
      //   formData.assessmentStatus = systCodeArr.value.SYS011[0].systValDCd;
      // }
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '조회 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

const props = defineProps({
  riskAssessmentData: {
    type: Object,
    default: () => ({}),
  },
  onSave: {
    type: Function,
    default: null,
  },
});

const emit = defineEmits(['close', 'save']);

const { position, startDrag } = useDraggable(
  window.innerWidth / 2 - 650,
  window.innerHeight / 10
);

// 폼 데이터
const formData = ref({
  // 기본 정보
  cmpnyCd: '',
  siteCd: '',
  processCd: '',
  processNm: '',
  riskTypeCd: '',
  riskTypeNm: '',
  hazardCd: '',
  hazardNm: '',
  assessmentCd: '',
  assessmentStatus: '',
  assessmentStatusNm: '',

  // 초기 평가 정보
  initLikelihoodScore: '',
  initSeverityScore: '',
  initRiskLv: '',
  initDesc: '',
  initAssessorId: '',
  initAssessorNm: '',
  initAssessDate: '',
  initFileMgmtCd: '',
  initFilePath: '',

  // 재평가 정보
  revalDate: '',
  revalBeforeDesc: '',
  revalRiskLv: '',
  revalLikelihoodScore: '',
  revalSeverityScore: '',
  revalAssessorId: '',
  revalAssessorNm: '',
  revalFileMgmtCd: '',
  revalFilePath: '',
  revalAssessDate: '',

  // 개선 전 사진
  beforePhoto: '',

  // 개선 후 정보
  revalDesc: '',
  afterFrequency: '',
  afterIntensity: '',
});

// 개선 전 사진 URL 생성 (initFilePath + initFileMgmtCd)
const beforePhotoUrl = computed(() => {
  if (formData.value.initFilePath && formData.value.initFileMgmtCd) {
    const apiBaseUrl = process.env.VUE_APP_API_BASE_URL || '';
    let fullPath = `${formData.value.initFilePath}/${formData.value.initFileMgmtCd}`;

    // API_BASE_URL이 있고 상대 경로인 경우에만 추가
    if (
      apiBaseUrl &&
      !formData.value.initFilePath.startsWith('http://') &&
      !formData.value.initFilePath.startsWith('https://')
    ) {
      const baseUrl = apiBaseUrl.endsWith('/')
        ? apiBaseUrl.slice(0, -1)
        : apiBaseUrl;
      const cleanFilePath = formData.value.initFilePath.startsWith('/')
        ? formData.value.initFilePath.slice(1)
        : formData.value.initFilePath;
      fullPath = `${baseUrl}/${cleanFilePath}/${formData.value.initFileMgmtCd}`;
    }

    return fullPath;
  }
  return null;
});

// 개선 후 사진 URL 생성 (revalFilePath 또는 revalFilePath + revalFileMgmtCd)
const revalPhotoUrl = computed(() => {
  const filePath = formData.value.revalFilePath || formData.value.revalFilePath;
  if (filePath && formData.value.revalFileMgmtCd) {
    const apiBaseUrl = process.env.VUE_APP_API_BASE_URL || '';
    let fullPath = `${filePath}/${formData.value.revalFileMgmtCd}`;

    // API_BASE_URL이 있고 상대 경로인 경우에만 추가
    if (
      apiBaseUrl &&
      !filePath.startsWith('http://') &&
      !filePath.startsWith('https://')
    ) {
      const baseUrl = apiBaseUrl.endsWith('/')
        ? apiBaseUrl.slice(0, -1)
        : apiBaseUrl;
      const cleanFilePath = filePath.startsWith('/')
        ? filePath.slice(1)
        : filePath;
      fullPath = `${baseUrl}/${cleanFilePath}/${formData.value.revalFileMgmtCd}`;
    }

    return fullPath;
  }
  return null;
});

// 위험도 계산 (빈도 × 강도)
const calculateRiskLevel = (frequency, intensity) => {
  if (!frequency || !intensity) return '';
  return String(Number(frequency) * Number(intensity));
};

watch(
  () => formData.value.assessmentStatus,
  (newVal) => {
    if (newVal == '003') {
      if (proxy.$util.isEmpty(formData.value.revalAssessorId)) {
        formData.value.revalAssessorId = sessionStorage.getItem('gv_userId');
        formData.value.revalAssessorNm = sessionStorage.getItem('gv_userNm');
      }

      if (proxy.$util.isEmpty(formData.value.revalLikelihoodScore)) {
        formData.value.revalLikelihoodScore =
          formData.value.initLikelihoodScore;
      }

      if (proxy.$util.isEmpty(formData.value.revalSeverityScore)) {
        formData.value.revalSeverityScore = formData.value.initSeverityScore;
      }
    } else {
      /* 개선완료 관련 데이터 초기화화 */
      // formData.value.revalAssessorId = '';
      // formData.value.revalAssessorNm = '';
      // formData.value.revalLikelihoodScore = '';
      // formData.value.revalSeverityScore = '';
      // formData.value.revalRiskLv = '';
      // formData.value.afterPhoto = '';
      // if (fileInput.value) {
      //   fileInput.value.value = ''; // 파일 입력 초기화
      // }
    }
  }
);

// 개선 전 빈도/강도 변경 감시
watch(
  () => [formData.value.initLikelihoodScore, formData.value.initSeverityScore],
  ([freq, intensity]) => {
    formData.value.initRiskLv = calculateRiskLevel(freq, intensity);
  }
);

// 개선 후 빈도/강도 변경 감시
watch(
  () => [
    formData.value.revalLikelihoodScore,
    formData.value.revalSeverityScore,
  ],
  ([freq, intensity]) => {
    formData.value.revalRiskLv = calculateRiskLevel(freq, intensity);
  }
);

// props 데이터가 변경되면 폼 데이터 업데이트
watch(
  () => props.riskAssessmentData,
  (newData) => {
    if (newData) {
      formData.value = {
        // 기본 정보
        cmpnyCd: newData.cmpnyCd || '',
        siteCd: newData.siteCd || '',
        processCd: newData.processCd || '',
        processNm: newData.processNm || '',
        riskTypeCd: newData.riskTypeCd || '',
        riskTypeNm: newData.riskTypeNm || '',
        hazardCd: newData.hazardCd || '',
        hazardNm: newData.hazardNm || '',
        assessmentCd: newData.assessmentCd || '',
        assessmentStatus: newData.assessmentStatus || '',
        assessmentStatusNm: newData.assessmentStatusNm || '',
        // 초기 평가 정보
        initLikelihoodScore: newData.initLikelihoodScore || '',
        initSeverityScore: newData.initSeverityScore || '',
        initRiskLv: newData.initRiskLv || '',
        initDesc: newData.initDesc || '',
        initAssessorId: newData.initAssessorId || '',
        initAssessorNm: newData.initAssessorNm || '',
        initAssessDate: newData.initAssessDate || '',
        initFileMgmtCd: newData.initFileMgmtCd || '',
        initFilePath: newData.initFilePath || '',
        // 재평가 정보
        revalDate: newData.revalDate || '',
        revalBeforeDesc: newData.revalBeforeDesc || '',
        revalLikelihoodScore: newData.revalLikelihoodScore || '',
        revalSeverityScore: newData.revalSeverityScore || '',
        revalRiskLv: newData.revalRiskLv || '',
        revalAssessorId: newData.revalAssessorId || '',
        revalAssessorNm: newData.revalAssessorNm || '',

        revalFileMgmtCd: newData.revalFileMgmtCd || '',
        revalFilePath: newData.revalFilePath || '',
        revalAssessDate: newData.revalAssessDate || '',
        // 개선 전 사진
        beforePhoto: newData.beforePhoto || '',
        // 개선 후 정보
        revalDesc: newData.revalDesc || '',
      };
    }
  },
  { immediate: true }
);

// 위험도에 따른 클래스 반환
const getRiskLevelClass = (riskLevel) => {
  const level = Number(riskLevel);
  if (level >= 13 && level <= 20) return 'risk-high'; // 높음 (빨간색)
  if (level >= 7 && level <= 12) return 'risk-medium'; // 보통 (주황색)
  if (level >= 4 && level <= 6) return 'risk-low'; // 낮음 (노란색)
  if (level >= 1 && level <= 3) return 'risk-very-low'; // 매우 낮음 (연두색)
  return '';
};

// 이미지 파일 확인 함수
const isImageFile = (file) =>
  file && typeof file.type === 'string' && file.type.startsWith('image/');

// 파일 선택 콜백
const onFileSelected = (evt) => {
  try {
    const input = evt?.target;
    const file = input?.files?.[0];

    if (!file) return;

    if (!isImageFile(file)) {
      proxy.$alert('이미지 파일만 선택해주세요.');
      if (input) input.value = '';
      return;
    }

    // 기존 프리뷰/URL 정리
    if (previewImage.value?.url) {
      URL.revokeObjectURL(previewImage.value.url);
    }

    const url = URL.createObjectURL(file);
    previewImage.value = { file, url };

    // 같은 파일 재선택 허용
    if (input) input.value = '';
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '파일을 읽는 중 오류가 발생했습니다.';

    proxy.$alert(msg);
  }
};

// 프리뷰 삭제
const removePreview = async () => {
  try {
    if (previewImage.value?.url) {
      URL.revokeObjectURL(previewImage.value.url);
    }
    previewImage.value = null;
    if (fileInput.value) {
      fileInput.value.value = '';
    }
  } catch (e) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '이미지 삭제 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

// 파일명 생성 함수
// const buildFileName = (prefix, originalName = 'photo.jpg') => {
//   const ts = new Date().toISOString().replace(/[:.]/g, '');
//   const safe = String(originalName).replace(/[^\w.\-]+/g, '_');
//   return `${prefix}_${ts}_${safe}`;
// };

// 저장 처리
const fnSave = async () => {
  const ok = await proxy.$confirm('저장하시겠습니까 ?');
  if (!ok) return;

  try {
    const saveData = new FormData();
    saveData.append('siteCd', props.riskAssessmentData.siteCd);
    saveData.append('assessmentCd', props.riskAssessmentData.assessmentCd);
    saveData.append('assessmentStatus', formData.value.assessmentStatus);
    saveData.append('processCd', props.riskAssessmentData.processCd);
    saveData.append(
      'initLikelihoodScore',
      formData.value.initLikelihoodScore || 0
    );
    saveData.append('initSeverityScore', formData.value.initSeverityScore || 0);
    saveData.append('initRiskLv', formData.value.initRiskLv || 0);

    saveData.append('revalDate', formData.value.revalDate);
    saveData.append('revalBeforeDesc', formData.value.revalBeforeDesc);
    saveData.append(
      'revalLikelihoodScore',
      formData.value.revalLikelihoodScore || 0
    );
    saveData.append(
      'revalSeverityScore',
      formData.value.revalSeverityScore || 0
    );
    saveData.append('revalRiskLv', formData.value.revalRiskLv || 0);
    saveData.append('revalDesc', formData.value.revalDesc);

    // 사진이 있으면 추가
    if (previewImage.value?.file) {
//      const fileName = buildFileName('risk', previewImage.value.file.name);
      const fileName = previewImage.value.file.name;
      saveData.append('item', previewImage.value.file, fileName);
    }

    const response = await axios.post(
      '/webApi/risk03/save-assessments',
      saveData
    );
    if (response.status === 200) {
      proxy.$alert('저장되었습니다.');
      // onSave 콜백이 있으면 호출
      if (props.onSave && typeof props.onSave === 'function') {
        props.onSave(response.data);
      }
      emit('close');
    } else {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        '저장 중 오류가 발생했습니다.';

      await proxy.$alert(msg);
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '저장 중 오류가 발생했습니다.';

    await proxy.$alert(msg);
  }
};

// 진행상태 이름 가져오기
const getAssessmentStatusName = (statusCd) => {
  if (!statusCd || !systCodeArr.value['SYS011']) return '';
  const status = systCodeArr.value['SYS011'].find(
    (item) => item.systValDCd === statusCd
  );
  return status ? status.systValDNm : '';
};

// 개선실행계획서 열기
const fnOpenImprovementPlan = () => {
  // 데이터 준비
  const processNm = formData.value.processNm || '-';
  const riskTypeNm = formData.value.riskTypeNm || '-';
  const initAssessDate = formData.value.initAssessDate || '-';
  const initAssessorNm = formData.value.initAssessorNm || '-';
  const hazardNm = formData.value.hazardNm || '-';
  const initDesc = (formData.value.initDesc || '-').replace(/\n/g, '<br>');
  const initLikelihoodScore = formData.value.initLikelihoodScore || '-';
  const initSeverityScore = formData.value.initSeverityScore || '-';
  const initRiskLv = formData.value.initRiskLv || '-';
  const riskLevelClass = getRiskLevelClass(formData.value.initRiskLv);
  const assessmentStatusName = getAssessmentStatusName(
    formData.value.assessmentStatus
  );
  const revalDate =
    formData.value.assessmentStatus == '002' ||
    formData.value.assessmentStatus == '003'
      ? formData.value.revalDate || '-'
      : '';
  const revalBeforeDesc =
    formData.value.assessmentStatus == '002' ||
    formData.value.assessmentStatus == '003'
      ? (formData.value.revalBeforeDesc || '-').replace(/\n/g, '<br>')
      : '';
  const photoHtml = beforePhotoUrl.value
    ? '<img src="' +
      beforePhotoUrl.value +
      '" alt="개선 전 사진" class="print-photo" />'
    : '사진 없음';
  const printDate = new Date().toLocaleString('ko-KR');

  // 프린트용 HTML 생성
  let printContent =
    '<!DOCTYPE html>' +
    '<html>' +
    '<head>' +
    '<meta charset="UTF-8">' +
    '<title>개선실행계획서</title>' +
    '<style>' +
    '@media print {' +
    '  @page { size: A4 landscape; margin: 5mm; }' +
    '  body { margin: 0; padding: 0; height: 100%; overflow: hidden; }' +
    '}' +
    'body {' +
    '  font-family: "Pretendard", sans-serif;' +
    '  font-size: 13px;' +
    '  line-height: 1.4;' +
    '  color: #333;' +
    '  padding: 10px 8px 5px 8px;' +
    '  height: 100%;' +
    '  display: flex;' +
    '  flex-direction: column;' +
    '  box-sizing: border-box;' +
    '}' +
    '.print-header {' +
    '  text-align: center;' +
    '  margin-bottom: 10px;' +
    '  border-bottom: 2px solid #333;' +
    '  padding-bottom: 6px;' +
    '  flex-shrink: 0;' +
    '}' +
    '.print-header h1 {' +
    '  font-size: 20px;' +
    '  font-weight: bold;' +
    '  margin: 0;' +
    '}' +
    '.print-content {' +
    '  display: flex;' +
    '  gap: 10px;' +
    '  margin-bottom: 2px;' +
    '  flex: 1;' +
    '  min-height: 0;' +
    '}' +
    '.print-section {' +
    '  flex: 1;' +
    '  border: 1px solid #ddd;' +
    '  border-radius: 4px;' +
    '  padding: 8px;' +
    '  display: flex;' +
    '  flex-direction: column;' +
    '  min-height: 0;' +
    '  overflow: hidden;' +
    '}' +
    '.section-title {' +
    '  background: #f5f5f5;' +
    '  padding: 6px;' +
    '  font-weight: bold;' +
    '  border-bottom: 1px solid #ddd;' +
    '  margin: -8px -8px 6px -8px;' +
    '  font-size: 14px;' +
    '  flex-shrink: 0;' +
    '}' +
    '.print-row {' +
    '  display: flex;' +
    '  margin-bottom: 3px;' +
    '  align-items: flex-start;' +
    '  flex-shrink: 0;' +
    '}' +
    '.print-label {' +
    '  flex: 0 0 120px;' +
    '  font-weight: 500;' +
    '  color: #333;' +
    '  font-size: 13px;' +
    '  padding-top: 5px;' +
    '}' +
    '.print-value {' +
    '  flex: 1;' +
    '  color: #666;' +
    '  font-size: 13px;' +
    '}' +
    '.print-textarea {' +
    '  min-height: 35px;' +
    '  white-space: pre-wrap;' +
    '  word-break: break-word;' +
    '  line-height: 1.2;' +
    '}' +
    '.print-photo {' +
    '  max-width: 100%;' +
    '  max-height: 144px;' +
    '  border: 1px solid #ddd;' +
    '  padding: 3px;' +
    '}' +
    '.risk-evaluation {' +
    '  display: flex;' +
    '  gap: 10px;' +
    '  align-items: center;' +
    '}' +
    '.risk-item {' +
    '  display: flex;' +
    '  align-items: center;' +
    '  gap: 5px;' +
    '}' +
    '.risk-item-label {' +
    '  font-weight: 500;' +
    '  font-size: 13px;' +
    '}' +
    '.risk-level {' +
    '  padding: 3px 8px;' +
    '  border-radius: 4px;' +
    '  font-weight: bold;' +
    '  min-width: 35px;' +
    '  text-align: center;' +
    '  font-size: 13px;' +
    '}' +
    '.risk-high { background: #ff6b6b; color: white; }' +
    '.risk-medium { background: #ffa500; color: white; }' +
    '.risk-low { background: #ffd700; color: #333; }' +
    '.risk-very-low { background: #90ee90; color: #333; }' +
    '.print-date {' +
    '  text-align: right;' +
    '  margin-top: auto;' +
    '  padding-top: 3px;' +
    '  font-size: 11px;' +
    '  color: #666;' +
    '  flex-shrink: 0;' +
    '}' +
    '</style>' +
    '</head>' +
    '<body>' +
    '<div class="print-header">' +
    '<h1>개선실행계획서</h1>' +
    '</div>' +
    '<div class="print-content">' +
    '<div class="print-section">' +
    '<div class="section-title">개선 전</div>' +
    '<div class="print-row">' +
    '<div class="print-label">작업명</div>' +
    '<div class="print-value">' +
    processNm +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">위험성구분</div>' +
    '<div class="print-value">' +
    processNm +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">위험성분류</div>' +
    '<div class="print-value">' +
    riskTypeNm +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">평가요청일자</div>' +
    '<div class="print-value">' +
    initAssessDate +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">평가요청자</div>' +
    '<div class="print-value">' +
    initAssessorNm +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">유해요인명</div>' +
    '<div class="print-value">' +
    hazardNm +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">유해요인설명</div>' +
    '<div class="print-value print-textarea">' +
    initDesc +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">사진</div>' +
    '<div class="print-value">' +
    photoHtml +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">개선 전 위험성 평가</div>' +
    '<div class="print-value">' +
    '<div class="risk-evaluation">' +
    '<div class="risk-item">' +
    '<span class="risk-item-label">빈도:</span>' +
    '<span>' +
    initLikelihoodScore +
    '</span>' +
    '</div>' +
    '<div class="risk-item">' +
    '<span class="risk-item-label">강도:</span>' +
    '<span>' +
    initSeverityScore +
    '</span>' +
    '</div>' +
    '<div class="risk-item">' +
    '<span class="risk-item-label">위험도:</span>' +
    '<span class="risk-level ' +
    riskLevelClass +
    '">' +
    initRiskLv +
    '</span>' +
    '</div>' +
    '</div>' +
    '</div>' +
    '</div>' +
    '</div>' +
    '<div class="print-section">' +
    '<div class="section-title">개선 후</div>' +
    '<div class="print-row">' +
    '<div class="print-label">진행상태</div>' +
    '<div class="print-value">' +
    assessmentStatusName +
    '</div>' +
    '</div>';

  // 개선예정일자와 임시조치 내용 추가
  if (
    formData.value.assessmentStatus == '002' ||
    formData.value.assessmentStatus == '003'
  ) {
    printContent =
      printContent +
      '<div class="print-row">' +
      '<div class="print-label">개선예정일자</div>' +
      '<div class="print-value">' +
      revalDate +
      '</div>' +
      '</div>' +
      '<div class="print-row">' +
      '<div class="print-label">임시조치 내용</div>' +
      '<div class="print-value print-textarea">' +
      revalBeforeDesc +
      '</div>' +
      '</div>';
  }

  printContent =
    printContent +
    '</div>' +
    '</div>' +
    '<div class="print-date">' +
    '출력일시: ' +
    printDate +
    '</div>' +
    '</body>' +
    '</html>';

  // 새 창 열기
  const printWindow = window.open('', '_blank');
  if (printWindow) {
    printWindow.document.write(printContent);
    printWindow.document.close();
    // 프린트 대화상자 열기
    setTimeout(() => {
      printWindow.print();
      // 프린트 후 창 닫기
      printWindow.addEventListener('afterprint', () => {
        printWindow.close();
      });
    }, 250);
  } else {
    proxy.$alert(
      '팝업이 차단되어 있습니다. 브라우저 설정에서 팝업을 허용해주세요.'
    );
  }
};

// 개선완료보고서 열기
const fnOpenImprovementReport = () => {
  // 데이터 준비
  const processNm = formData.value.processNm || '-';
  const riskTypeNm = formData.value.riskTypeNm || '-';
  const initAssessDate = formData.value.initAssessDate || '-';
  const initAssessorNm = formData.value.initAssessorNm || '-';
  const hazardNm = formData.value.hazardNm || '-';
  const initDesc = (formData.value.initDesc || '-').replace(/\n/g, '<br>');
  const initLikelihoodScore = formData.value.initLikelihoodScore || '-';
  const initSeverityScore = formData.value.initSeverityScore || '-';
  const initRiskLv = formData.value.initRiskLv || '-';
  const initRiskLevelClass = getRiskLevelClass(formData.value.initRiskLv);
  const assessmentStatusName = getAssessmentStatusName(
    formData.value.assessmentStatus
  );
  const revalDate = formData.value.revalDate || '-';
  const revalBeforeDesc = (formData.value.revalBeforeDesc || '-').replace(
    /\n/g,
    '<br>'
  );
  const revalAssessorNm = formData.value.revalAssessorNm || '-';
  const revalAssessDate = formData.value.revalAssessDate || '-';
  const revalDesc = (formData.value.revalDesc || '-').replace(/\n/g, '<br>');
  const revalLikelihoodScore = formData.value.revalLikelihoodScore || '-';
  const revalSeverityScore = formData.value.revalSeverityScore || '-';
  const revalRiskLv = formData.value.revalRiskLv || '-';
  const revalRiskLevelClass = getRiskLevelClass(formData.value.revalRiskLv);
  const beforePhotoHtml = beforePhotoUrl.value
    ? '<img src="' +
      beforePhotoUrl.value +
      '" alt="개선 전 사진" class="print-photo" />'
    : '사진 없음';
  const afterPhotoHtml =
    previewImage?.value?.url || revalPhotoUrl.value
      ? '<img src="' +
        (previewImage?.value?.url || revalPhotoUrl.value) +
        '" alt="개선 후 사진" class="print-photo" />'
      : '사진 없음';
  const printDate = new Date().toLocaleString('ko-KR');

  // 프린트용 HTML 생성
  let printContent =
    '<!DOCTYPE html>' +
    '<html>' +
    '<head>' +
    '<meta charset="UTF-8">' +
    '<title>개선완료보고서</title>' +
    '<style>' +
    '@media print {' +
    '  @page { size: A4 landscape; margin: 5mm; }' +
    '  body { margin: 0; padding: 0; height: 100%; overflow: hidden; }' +
    '}' +
    'body {' +
    '  font-family: "Pretendard", sans-serif;' +
    '  font-size: 13px;' +
    '  line-height: 1.4;' +
    '  color: #333;' +
    '  padding: 10px 8px 5px 8px;' +
    '  height: 100%;' +
    '  display: flex;' +
    '  flex-direction: column;' +
    '  box-sizing: border-box;' +
    '}' +
    '.print-header {' +
    '  text-align: center;' +
    '  margin-bottom: 10px;' +
    '  border-bottom: 2px solid #333;' +
    '  padding-bottom: 6px;' +
    '  flex-shrink: 0;' +
    '}' +
    '.print-header h1 {' +
    '  font-size: 20px;' +
    '  font-weight: bold;' +
    '  margin: 0;' +
    '}' +
    '.print-content {' +
    '  display: flex;' +
    '  gap: 10px;' +
    '  margin-bottom: 5px;' +
    '  flex: 1;' +
    '  min-height: 0;' +
    '}' +
    '.print-section {' +
    '  flex: 1;' +
    '  border: 1px solid #ddd;' +
    '  border-radius: 4px;' +
    '  padding: 8px;' +
    '  display: flex;' +
    '  flex-direction: column;' +
    '  min-height: 0;' +
    '  overflow: hidden;' +
    '}' +
    '.section-title {' +
    '  background: #f5f5f5;' +
    '  padding: 6px;' +
    '  font-weight: bold;' +
    '  border-bottom: 1px solid #ddd;' +
    '  margin: -8px -8px 6px -8px;' +
    '  font-size: 14px;' +
    '  flex-shrink: 0;' +
    '}' +
    '.print-row {' +
    '  display: flex;' +
    '  margin-bottom: 8px;' +
    '  align-items: flex-start;' +
    '  flex-shrink: 0;' +
    '}' +
    '.print-label {' +
    '  flex: 0 0 120px;' +
    '  font-weight: 500;' +
    '  color: #333;' +
    '  font-size: 13px;' +
    '  padding-top: 5px;' +
    '}' +
    '.print-value {' +
    '  flex: 1;' +
    '  color: #666;' +
    '  font-size: 13px;' +
    '  padding-top: 5px;' +
    '}' +
    '.print-textarea {' +
    '  min-height: 35px;' +
    '  white-space: pre-wrap;' +
    '  word-break: break-word;' +
    '  line-height: 1.2;' +
    '}' +
    '.print-photo {' +
    '  max-width: 100%;' +
    '  max-height: 144px;' +
    '  border: 1px solid #ddd;' +
    '}' +
    '.risk-evaluation {' +
    '  display: flex;' +
    '  gap: 10px;' +
    '  align-items: center;' +
    '  margin-top: -3px;' +
    '}' +
    '.risk-item {' +
    '  display: flex;' +
    '  align-items: center;' +
    '  gap: 5px;' +
    '}' +
    '.risk-item-label {' +
    '  font-weight: 500;' +
    '  font-size: 13px;' +
    '}' +
    '.risk-level {' +
    '  padding: 3px 8px;' +
    '  border-radius: 4px;' +
    '  font-weight: bold;' +
    '  min-width: 35px;' +
    '  text-align: center;' +
    '  font-size: 13px;' +
    '}' +
    '.risk-high { background: #ff6b6b; color: white; }' +
    '.risk-medium { background: #ffa500; color: white; }' +
    '.risk-low { background: #ffd700; color: #333; }' +
    '.risk-very-low { background: #90ee90; color: #333; }' +
    '.print-date {' +
    '  text-align: right;' +
    '  margin-top: auto;' +
    '  padding-top: 5px;' +
    '  font-size: 11px;' +
    '  color: #666;' +
    '  flex-shrink: 0;' +
    '}' +
    '</style>' +
    '</head>' +
    '<body>' +
    '<div class="print-header">' +
    '<h1>개선완료보고서</h1>' +
    '</div>' +
    '<div class="print-content">' +
    '<div class="print-section">' +
    '<div class="section-title">개선 전</div>' +
    '<div class="print-row">' +
    '<div class="print-label">작업명</div>' +
    '<div class="print-value">' +
    processNm +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">위험성구분</div>' +
    '<div class="print-value">' +
    processNm +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">위험성분류</div>' +
    '<div class="print-value">' +
    riskTypeNm +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">평가요청일자</div>' +
    '<div class="print-value">' +
    initAssessDate +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">평가요청자</div>' +
    '<div class="print-value">' +
    initAssessorNm +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">유해요인명</div>' +
    '<div class="print-value">' +
    hazardNm +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">유해요인설명</div>' +
    '<div class="print-value print-textarea">' +
    initDesc +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">사진</div>' +
    '<div class="print-value">' +
    beforePhotoHtml +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">개선 전 위험성 평가</div>' +
    '<div class="print-value">' +
    '<div class="risk-evaluation">' +
    '<div class="risk-item">' +
    '<span class="risk-item-label">빈도:</span>' +
    '<span>' +
    initLikelihoodScore +
    '</span>' +
    '</div>' +
    '<div class="risk-item">' +
    '<span class="risk-item-label">강도:</span>' +
    '<span>' +
    initSeverityScore +
    '</span>' +
    '</div>' +
    '<div class="risk-item">' +
    '<span class="risk-item-label">위험도:</span>' +
    '<span class="risk-level ' +
    initRiskLevelClass +
    '">' +
    initRiskLv +
    '</span>' +
    '</div>' +
    '</div>' +
    '</div>' +
    '</div>' +
    '</div>' +
    '<div class="print-section">' +
    '<div class="section-title">개선 후</div>' +
    '<div class="print-row">' +
    '<div class="print-label">진행상태</div>' +
    '<div class="print-value">' +
    assessmentStatusName +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">개선예정일자</div>' +
    '<div class="print-value">' +
    revalDate +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">임시조치 내용</div>' +
    '<div class="print-value print-textarea">' +
    revalBeforeDesc +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">개선관리자</div>' +
    '<div class="print-value">' +
    revalAssessorNm +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">개선완료일자</div>' +
    '<div class="print-value">' +
    revalAssessDate +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">개선내용</div>' +
    '<div class="print-value print-textarea">' +
    revalDesc +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">개선사진</div>' +
    '<div class="print-value">' +
    afterPhotoHtml +
    '</div>' +
    '</div>' +
    '<div class="print-row">' +
    '<div class="print-label">개선 후 위험성 평가</div>' +
    '<div class="print-value">' +
    '<div class="risk-evaluation">' +
    '<div class="risk-item">' +
    '<span class="risk-item-label">빈도:</span>' +
    '<span>' +
    revalLikelihoodScore +
    '</span>' +
    '</div>' +
    '<div class="risk-item">' +
    '<span class="risk-item-label">강도:</span>' +
    '<span>' +
    revalSeverityScore +
    '</span>' +
    '</div>' +
    '<div class="risk-item">' +
    '<span class="risk-item-label">위험도:</span>' +
    '<span class="risk-level ' +
    revalRiskLevelClass +
    '">' +
    revalRiskLv +
    '</span>' +
    '</div>' +
    '</div>' +
    '</div>' +
    '</div>' +
    '</div>' +
    '</div>' +
    '<div class="print-date">' +
    '출력일시: ' +
    printDate +
    '</div>' +
    '</body>' +
    '</html>';

  // 새 창 열기
  const printWindow = window.open('', '_blank');
  if (printWindow) {
    printWindow.document.write(printContent);
    printWindow.document.close();
    // 프린트 대화상자 열기
    setTimeout(() => {
      printWindow.print();
      // 프린트 후 창 닫기
      printWindow.addEventListener('afterprint', () => {
        printWindow.close();
      });
    }, 250);
  } else {
    proxy.$alert(
      '팝업이 차단되어 있습니다. 브라우저 설정에서 팝업을 허용해주세요.'
    );
  }
};

// 정리
onBeforeUnmount(() => {
  if (previewImage.value?.url) {
    URL.revokeObjectURL(previewImage.value.url);
  }
});
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content-wide {
  width: 100%;
  max-width: 1400px;
  max-height: 70vh;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
}

.risk-assess-content {
  display: flex;
  flex: 1;
  overflow: hidden;
  padding: 1.5rem;
  gap: 1.5rem;
  min-height: 0;
}

.improvement-section {
  flex: 1;
  border: 1px solid #ddd;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.section-header {
  background: #f5f5f5;
  padding: 0.75rem 1rem;
  font-weight: bold;
  border-bottom: 1px solid #ddd;
}

.form-container {
  flex: 1;
  padding: 1rem;
  /* display: flex; */
  flex-direction: column;
  gap: 1rem;
  overflow-y: auto;
  overflow-x: hidden;
  min-height: 0;
  align-items: flex-start;
  text-align: left;
}

.form-row {
  display: flex !important;
  align-items: center !important;
  gap: 0.5rem;
  text-align: left;
  margin-bottom: 0.5rem;
}

.form-row label {
  flex: 0 0 120px;
  font-weight: 500;
  font-size: 0.9rem;
  text-align: left;
  color: #333;
}

.form-row input,
.form-row select,
.form-row textarea {
  flex: 1 1 auto;
  padding: 0.4rem 0.6rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.9rem;
  text-align: left;
  box-sizing: border-box;
}

.form-row input:read-only {
  background: #f5f5f5;
  cursor: not-allowed;
}

.form-row textarea {
  resize: vertical;
  min-height: 100px;
}

.photo-row {
  flex-direction: column !important;
  align-items: center !important;
  flex: 1;
  min-height: 200px;
}

.photo-row label {
  flex: 0 0 auto;
  margin-bottom: 0.5rem;
  /* align-self: flex-start; */
}

/* .photo-row .photo-container {
  width: 80%;
  max-width: 80%;
  min-width: 80%;
} */

.photo-container {
  flex: 1;
  border: 1px solid #ddd;
  border-radius: 4px;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  background: #f9f9f9;
  min-height: 200px;
  padding: 0.5rem;
  position: relative;
}

.photo-container .photo-placeholder {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.photo-preview {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.photo-placeholder {
  color: #999;
  font-size: 0.9rem;
  max-width: 100%;
  max-height: 100%;
}

.risk-assessment {
  margin-top: 0;
}

.risk-evaluation-group {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex: 1 1 auto;
}

.risk-input-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 0 0 auto;
}

.risk-input-item label {
  font-size: 0.85rem;
  font-weight: 500;
  flex: 0 0 auto;
  white-space: nowrap;
  color: #333;
}

.risk-input-item select {
  padding: 0.4rem 0.6rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.9rem;
  text-align: left;
  box-sizing: border-box;
  min-width: 80px;
}

.risk-level-display {
  padding: 0.4rem 0.6rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  text-align: center;
  font-weight: bold;
  min-height: 2.2rem;
  min-width: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  font-size: 0.9rem;
}

.risk-high {
  background: #ff6b6b;
  color: white;
}

.risk-medium {
  background: #ffa500;
  color: white;
}

.risk-low {
  background: #ffd700;
  color: #333;
}

.risk-very-low {
  background: #90ee90;
  color: #333;
}

.readonly-input {
  background-color: #f5f5f5;
  cursor: not-allowed;
  border: 1px solid #ddd;
  padding: 0.5rem;
  border-radius: 4px;
  width: 100%;
  box-sizing: border-box;
}

.textarea-row {
  flex-direction: column !important;
  align-items: flex-start !important;
  flex: 1;
}

.textarea-row label {
  flex: 0 0 auto;
  margin-bottom: 0.5rem;
}

.textarea-row textarea {
  width: 100%;
}

.photo-upload-row {
  flex-direction: column !important;
  align-items: flex-start !important;
  flex: 1;
}

.photo-upload-row label {
  flex: 0 0 auto;
  margin-bottom: 0.5rem;
}

.photo-upload-row .file-upload-container {
  width: 100%;
}

.file-upload-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: flex-start;
  flex: 1 1 auto;
  width: 100%;
}

.file-upload-container {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: flex-start;
  flex: 1 1 auto;
  width: 100%;
}

.upload-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1rem;
  border: 2px dashed #ccc;
  border-radius: 4px;
  background: #f9f9f9;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  width: 100%;
  box-sizing: border-box;
  min-height: 60px;
}

.upload-button-small {
  padding: 0.5rem;
  min-height: 30px;
}

.upload-button-small .upload-icon {
  width: 18px;
  height: 18px;
}

.upload-button-small span {
  font-size: 0.85rem;
}

.upload-button span {
  white-space: nowrap;
}

.upload-button:hover {
  border-color: #30796a;
  background: #f0f0f0;
}

.upload-icon {
  width: 24px;
  height: 24px;
  color: #666;
  transition: all 0.2s;
}

.uploaded-photo-container {
  width: 100%;
  margin-top: 0.5rem;
}

.uploaded-photo-container .photo-container {
  position: relative;
  flex: 1;
  border: 1px solid #ddd;
  border-radius: 4px;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  background: #f9f9f9;
  min-height: 200px;
  padding: 0.5rem;
  width: 100%;
  box-sizing: border-box;
}

.uploaded-photo-container .photo-preview {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  width: 100%;
  height: auto;
}

.delete-photo-button {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  z-index: 10;
}

.delete-photo-button:hover {
  background: rgba(220, 53, 69, 0.8);
  transform: scale(1.1);
}

.delete-icon {
  width: 16px;
  height: 16px;
  color: white;
  stroke-width: 2.5;
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid #ddd;
  background: #f9f9f9;
}

.footer-buttons-left {
  display: flex;
  gap: 0.5rem;
}

.footer-buttons-right {
  display: flex;
  gap: 0.5rem;
}

.btn {
  padding: 0.5rem 1.5rem;
  border: none;
  border-radius: 4px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel {
  background: white;
  color: #666;
  border: 1px solid #ccc;
}

.btn-cancel:hover {
  background: #f5f5f5;
}

.btn-save {
  background: #30796a;
  color: white;
}

.btn-save:hover {
  background: #256b5a;
}

.btn-report {
  background: #4a90e2;
  color: white;
}

.btn-report:hover {
  background: #357abd;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
