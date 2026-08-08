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
          <span>지속평가대상 관리</span>
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

        <!-- 본문: 좌(위험상황) / 우(개선항목 N건) -->
        <div class="risk-assess-content">
          <!-- 좌측: 위험상황 (개선 전 = 읽기) -->
          <div class="improvement-section before-section">
            <div class="section-header">위험상황</div>
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
                    alt="위험상황 사진"
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
                    <div class="risk-readonly">
                      {{ formData.initLikelihoodScore || "-" }}
                    </div>
                  </div>
                  <div class="risk-input-item">
                    <label>강도</label>
                    <div class="risk-readonly">
                      {{ formData.initSeverityScore || "-" }}
                    </div>
                  </div>
                  <div class="risk-input-item">
                    <label>위험도</label>
                    <div
                      class="risk-level-display"
                      :class="riskLevelClass(formData.initRiskLv)"
                    >
                      {{ formData.initRiskLv || "-" }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 우측: 개선항목 (N건 동적) -->
          <div class="improvement-section after-section">
            <div class="section-header improvement-header">
              <span>개선항목</span>
              <button
                type="button"
                class="btn btn-report"
                @click="addImprovementItem"
              >
                + 항목 추가
              </button>
            </div>
            <div class="form-container">
              <!-- empty -->
              <div v-if="improvementItems.length === 0" class="improve-empty">
                항목 추가 버튼으로 개선항목을 등록하세요.
              </div>

              <!-- 개선항목 카드 N건 (신규 항목이 상단) -->
              <div
                v-for="(item, idx) in improvementItems"
                :key="item.uid"
                class="improve-card"
              >
                <div class="improve-card__head">
                  <span class="improve-card__title"
                    >개선항목 #{{ improvementItems.length - idx }}</span
                  >
                  <button
                    type="button"
                    class="btn btn-cancel improve-card__del"
                    @click="removeImprovementItem(idx)"
                  >
                    삭제
                  </button>
                </div>

                <div class="form-row">
                  <label>개선일자</label>
                  <CalendarSrch v-model="item.improveDate" />
                </div>
                <div class="form-row">
                  <label>개선내용</label>
                  <textarea
                    v-model="item.improveDesc"
                    placeholder="개선 내용을 입력해 주세요"
                    rows="3"
                  ></textarea>
                </div>
                <div class="form-row">
                  <label>개선사진</label>
                  <div class="file-upload-wrapper">
                    <input
                      type="file"
                      :ref="(el) => setFileRef(el, idx)"
                      accept="image/*"
                      style="display: none"
                      @change="(e) => onFileSelected(e, idx)"
                    />
                    <button
                      type="button"
                      :class="[
                        'upload-button',
                        {
                          'upload-button-small':
                            item.previewUrl || item.fileMgmtCd,
                        },
                      ]"
                      style="margin-left: -0.5px"
                      @click="openFilePicker(idx)"
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
                      v-if="item.previewUrl"
                      class="uploaded-photo-container"
                    >
                      <div class="photo-container">
                        <img
                          :src="item.previewUrl"
                          alt="개선 후 사진"
                          class="photo-preview"
                        />
                        <button
                          type="button"
                          class="delete-photo-button"
                          @click="removePreview(idx)"
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
                <div class="form-row risk-assessment">
                  <label>개선 후 위험성 평가</label>
                  <div class="risk-evaluation-group">
                    <div class="risk-input-item">
                      <label>빈도</label>
                      <select
                        v-model="item.likelihoodScore"
                        @change="recalcItemRiskLv(item)"
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
                        v-model="item.severityScore"
                        @change="recalcItemRiskLv(item)"
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
                        :class="riskLevelClass(item.riskLv)"
                      >
                        {{ item.riskLv || "-" }}
                      </div>
                    </div>
                  </div>
                </div>

                <div class="improve-card__foot">
                  <!-- 항목 단위 저장(save-item). 신규/수정 모두 upsert -->
                  <button
                    type="button"
                    class="btn btn-save"
                    @click="saveImprovementItem(idx)"
                  >
                    항목 저장
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 푸터 (F-10 규약: 왼쪽=진행/확정(저장·개선완료), 오른쪽=이탈(취소)) -->
        <div class="modal-footer">
          <div class="footer-buttons-left">
            <button class="btn btn-save" @click="saveAll">저장</button>
            <!-- 개선완료: 005→003 전이, 개선 후 위험도 매우낮음(1-3) 가드는 BE/FE 공동 -->
            <button class="btn btn-report" @click="completeImprovement">
              개선완료
            </button>
          </div>
          <div class="footer-buttons-right">
            <button class="btn btn-cancel" @click="$emit('close')">취소</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, onMounted, onBeforeUnmount, getCurrentInstance, defineProps, defineEmits } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { readFileAsBase64 } from "@/utils/fileUtil";
import { getRiskLevelClass6, isVeryLow } from "@/utils/riskLevel";
import { buildFileServingUrl } from "@/utils/fileUrl";

const { proxy } = getCurrentInstance();

// 컴포넌트 계약(§A): 평가키 + 사업장 식별을 props 로 받는다.
const props = defineProps({
  // 위험성평가 식별/표시 데이터 (Risk_03 행 데이터 그대로 전달)
  riskAssessmentData: {
    type: Object,
    default: () => ({}),
  },
});

// close: 닫기 / saved: 개선항목 저장 후 목록 갱신 신호 / completed: 개선완료(003 전이) 후 신호
const emit = defineEmits(["close", "saved", "completed"]);

const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// 좌측 위험상황(읽기) 폼 — props 로 채움
const formData = ref({
  cmpnyCd: "",
  siteCd: "",
  processCd: "",
  processNm: "",
  riskTypeNm: "",
  hazardNm: "",
  assessmentCd: "",
  initAssessDate: "",
  initAssessorNm: "",
  initDesc: "",
  initLikelihoodScore: "",
  initSeverityScore: "",
  initRiskLv: "",
  initFileMgmtCd: "",
  initFilePath: "",
});

// 우측 개선항목 N건 (신규 항목이 배열 선두 = 화면 상단)
const improvementItems = ref([]);

// 개선 전 사진 URL
const beforePhotoUrl = ref(null);

// 파일 input 참조 맵 (인덱스별)
const fileRefs = ref({});
const setFileRef = (el, idx) => {
  if (el) fileRefs.value[idx] = el;
};

// 위험도 6단계 클래스 (공용 유틸 위임 — D1 단일 출처)
const riskLevelClass = (riskLv) => getRiskLevelClass6(riskLv);

// 평가키 (API 공통 파라미터)
const assessKeys = () => ({
  siteCd: props.riskAssessmentData.siteCd,
  processCd: props.riskAssessmentData.processCd,
  assessmentCd: props.riskAssessmentData.assessmentCd,
});

// 위험도 산출 (빈도 × 강도)
const calcRiskLv = (frequency, intensity) => {
  if (!frequency || !intensity) return "";
  return String(Number(frequency) * Number(intensity));
};

// 파일 경로 + 파일관리코드 → 표시 URL 조합.
//   공용 유틸(buildFileServingUrl)로 일원화 — 동일 출처 상대경로 조립이라 터널/도메인 경유에도 동작.
const buildFileUrl = (filePath, fileMgmtCd) =>
  buildFileServingUrl(filePath, fileMgmtCd) || null;

// 신규 개선항목 1건 생성
const blankItem = () => ({
  uid: `new-${Date.now()}-${Math.random().toString(36).slice(2)}`,
  improvementSeq: null, // null = 미저장 신규
  improveDate: proxy.$util.getToday(),
  improveDesc: "",
  fileMgmtCd: "",
  filePath: "",
  previewFile: null,
  previewUrl: "",
  likelihoodScore: "",
  severityScore: "",
  riskLv: "",
});

// 항목 추가(상단 삽입)
const addImprovementItem = () => {
  improvementItems.value.unshift(blankItem());
};

// 항목 제거 (미저장 = 배열에서만 제거 / 기저장 = delete-item 호출)
const removeImprovementItem = async (idx) => {
  const item = improvementItems.value[idx];
  if (!item) return;

  // 미저장(신규)면 배열에서만 제거 + 프리뷰 URL 정리
  if (item.improvementSeq == null) {
    if (item.previewUrl) URL.revokeObjectURL(item.previewUrl);
    improvementItems.value.splice(idx, 1);
    return;
  }

  const ok = await proxy.$confirm("이 개선항목을 삭제할까요?");
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/riskimpr01/delete-item",
      { ...assessKeys(), improvementSeq: item.improvementSeq },
      { headers: { "Content-Type": "application/json" } }
    );
    if (response.status === 200) {
      await loadImprovementItems();
      emit("saved");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "삭제 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 파일 선택기 열기
const openFilePicker = (idx) => {
  fileRefs.value[idx]?.click();
};

// 이미지 파일 검증
const isImageFile = (file) =>
  file && typeof file.type === "string" && file.type.startsWith("image/");

// 파일 선택 콜백 (이미지 검증 + objectURL 프리뷰)
const onFileSelected = (evt, idx) => {
  try {
    const input = evt?.target;
    const file = input?.files?.[0];
    if (!file) return;

    if (!isImageFile(file)) {
      proxy.$alert("이미지 파일만 업로드할 수 있습니다.");
      if (input) input.value = "";
      return;
    }

    const item = improvementItems.value[idx];
    if (!item) return;

    // 기존 프리뷰 URL 정리
    if (item.previewUrl) URL.revokeObjectURL(item.previewUrl);

    item.previewFile = file;
    item.previewUrl = URL.createObjectURL(file);

    // 같은 파일 재선택 허용
    if (input) input.value = "";
  } catch (err) {
    const msg = err?.message || "파일을 읽는 중 오류가 발생했습니다.";
    proxy.$alert(msg);
  }
};

// 프리뷰 삭제 (신규 선택분만 취소. 기저장 사진 자체 삭제는 항목 삭제로 처리)
const removePreview = (idx) => {
  const item = improvementItems.value[idx];
  if (!item) return;
  if (item.previewUrl) URL.revokeObjectURL(item.previewUrl);
  item.previewFile = null;
  item.previewUrl = "";
};

// 개선항목 위험도 자동 산출 (빈도/강도 변경 시 호출 — 템플릿 select @change 대신 저장 직전/표시 시 계산)
const recalcItemRiskLv = (item) => {
  item.riskLv = calcRiskLv(item.likelihoodScore, item.severityScore);
};

// 개선항목 단건 저장(upsert)
const saveImprovementItem = async (idx) => {
  const item = improvementItems.value[idx];
  if (!item) return;

  // 위험도 최신화
  recalcItemRiskLv(item);

  if (proxy.$util.isEmpty(item.likelihoodScore) || proxy.$util.isEmpty(item.severityScore)) {
    await proxy.$alert("개선 후 빈도와 강도를 선택해 주세요.");
    return;
  }

  try {
    const requestBody = {
      ...assessKeys(),
      improvementSeq: item.improvementSeq, // null=신규 / 값=수정
      improveDate: item.improveDate,
      improveDesc: item.improveDesc,
      likelihoodScore: Number(item.likelihoodScore),
      severityScore: Number(item.severityScore),
      riskLv: item.riskLv,
    };

    if (item.previewFile) {
      requestBody.itemBase64 = await readFileAsBase64(item.previewFile);
      requestBody.itemOriginalFilename = item.previewFile.name;
    }

    const response = await axios.post("/webApi/riskimpr01/save-item", requestBody, {
      headers: { "Content-Type": "application/json" },
    });

    if (response.status === 200) {
      await proxy.$alert("저장되었습니다.");
      await loadImprovementItems();
      emit("saved");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 전체 저장(빈도/강도 입력된 모든 항목 일괄 upsert)
const saveAll = async () => {
  // 위험도 최신화 + 입력된 항목만 대상
  const targets = [];
  improvementItems.value.forEach((item, idx) => {
    recalcItemRiskLv(item);
    if (proxy.$util.isNotEmpty(item.likelihoodScore) && proxy.$util.isNotEmpty(item.severityScore)) {
      targets.push(idx);
    }
  });

  if (targets.length === 0) {
    await proxy.$alert("저장할 개선항목이 없습니다. 빈도와 강도를 입력해 주세요.");
    return;
  }

  const ok = await proxy.$confirm("입력한 개선항목을 모두 저장할까요?");
  if (!ok) return;

  try {
    for (const idx of targets) {
      const item = improvementItems.value[idx];
      const requestBody = {
        ...assessKeys(),
        improvementSeq: item.improvementSeq,
        improveDate: item.improveDate,
        improveDesc: item.improveDesc,
        likelihoodScore: Number(item.likelihoodScore),
        severityScore: Number(item.severityScore),
        riskLv: item.riskLv,
      };
      if (item.previewFile) {
        requestBody.itemBase64 = await readFileAsBase64(item.previewFile);
        requestBody.itemOriginalFilename = item.previewFile.name;
      }
      await axios.post("/webApi/riskimpr01/save-item", requestBody, {
        headers: { "Content-Type": "application/json" },
      });
    }
    await proxy.$alert("저장되었습니다.");
    await loadImprovementItems();
    emit("saved");
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 개선완료 (005→003 전이)
const completeImprovement = async () => {
  // 위험도 최신화
  improvementItems.value.forEach((item) => recalcItemRiskLv(item));

  // 최신(가장 최근, 배열 선두) 저장된 개선항목을 reval* 로 승격
  const latest = improvementItems.value.find((item) => item.improvementSeq != null);
  if (!latest) {
    await proxy.$alert("개선완료 전에 개선항목을 1건 이상 저장해 주세요.");
    return;
  }

  // FE 선검증: 개선 후 위험도가 매우낮음(1~3) 아니면 중단 (서버 가드 이중)
  if (!isVeryLow(latest.riskLv)) {
    await proxy.$alert(
      '개선완료는 개선 후 위험도가 "매우낮음"(1~3)일 때만 처리할 수 있습니다.'
    );
    return;
  }

  const ok = await proxy.$confirm(
    "개선완료 처리할까요? 처리 후에는 개선항목을 수정할 수 없습니다."
  );
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/riskimpr01/complete",
      {
        ...assessKeys(),
        revalLikelihoodScore: Number(latest.likelihoodScore),
        revalSeverityScore: Number(latest.severityScore),
        revalRiskLv: latest.riskLv,
        revalDesc: latest.improveDesc,
        revalDate: latest.improveDate,
      },
      { headers: { "Content-Type": "application/json" } }
    );
    if (response.status === 200) {
      await proxy.$alert("개선완료 처리되었습니다.");
      emit("completed");
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "개선완료 처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 개선항목 목록 로드 (최신 SEQ 상단)
const loadImprovementItems = async () => {
  try {
    const response = await axios.get("/webApi/riskimpr01/improvement-items", {
      params: assessKeys(),
    });
    if (response.status === 200) {
      const list = response.data?.improvementItemList || [];
      improvementItems.value = list.map((row) => ({
        uid: `seq-${row.improvementSeq}`,
        improvementSeq: row.improvementSeq,
        improveDate: row.improveDate || "",
        improveDesc: row.improveDesc || "",
        fileMgmtCd: row.fileMgmtCd || "",
        filePath: row.filePath || "",
        previewFile: null,
        previewUrl: buildFileUrl(row.filePath, row.fileMgmtCd) || "",
        likelihoodScore: row.likelihoodScore != null ? String(row.likelihoodScore) : "",
        severityScore: row.severityScore != null ? String(row.severityScore) : "",
        riskLv: row.riskLv || "",
      }));
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 좌측 위험상황 초기화 (props 데이터)
const initBefore = () => {
  const d = props.riskAssessmentData || {};
  formData.value = {
    cmpnyCd: d.cmpnyCd || "",
    siteCd: d.siteCd || "",
    processCd: d.processCd || "",
    processNm: d.processNm || "",
    riskTypeNm: d.riskTypeNm || "",
    hazardNm: d.hazardNm || "",
    assessmentCd: d.assessmentCd || "",
    initAssessDate: d.initAssessDate || "",
    initAssessorNm: d.initAssessorNm || "",
    initDesc: d.initDesc || "",
    initLikelihoodScore: d.initLikelihoodScore || "",
    initSeverityScore: d.initSeverityScore || "",
    initRiskLv: d.initRiskLv || "",
    initFileMgmtCd: d.initFileMgmtCd || "",
    initFilePath: d.initFilePath || "",
  };
  beforePhotoUrl.value = buildFileUrl(d.initFilePath, d.initFileMgmtCd);
};

onMounted(async () => {
  initBefore();
  await loadImprovementItems();
});

onBeforeUnmount(() => {
  // 신규 선택분 objectURL 정리 (서버 경로 URL 은 revoke 대상 아님)
  improvementItems.value.forEach((item) => {
    if (item.previewFile && item.previewUrl) URL.revokeObjectURL(item.previewUrl);
  });
});
</script>

<style scoped>
.modal-content-wide {
  width: 100%;
  max-width: 1400px;
  max-height: 90vh;
}

.risk-assess-content {
  display: flex;
  flex: 1;
  overflow: hidden;
  gap: 1.5rem;
  min-height: 0;
}

.improvement-section {
  flex: 1;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.section-header {
  background: var(--color-bg);
  padding: 0.75rem 1rem;
  font-weight: 700;
  color: var(--color-text-strong);
  border-bottom: 1px solid var(--color-border);
}

.improvement-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  /* 개선항목 헤더 높이를 1px 줄임(버튼으로 인한 미세한 높이 차 보정) */
  padding-bottom: calc(0.75rem - 5px);
}

.form-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  margin-bottom: 0.5rem;
}

.form-row > label {
  flex: 0 0 120px;
  font-weight: 500;
  font-size: 0.9rem;
  text-align: left;
  color: var(--color-text);
}

.form-row input,
.form-row select,
.form-row textarea {
  flex: 1 1 0%;
  min-width: 0;
  border-radius: var(--input-radius);
  box-sizing: border-box;
}

.form-row textarea {
  resize: vertical;
  min-height: 70px;
  /* 공용 가이드 CSS 가 textarea 에는 테두리를 주지 않아 입력란과 동일한 테두리/패딩을 명시한다. */
  border: 1px solid var(--color-border);
  padding: 0.4rem 0.6rem;
}

.photo-container {
  flex: 1 1 auto;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  background: var(--color-bg);
  min-height: 160px;
  padding: 0.5rem;
  position: relative;
}

.photo-preview {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.photo-placeholder {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

/* 위험성 평가 정보 팝업(RiskAssessInfo)의 위험평가 영역을 캐스케이드까지 동일하게 복제.
   - 항목(.risk-input-item)은 축소 금지(flex:0 0 auto) → 빈도/강도/위험도가 한 줄로 고정.
   - select 의 flex 는 선언하지 않아 .form-row select(flex:1 1 0%)가 그대로 적용되고,
     min-width:80px 가 더 구체적이라 우선 → 기준 팝업과 동일한 폭으로 렌더된다.
   - 카드(.improve-card)가 본문보다 카드 패딩만큼 좁아도, 세 항목 합(약 372px)이
     카드 내부 가용 폭보다 작아 한 줄을 유지하며 영역을 벗어나지 않는다. */
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

.risk-input-item > label {
  font-size: 0.85rem;
  font-weight: 500;
  flex: 0 0 auto;
  white-space: nowrap;
  color: var(--color-text);
}

.risk-input-item select {
  padding: 0.4rem 0.6rem;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-size: 0.9rem;
  text-align: left;
  box-sizing: border-box;
  min-width: 80px;
}

.risk-readonly,
.risk-level-display {
  padding: 0.4rem 0.6rem;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  text-align: center;
  font-weight: bold;
  min-width: 60px;
  min-height: 2.2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  font-size: 0.9rem;
}

/* 위험도 등급 색(6단계). 위험성 평가 정보 팝업(RiskAssessInfo)·Risk_02 관리기준표와 동일 팔레트.
   해당 클래스가 전역에 없어(각 컴포넌트 scoped 정의) 여기에도 동일하게 둔다. */
.risk-very-high {
  background: #ff4444;
  color: #fff;
}

.risk-high {
  background: #ff8800;
  color: #fff;
}

.risk-slightly-high {
  background: #ffaa00;
  color: #1f1e1e;
}

.risk-normal {
  background: #ffd700;
  color: #1f1e1e;
}

.risk-low {
  background: #90ee90;
  color: #1f1e1e;
}

.risk-very-low {
  background: #228b22;
  color: #fff;
}

/* 개선항목 카드 */
.improve-empty {
  color: var(--color-text-muted);
  font-size: 0.875rem;
  padding: 1rem;
  text-align: center;
}

.improve-card {
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 1rem;
  margin-bottom: 1rem;
  background: var(--color-surface);
}

.improve-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.improve-card__title {
  font-weight: 600;
  color: var(--color-text-strong);
  font-size: 0.95rem;
}

.improve-card__del {
  padding: 0.2rem 0.6rem;
  font-size: 0.8rem;
}

.improve-card__foot {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.5rem;
}

/* 사진 업로드 (RiskAssessInfo 패턴 토큰화) */
.file-upload-wrapper {
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
  border: 2px dashed var(--color-border-strong);
  border-radius: 4px;
  background: var(--color-bg);
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  width: 100%;
  box-sizing: border-box;
  min-height: 56px;
}

.upload-button span {
  white-space: nowrap;
}

.upload-button-small {
  padding: 0.5rem;
  min-height: 30px;
}

.upload-button:hover {
  border-color: var(--color-primary);
  background: var(--color-surface);
}

.upload-icon {
  width: 22px;
  height: 22px;
  color: var(--color-text-muted);
}

.uploaded-photo-container {
  width: 100%;
  margin-top: 0.5rem;
}

.uploaded-photo-container .photo-container {
  width: 100%;
  min-height: 160px;
}

.delete-photo-button {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(17, 24, 39, 0.6);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.delete-photo-button:hover {
  background: var(--color-danger);
}

.delete-icon {
  width: 16px;
  height: 16px;
  color: var(--color-surface);
  stroke-width: 2.5;
}

/* 푸터 */
.modal-footer {
  justify-content: space-between;
}

.footer-buttons-left,
.footer-buttons-right {
  display: flex;
  gap: 0.5rem;
}

.btn {
  padding: 0 1rem;
  border-radius: var(--btn-radius);
  font-size: 0.875rem;
  font-weight: 500;
  font-family: "Pretendard", sans-serif;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s, box-shadow 0.2s;
}

.btn-cancel {
  background: var(--color-surface);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.btn-cancel:hover {
  background: var(--color-bg);
}

.btn-save {
  background: var(--color-primary);
  color: var(--color-surface);
  border: none;
}

.btn-save:hover {
  background: var(--color-primary-hover);
}

.btn-report {
  background: var(--color-surface);
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
}

.btn-report:hover {
  background: rgba(22, 163, 74, 0.06);
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
