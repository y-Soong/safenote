<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 헤더 (드래그) -->
        <div class="modal-header" @mousedown="startDrag">
          <span>아차사고 상세</span>
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

        <!-- 메인 컨텐츠 (2단) -->
        <div class="near-miss-content">
          <!-- 좌: 보고 내용 (읽기) -->
          <div class="incident-section report-section">
            <div class="section-header">보고 내용</div>
            <div class="form-container">
              <div class="form-row">
                <label>사건ID</label>
                <input v-model="formData.nearMissId" readonly />
              </div>
              <div class="form-row">
                <label>발생일시</label>
                <input v-model="formData.occurDtime" readonly />
              </div>
              <div class="form-row">
                <label>발생장소</label>
                <input v-model="formData.locationDesc" readonly />
              </div>
              <div class="form-row">
                <label>경위</label>
                <textarea
                  v-model="formData.description"
                  readonly
                  rows="3"
                ></textarea>
              </div>
              <div class="form-row">
                <label>잠재중대성</label>
                <span
                  class="severity-badge"
                  :class="fnSeverityClass(formData.potentialSeverityCd)"
                >
                  {{ formData.potentialSeverityNm || "-" }}
                </span>
              </div>
              <div class="form-row">
                <label>사진</label>
                <div class="photo-container">
                  <img
                    v-if="photoUrl"
                    :src="photoUrl"
                    alt="현장 사진"
                    class="photo-preview"
                  />
                  <div v-else class="photo-placeholder">사진 없음</div>
                </div>
              </div>
              <div class="form-row">
                <label>보고자</label>
                <input v-model="formData.reporterNm" readonly />
              </div>
              <div class="form-row">
                <label>즉시조치</label>
                <textarea
                  v-model="formData.immediateActionDesc"
                  readonly
                  rows="2"
                ></textarea>
              </div>
            </div>
          </div>

          <!-- 우: 조사 / 조치 -->
          <div class="incident-section action-section">
            <div class="section-header">조사 / 조치</div>
            <div class="form-container">
              <div class="form-row">
                <label>추정 원인</label>
                <textarea
                  v-model="formData.causeDesc"
                  :readonly="isReadOnly"
                  placeholder="추정 원인을 입력해 주세요"
                  rows="3"
                ></textarea>
              </div>
              <div class="form-row">
                <label>재발방지 대책</label>
                <textarea
                  v-model="formData.preventionDesc"
                  :readonly="isReadOnly"
                  placeholder="재발방지 대책을 입력해 주세요"
                  rows="4"
                ></textarea>
              </div>
              <div class="form-row">
                <label>처리상태</label>
                <select
                  v-model="formData.reportStatusCd"
                  :disabled="isReadOnly"
                >
                  <!-- 현재 상태 기준 전이 가능한 다음 상태만 노출(설계 §4 상태전이) -->
                  <option
                    v-for="opt in statusOptions"
                    :key="opt.systValDCd"
                    :value="opt.systValDCd"
                  >
                    {{ opt.systValDNm }}
                  </option>
                </select>
              </div>
              <!-- 이미 미처리대상 처리된 건: 저장된 미처리 사유 표시(읽기) -->
              <div
                class="form-row"
                v-if="
                  isReadOnly && currentStatusCd === '400' && storedRejectReason
                "
              >
                <label>미처리 사유</label>
                <textarea
                  :value="storedRejectReason"
                  readonly
                  rows="3"
                ></textarea>
              </div>
              <!-- 미처리대상 선택 시 사유 필수(편집 가능 건만) -->
              <div
                class="form-row"
                v-if="!isReadOnly && formData.reportStatusCd === '400'"
              >
                <label>미처리 사유</label>
                <textarea
                  v-model="rejectReason"
                  placeholder="미처리 사유를 입력해 주세요"
                  rows="3"
                ></textarea>
              </div>
            </div>
          </div>
        </div>

        <!-- 푸터 -->
        <div class="modal-footer">
          <div class="footer-buttons-left"></div>
          <div class="footer-buttons-right">
            <button class="btn btn-cancel" @click="$emit('close')">닫기</button>
            <button v-if="!isReadOnly" class="btn btn-save" @click="fnSave">
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
  computed,
  watch,
  onMounted,
  defineProps,
  defineEmits,
  getCurrentInstance,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { buildFileServingUrl } from "@/utils/fileUrl";

const { proxy } = getCurrentInstance();

const props = defineProps({
  nearMissData: {
    type: Object,
    default: () => ({}),
  },
  onSave: {
    type: Function,
    default: null,
  },
});

const emit = defineEmits(["close", "save"]);

const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const systCodeArr = ref([]);
const rejectReason = ref("");
// 상세 진입 시점의 원본 처리상태(전이 기준값). select로 변경되어도 보존
const currentStatusCd = ref("");

// 사건 헤더 폼 (tb_near_miss 컬럼 기준)
const formData = ref({
  cmpnyCd: "",
  siteCd: "",
  nearMissId: "",
  processCd: "",
  processNm: "",
  occurDtime: "",
  locationDesc: "",
  description: "",
  potentialSeverityCd: "",
  potentialSeverityNm: "",
  immediateActionDesc: "",
  causeDesc: "",
  preventionDesc: "",
  fileMgmtCd: "",
  fileName: "",
  filePath: "",
  reportStatusCd: "",
  reportStatusNm: "",
  rejectReason: "",
  reporterId: "",
  reporterNm: "",
  reportDtime: "",
});

// 완료(300)·미처리대상(400) 건은 읽기전용 (전이 기준은 진입 시점 원본 상태)
// SYS063 재번호(D4): 100 접수 / 200 조치중 / 300 완료 / 400 미처리대상.
const isReadOnly = computed(
  () => currentStatusCd.value === "300" || currentStatusCd.value === "400"
);

// 상태 전이 규칙(정책 A): 선형 활성단계(접수→조치중→완료)에서 더 뒤 단계로 전진 점프 허용
//   (접수→완료 직접 등). 자기 자신 포함, 어느 활성단계든 400 미처리대상. 뒤로 가기 불가.
//   종결(완료300/미처리대상400)은 자기 자신만.
const STATUS_TRANSITIONS = {
  "100": ["100", "200", "300", "400"],
  "200": ["200", "300", "400"],
  "300": ["300"],
  "400": ["400"],
};

// 처리상태 select 옵션 — 현재 상태 기준 전이 가능한 코드만 노출
const statusOptions = computed(() => {
  const all = systCodeArr.value["SYS063"] || [];
  const allowed = STATUS_TRANSITIONS[currentStatusCd.value] || [];
  if (allowed.length === 0) {
    return all;
  }
  return all.filter((opt) => allowed.includes(opt.systValDCd));
});

// 저장된 미처리 사유(상세 응답의 rejectReason 분리 필드, 컬럼 재활용. 없을 수 있어 옵셔널 처리)
const storedRejectReason = computed(() => formData.value.rejectReason || "");

// 현장 사진 URL
// - 서빙 파일명은 확장자 포함명(fileName = FILE_MGMT_CD + FILE_EXT)을 사용한다.
//   fileMgmtCd(확장자 없음)로 URL 을 만들면 정적 서빙 핸들러가 파일을 찾지 못해(404) 사진이 안 보인다.
// - Windows 저장 시 FILE_PATH 에 백슬래시(\)가 섞이므로 URL 안전 형태(슬래시)로 정규화한다.
const photoUrl = computed(
  () =>
    buildFileServingUrl(formData.value.filePath, formData.value.fileName) ||
    null
);

// 잠재중대성 배지 클래스 (UI 표현만)
const fnSeverityClass = (code) => {
  if (code === "300") return "severity-badge--critical";
  if (code === "200") return "severity-badge--major";
  if (code === "100") return "severity-badge--minor";
  return "";
};

onMounted(async () => {
  await fnGetSystinfoList();
  await fnGetIncidentInfo();
});

// 코드(SYS062/063) 조회 + systValCd 기준 그룹핑
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS062", "SYS063"],
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

// 사건 단건 상세 조회 → formData 매핑(코드명/파일경로 포함)
const fnGetIncidentInfo = async () => {
  if (proxy.$util.isEmpty(formData.value.nearMissId)) {
    return;
  }

  try {
    const response = await axios.get("/webApi/nearmiss01/incident-info", {
      params: {
        siteCd: formData.value.siteCd,
        nearMissId: formData.value.nearMissId,
      },
    });

    if (response.status === 200) {
      const info = response.data?.incidentInfo;
      if (info) {
        // 식별자(cmpnyCd/siteCd/nearMissId)는 목록 진입 시 세팅된 값을 유지하며 상세로 보강
        formData.value = {
          ...formData.value,
          ...info,
        };
        // 현재 처리상태를 전이 기준값으로 보존(select에서 변경되기 전 원본)
        currentStatusCd.value = info.reportStatusCd || "";
      }
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 저장 후 성공 처리(목록 새로고침 + 팝업 닫기) 공통
const fnAfterSuccess = () => {
  if (props.onSave && typeof props.onSave === "function") {
    props.onSave();
  }
  emit("close");
};

// 처리상태 전환 공통 호출(change-status). reportStatusCd/rejectReason 전달
const fnChangeStatus = async (reportStatusCd, rejectReasonVal) => {
  const requestBody = {
    siteCd: formData.value.siteCd,
    nearMissId: formData.value.nearMissId,
    reportStatusCd,
  };
  if (proxy.$util.isNotEmpty(rejectReasonVal)) {
    requestBody.rejectReason = rejectReasonVal;
  }

  const response = await axios.post(
    "/webApi/nearmiss01/change-status",
    requestBody,
    { headers: { "Content-Type": "application/json" } }
  );
  return response;
};

// 정밀조사 저장 = save-incident(원인/재발방지/임시조치) + 상태 변경 시 change-status 동반
const fnSave = async () => {
  const ok = await proxy.$confirm("정밀조사 내용을 저장하시겠습니까?");
  if (!ok) return;

  try {
    const saveRes = await axios.post(
      "/webApi/nearmiss01/save-incident",
      {
        siteCd: formData.value.siteCd,
        nearMissId: formData.value.nearMissId,
        causeDesc: formData.value.causeDesc,
        preventionDesc: formData.value.preventionDesc,
        immediateActionDesc: formData.value.immediateActionDesc,
      },
      { headers: { "Content-Type": "application/json" } }
    );

    if (saveRes.status !== 200) {
      await proxy.$alert("저장 중 오류가 발생했습니다.");
      return;
    }

    // 처리상태가 진입 시점과 달라졌으면 상태 전환도 함께 반영
    if (
      proxy.$util.isNotEmpty(formData.value.reportStatusCd) &&
      formData.value.reportStatusCd !== currentStatusCd.value
    ) {
      // 미처리대상(400) 선택 시 사유 필수
      if (formData.value.reportStatusCd === "400") {
        if (proxy.$util.isEmpty(rejectReason.value)) {
          await proxy.$alert("미처리 사유를 입력해 주세요.");
          return;
        }
      }
      await fnChangeStatus(
        formData.value.reportStatusCd,
        formData.value.reportStatusCd === "400" ? rejectReason.value : null
      );
    }

    await proxy.$alert("저장되었습니다.");
    fnAfterSuccess();
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// props → formData 매핑 (목록에서 전달된 키만 우선 세팅, 상세는 fnGetIncidentInfo로 보강)
watch(
  () => props.nearMissData,
  (newData) => {
    if (newData) {
      formData.value.cmpnyCd = newData.cmpnyCd || "";
      formData.value.siteCd = newData.siteCd || "";
      formData.value.nearMissId = newData.nearMissId || "";
    }
  },
  { immediate: true }
);
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
  background: var(--color-surface, #fff);
  border-radius: var(--radius-lg, 8px);
  overflow: hidden;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
}

.near-miss-content {
  display: flex;
  flex: 1;
  overflow: hidden;
  padding: var(--space-lg, 1.5rem);
  gap: var(--space-lg, 1.5rem);
  min-height: 0;
}

.incident-section {
  flex: 1;
  border: 1px solid var(--color-border, #ddd);
  border-radius: var(--radius-md, 4px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.section-header {
  background: var(--color-surface-muted, #f5f5f5);
  padding: var(--space-md, 0.75rem) var(--space-lg, 1rem);
  font-weight: bold;
  border-bottom: 1px solid var(--color-border, #ddd);
}

.form-container {
  flex: 1;
  padding: var(--space-lg, 1rem);
  flex-direction: column;
  gap: var(--space-lg, 1rem);
  overflow-y: auto;
  overflow-x: hidden;
  min-height: 0;
  align-items: flex-start;
  text-align: left;
}

.form-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm, 0.5rem);
  text-align: left;
  margin-bottom: var(--space-sm, 0.5rem);
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.form-row label {
  flex: 0 0 120px;
  font-weight: 500;
  font-size: var(--font-size-sm, 0.9rem);
  text-align: left;
  color: var(--color-text, #333);
}

.form-row input,
.form-row select,
.form-row textarea {
  flex: 1 1 0%;
  min-width: 0;
  padding: var(--space-xs, 0.4rem) var(--space-sm, 0.6rem);
  border: 1px solid var(--color-border-input, #ccc);
  border-radius: var(--radius-md, 4px);
  font-size: var(--font-size-sm, 0.9rem);
  text-align: left;
  box-sizing: border-box;
}

.form-row input:read-only,
.form-row textarea:read-only {
  background: var(--color-surface-muted, #f5f5f5);
  cursor: not-allowed;
}

.form-row textarea {
  resize: vertical;
  min-height: 80px;
}

.photo-container {
  flex: 1;
  border: 1px solid var(--color-border, #ddd);
  border-radius: var(--radius-md, 4px);
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  background: var(--color-surface-subtle, #f9f9f9);
  min-height: 160px;
  padding: var(--space-sm, 0.5rem);
  position: relative;
}

.photo-container .photo-placeholder {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: var(--color-text-muted, #999);
  font-size: var(--font-size-sm, 0.9rem);
}

.photo-preview {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

/* 잠재중대성 배지 */
.severity-badge {
  display: inline-block;
  padding: var(--space-xxs, 0.125rem) var(--space-sm, 0.5rem);
  border-radius: var(--radius-pill, 999px);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
}

.severity-badge--minor {
  background: var(--color-severity-minor-bg, #ecfdf5);
  color: var(--color-severity-minor-fg, #047857);
}

.severity-badge--major {
  background: var(--color-severity-major-bg, #fff7ed);
  color: var(--color-severity-major-fg, #c2410c);
}

.severity-badge--critical {
  background: var(--color-severity-critical-bg, #fef2f2);
  color: var(--color-severity-critical-fg, #b91c1c);
}

.modal-footer {
  justify-content: space-between;
}

.footer-buttons-left,
.footer-buttons-right {
  display: flex;
  align-items: center;
  gap: var(--space-sm, 0.5rem);
}

.industrial-notice {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-severity-major-fg, #c2410c);
}

.btn {
  padding: 0 var(--space-lg, 1rem);
  border-radius: var(--radius-md, 6px);
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 500;
  font-family: "Pretendard", sans-serif;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s, box-shadow 0.2s;
}

.btn-cancel {
  background: var(--color-surface, #ffffff);
  color: var(--color-text, #374151);
  border: 1px solid var(--color-border, #e5e7eb);
}

.btn-cancel:hover {
  background: var(--color-surface-hover, #f9fafb);
}

.btn-save {
  background: var(--color-primary, #16a34a);
  color: #ffffff;
  border: none;
}

.btn-save:hover {
  background: var(--color-primary-strong, #15803d);
}

.btn-report {
  background: var(--color-surface, #ffffff);
  color: var(--color-primary, #16a34a);
  border: 1px solid var(--color-primary, #16a34a);
}

.btn-report:hover {
  background: var(--color-primary-soft, rgba(22, 163, 74, 0.06));
}

.btn-reject {
  background: var(--color-surface, #ffffff);
  color: var(--color-danger, #b91c1c);
  border: 1px solid var(--color-danger, #b91c1c);
}

.btn-reject:hover {
  background: var(--color-danger-soft, rgba(185, 28, 28, 0.06));
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
