<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-show="true"
        ref="overlayRef"
        class="modal-overlay prafta-modal-popup"
        tabindex="-1"
        @click.self="onClose"
        @keydown.esc="onClose"
      >
        <div class="gps-pop" role="dialog" aria-modal="true">
          <!-- ── HEADER ─────────────────────────────────────── -->
          <div class="gps-pop__header">
            <span class="gps-pop__title">
              외근 GPS 동선
              <span v-if="label_p" class="gps-pop__sub">· {{ label_p }}</span>
            </span>
            <button
              class="gps-pop__close"
              type="button"
              aria-label="닫기"
              @click="onClose"
            >
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
              >
                <path d="M18 6L6 18M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- ── BODY ───────────────────────────────────────── -->
          <div class="gps-pop__body">
            <AttdGpsCoordPanel :trail="trail" :loading="loading" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, onMounted, nextTick } from "vue";
import AttdGpsCoordPanel from "@/views/attd/popup/AttdGpsCoordPanel.vue";
import axios from "@/api/axios";

const props = defineProps({
  // GPS 동선을 조회할 근태 식별자
  attdId_p: { type: String, default: "" },
  // 헤더 보조 라벨 (예: "1구간")
  label_p: { type: String, default: "" },
});

const emit = defineEmits(["close"]);

// GPS 좌표 배열 / 조회 로딩 상태 — AttdGpsCoordPanel 에 그대로 주입
const trail = ref([]);
const loading = ref(false);
const overlayRef = ref(null);

const onClose = () => emit("close");

// 근태 식별자로 외근 GPS 동선을 조회한다 (Attd_08 attd-gps-trail 패턴 차용).
const fnLoadTrail = async () => {
  if (!props.attdId_p) return;
  loading.value = true;
  try {
    const response = await axios.get("/webApi/attd08/attd-gps-trail", {
      params: { attdId: props.attdId_p },
    });
    if (response.status === 200) {
      trail.value = response.data?.attdGpsTrailResultList ?? [];
    }
  } catch (err) {
    console.error("[AttdGpsTrailPop] GPS 동선 조회 실패", err);
    trail.value = [];
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  await nextTick();
  overlayRef.value?.focus();
  fnLoadTrail();
});
</script>

<style scoped>
/* ReasonInputModal.vue 오버레이 패턴 차용 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.gps-pop {
  background: var(--color-surface);
  border-radius: var(--card-radius);
  width: 90%;
  max-width: 520px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.gps-pop__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--card-padding);
  border-bottom: 1px solid var(--color-border);
}

.gps-pop__title {
  font-weight: 600;
  font-size: 1rem;
  color: var(--color-text-strong);
}

.gps-pop__sub {
  margin-left: 0.25rem;
  font-weight: 500;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.gps-pop__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  border-radius: var(--input-radius);
  cursor: pointer;
}

.gps-pop__close:hover {
  background: var(--color-bg);
  color: var(--color-text-strong);
}

.gps-pop__body {
  padding: var(--card-padding);
}
</style>
