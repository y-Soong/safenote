<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-normal modal-content-admin-qr"
        :style="positionStyle"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>관리자 QR 생성</span>
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

        <div class="modal-body-admin-qr">
          <p class="modal-desc">
            배정할 빈 자리 슬롯을 선택하고 근로자 정보를 입력하세요. 저장 시
            해당 슬롯이 점유됩니다.
          </p>

          <div class="form-grid">
            <div class="form-item full-width">
              <label
                >배정 슬롯 (빈 자리만) <span class="required">*</span></label
              >
              <select v-model="selectedSlotNo">
                <option value="" disabled>슬롯을 선택하세요</option>
                <option
                  v-for="s in availableSlots"
                  :key="s.slotNo"
                  :value="s.slotNo"
                  :disabled="!hasNode(s)"
                >
                  {{ s.slotNo }}번 슬롯
                  {{
                    hasNode(s)
                      ? "· " + s.nodeNm
                      : "(소속부서 미지정 - 매칭 불가)"
                  }}
                </option>
              </select>
              <p class="field-desc">
                소속부서가 지정된 빈 슬롯만 선택할 수 있습니다.
              </p>
            </div>

            <div class="form-item full-width">
              <label>근로자 이름 <span class="required">*</span></label>
              <input
                v-model.trim="userNm"
                type="text"
                placeholder="예: 홍길동"
              />
              <p class="field-desc">출퇴근 QR과 함께 슬롯에 기록됩니다.</p>
            </div>

            <div class="form-item full-width">
              <label>휴대폰번호 <span class="required">*</span></label>
              <input
                v-model.trim="userPhone"
                type="text"
                placeholder="예: 010-1234-5678"
                maxlength="13"
              />
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <div class="footer-actions">
            <button class="btn btn-secondary" @click="$emit('close')">
              취소
            </button>
            <button
              class="btn btn-primary"
              :disabled="!canSave || isSaving"
              @click="fnSave"
            >
              {{ isSaving ? "처리 중..." : "QR 생성 · 슬롯 점유" }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import axios from "@/api/axios";

const props = defineProps({
  slotList: { type: Array, default: () => [] },
  siteCd: { type: String, default: "" },
  onSaved: { type: Function, default: null },
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 2,
});

const selectedSlotNo = ref("");
const userNm = ref("");
const userPhone = ref("");
// T1-04b: 연속/중복 클릭 가드. 처리 중에는 저장 버튼 비활성 + 재요청 차단.
const isSaving = ref(false);

const availableSlots = computed(() =>
  (props.slotList || []).filter((s) => s.slotStatus === "01" && s.useYn === "Y")
);

// 요건1: 슬롯에 소속부서(nodeCd)가 지정돼야 계정 매칭 가능. 미지정 슬롯은 선택 불가.
const hasNode = (s) => !!(s && s.nodeCd != null && String(s.nodeCd).trim());

const positionStyle = computed(() => {
  const padding = 16;
  const modalWidth = 480;
  const modalHeight = 460;
  const maxX = window.innerWidth - (modalWidth + padding);
  const maxY = window.innerHeight - (modalHeight + padding);
  const x = Math.max(padding, Math.min(maxX, position.value.x));
  const y = Math.max(padding, Math.min(maxY, position.value.y));
  return { top: y + "px", left: x + "px" };
});

const canSave = computed(
  () => !!selectedSlotNo.value && !!userNm.value && !!userPhone.value
);

const fnSave = async () => {
  if (!canSave.value) return;
  // T1-04b: 이미 처리 중이면 중복 요청 차단(성공/실패 alert 중첩 방지).
  if (isSaving.value) return;

  if (!proxy.$util.validatePhoneNumber(userPhone.value)) {
    await proxy.$alert("휴대폰 번호를 확인해주세요.");
    return;
  }

  // 요건1 방어 가드: 소속부서 미지정 슬롯은 매칭 불가(서버도 BAIM_400_007 로 차단). 비활성 옵션과 이중 방어.
  const targetSlot = availableSlots.value.find(
    (s) => s.slotNo === selectedSlotNo.value
  );
  if (!hasNode(targetSlot)) {
    await proxy.$alert(
      "소속부서가 지정되지 않은 슬롯입니다.\n계정슬롯 목록에서 소속부서를 먼저 지정해 주세요."
    );
    return;
  }

  const mblNo = userPhone.value.replace(/\D+/g, "");

  isSaving.value = true;
  try {
    // 휴대폰 중복 사전확인(baim05-qr-phone-precheck).
    //   ACTIVE: 활성 계정 존재 → 발급 불가 안내(서버도 BAIM_400_003 으로 이중 차단).
    //   REACTIVATABLE: 비활성 계정 존재 → 발급 시 그 계정이 재활성(재사용)되므로 관리자 confirm 후 진행.
    //   휴대폰 평문의 접근로그 잔존 방지를 위해 POST 바디로 전송(GET 쿼리스트링 금지 — security Medium #3).
    const checkRes = await axios.post("/webApi/baim05/check-daily-user-phone", {
      siteCd: props.siteCd,
      mblNo,
    });
    const duplicateType = checkRes.data?.duplicateType;
    if (duplicateType === "ACTIVE") {
      await proxy.$alert(
        "동일한 휴대폰번호를 사용 중인 계정이 이미 존재합니다.\n휴대폰번호를 확인해 주세요."
      );
      return;
    }
    if (duplicateType === "REACTIVATABLE") {
      const maskedNm = checkRes.data?.maskedUserNm || "";
      const ok = await proxy.$confirm(
        `이 번호로 사용되던 비활성 계정${maskedNm ? `(${maskedNm})` : ""}이 있습니다.\n` +
          "발급 시 해당 계정이 재활성되어 재사용됩니다. 계속하시겠습니까?"
      );
      if (!ok) return;
    }

    const response = await axios.post("/webApi/baim05/insert-daily-qr-user", {
      siteCd: props.siteCd,
      userNm: userNm.value,
      mblNo: mblNo,
      slotNo: selectedSlotNo.value,
    });

    if (response.status === 200) {
      const { cmpnyCd, siteCd, userCd } =
        response.data.dailyUserQrInfoResult ?? {};
      if (cmpnyCd && siteCd && userCd) {
        // T1-04b: 성공 시 메시지 1건만 노출 후 즉시 닫는다(메시지 단일화).
        await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));

        emit("close");
        props.onSaved?.({
          cmpnyCd,
          siteCd,
          userCd,
          userNm: userNm.value,
        });
      } else {
        await proxy.$alert("QR 생성에 필요한 정보가 부족합니다.");
      }
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    isSaving.value = false;
  }
};
</script>

<style scoped>
@import "@/assets/css/modal-popup-guide.css";

.modal-content-admin-qr {
  width: 480px;
  max-height: 85vh;
}

.modal-body-admin-qr {
  padding: 1rem 1.5rem;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

.modal-desc {
  margin: 0 0 1rem;
  font-size: 0.8125rem;
  color: #6b7280;
  line-height: 1.5;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-item.full-width {
  grid-column: 1 / -1;
}

.form-item label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}

.form-item .required {
  color: #ef4444;
}

.form-item input,
.form-item select {
  padding: 0.5rem 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.875rem;
}

.form-item input:focus,
.form-item select:focus {
  outline: none;
  border-color: #16a34a;
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.2);
}

.field-desc {
  margin: 0.125rem 0 0;
  font-size: 0.75rem;
  color: #6b7280;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 1rem 1.5rem;
  background: var(--modal-footer-bg, #f9fafb);
  border-top: 1px solid var(--modal-border, #e5e7eb);
}

.footer-actions {
  display: flex;
  gap: 0.5rem;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
}

.btn-secondary {
  background: #fff;
  border: 1px solid #d1d5db;
  color: #374151;
}

.btn-secondary:hover {
  background: #f9fafb;
}

.btn-primary {
  background: #16a34a;
  border: 1px solid #16a34a;
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: #15803d;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
