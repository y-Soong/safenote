<!--
  SafetyInspectBadForm.vue — 불량 항목 입력 영역 (prafta-app-011 화면 B)
  - 사유(필수, <=500자, 카운터) + 사진(선택, 1장).
  - 사진은 메모리 보관(File). 저장 시점에 부모가 multipart 로 일괄 전송(기존 계약).
  - 양방향: reason(v-model), file 변경은 이벤트로 부모에 통지.
-->
<template>
  <div class="bad-form">
    <!-- 사유 -->
    <div class="bf-row">
      <label :for="reasonId" class="bf-lbl">
        <span class="req" aria-hidden="true">*</span> 불량 사유
      </label>
      <textarea
        :id="reasonId"
        class="bf-ta"
        :value="reason"
        :maxlength="MAX_LEN"
        placeholder="발견한 불량 상태를 구체적으로 입력해 주세요."
        aria-required="true"
        @input="onReasonInput"
      ></textarea>
      <div class="bf-count">{{ reason.length }} / {{ MAX_LEN }}</div>
    </div>

    <!-- 사진 -->
    <div class="bf-row">
      <div class="bf-lbl">현장 사진 <span class="bf-opt">(선택 · 1장)</span></div>
      <div class="bf-photo">
        <!-- 미첨부: 추가 버튼 -->
        <button
          v-if="!photo"
          type="button"
          class="bf-photo-btn"
          aria-label="사진 추가"
          :disabled="picking"
          @click="onPickPhoto"
        >
          <svg
            width="22"
            height="22"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path
              d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"
            />
            <circle cx="12" cy="13" r="4" />
          </svg>
          <span>{{ picking ? '여는 중...' : '사진 추가' }}</span>
        </button>

        <!-- 첨부: 미리보기 + 제거 + 메타 -->
        <template v-else>
          <div class="bf-photo-prv">
            <img :src="photo.previewUrl" alt="첨부한 현장 사진" />
            <button type="button" class="rm" aria-label="사진 제거" @click="onRemovePhoto">
              <svg
                width="12"
                height="12"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
              >
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>
          <div class="bf-photo-meta">
            <strong>{{ photo.name }}</strong>
            {{ photo.sizeText }}
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { selectImage, revokePreview } from '@/utils/imagePicker'

const MAX_LEN = 500

const props = defineProps({
  // 항목 식별자 (라벨 for 연결용)
  itemCd: { type: String, required: true },
  // 불량 사유 (v-model)
  reason: { type: String, default: '' },
  // 첨부 사진 정보 { file, previewUrl, name, sizeText } | null
  photo: { type: Object, default: null },
})

const emit = defineEmits(['update:reason', 'update:photo'])

const picking = ref(false)

const reasonId = `bf-reason-${props.itemCd}`

const onReasonInput = (e) => {
  emit('update:reason', e.target.value)
}

// 바이트 → 사람이 읽는 크기 텍스트
const formatSize = (bytes) => {
  if (!bytes && bytes !== 0) return ''
  if (bytes < 1024) return `${bytes}B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)}KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`
}

const onPickPhoto = async () => {
  if (picking.value) return
  picking.value = true
  try {
    // 카메라 우선 (실패/취소 시 갤러리 폴백). selectImage 는 HEIC->JPEG 변환·미리보기 포함.
    const { file, previewUrl } = await selectImage('camera')
    // 기존 미리보기 정리
    if (props.photo?.previewUrl) revokePreview(props.photo.previewUrl)
    emit('update:photo', {
      file,
      previewUrl,
      name: file.name || 'photo.jpg',
      sizeText: `방금 촬영 · ${formatSize(file.size)}`,
    })
  } catch (e) {
    // 사용자가 선택을 취소한 경우(no-file)는 무시
    if (e?.message !== 'no-file') {
      console.warn('[SafetyInspectBadForm] 사진 선택 실패:', e?.message)
    }
  } finally {
    picking.value = false
  }
}

const onRemovePhoto = () => {
  if (props.photo?.previewUrl) revokePreview(props.photo.previewUrl)
  emit('update:photo', null)
}

// 자가복구(C-3b): C-3a 가 어떤 엣지에서 또 결말을 못 내더라도, 화면이 다시
// 포커스/가시화되면 grace 후 picking 을 강제 해제하여 "여는 중..." 고착 방지.
// (정상 케이스에서는 selectImage 가 먼저 종결해 picking 이 이미 false 이므로 무해.)
let recoverTimer = null

const scheduleRecover = () => {
  if (!picking.value) return
  if (recoverTimer) clearTimeout(recoverTimer)
  // 정상 촬영 복귀 직후 change 처리 중인 케이스를 취소로 오판하지 않도록 grace 부여.
  recoverTimer = setTimeout(() => {
    recoverTimer = null
    if (picking.value) {
      picking.value = false
    }
  }, 700)
}

const onWindowFocus = () => {
  scheduleRecover()
}

const onVisibilityChange = () => {
  if (document.visibilityState === 'visible') scheduleRecover()
}

onMounted(() => {
  window.addEventListener('focus', onWindowFocus)
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onBeforeUnmount(() => {
  window.removeEventListener('focus', onWindowFocus)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  if (recoverTimer) {
    clearTimeout(recoverTimer)
    recoverTimer = null
  }
})
</script>

<style scoped>
.bad-form {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #fecaca;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.bf-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.bf-lbl {
  font-size: 12px;
  font-weight: 600;
  color: #991b1b;
  display: flex;
  align-items: center;
  gap: 4px;
}
.bf-lbl .req {
  color: var(--color-danger);
}
.bf-opt {
  color: var(--color-text-secondary);
  font-weight: 500;
  font-size: 11px;
}
.bf-ta {
  width: 100%;
  min-height: 72px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  color: var(--color-text-primary);
  background: var(--color-surface);
  font-family: inherit;
  resize: none;
  line-height: 1.5;
}
.bf-ta::placeholder {
  color: var(--color-text-tertiary);
}
.bf-count {
  font-size: 11px;
  color: var(--color-text-tertiary);
  text-align: right;
}
.bf-photo {
  display: flex;
  align-items: center;
  gap: 8px;
}
.bf-photo-btn {
  width: 72px;
  height: 72px;
  border: 1.5px dashed var(--color-border);
  border-radius: 10px;
  background: var(--color-surface);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: var(--color-text-secondary);
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
  flex-shrink: 0;
}
.bf-photo-btn:disabled {
  opacity: 0.6;
  cursor: progress;
}
.bf-photo-prv {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
}
.bf-photo-prv img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.bf-photo-prv .rm {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #ffffff;
  border: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 0;
}
.bf-photo-meta {
  font-size: 11px;
  color: var(--color-text-secondary);
  flex: 1;
  min-width: 0;
}
.bf-photo-meta strong {
  color: var(--color-text-primary);
  font-weight: 600;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
