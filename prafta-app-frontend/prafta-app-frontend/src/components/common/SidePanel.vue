<template>
  <transition name="slide">
    <div
      v-if="visible"
      class="fixed inset-y-0 right-0 w-full sm:w-96 bg-white shadow-xl z-50 flex flex-col"
    >
      <!-- 헤더 -->
      <div class="flex justify-between items-center p-4 border-b">
        <h3 class="text-lg font-semibold text-gray-800">{{ title }}</h3>
        <button class="text-gray-600 hover:text-black" @click="$emit('close')">✕</button>
      </div>

      <!-- 검색 영역 -->
      <div v-if="showSearch" class="p-4 border-b">
        <div class="flex items-center border rounded-md px-3 py-2">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-5 w-5 text-gray-400 mr-2"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
            />
          </svg>
          <input
            v-model="srchParam"
            type="text"
            placeholder="검색어 입력"
            class="flex-1 outline-none"
            @keyup.enter="onSearch"
          />
        </div>
      </div>

      <!-- ✅ 콘텐츠 영역 -->
      <div class="flex-1 overflow-y-auto p-4 mb-20">
        <!-- 로딩 중 -->
        <div v-if="isLoading" class="flex flex-col items-center justify-center text-gray-500 mt-20">
          <svg
            class="animate-spin h-10 w-10 text-green-600 mb-3"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              class="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              stroke-width="4"
            ></circle>
            <path
              class="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
            ></path>
          </svg>
          <p class="text-sm">검색 중입니다...</p>
        </div>

        <!-- 에러 발생 -->
        <div
          v-else-if="isError"
          class="flex flex-col items-center justify-center text-red-500 mt-20"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-12 w-12 mb-3"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M12 9v2m0 4h.01M12 5a7 7 0 100 14a7 7 0 000-14z"
            />
          </svg>
          <p class="font-semibold">오류가 발생했습니다.</p>
          <p class="text-sm text-gray-500">잠시 후 다시 시도해주세요.</p>
        </div>

        <!-- 결과 있을 때 -->
        <template v-else-if="hasResults">
          <label
            v-for="(item, idx) in items"
            :key="item[keyField] || idx"
            class="flex items-center justify-between border rounded-md px-4 py-3 mb-2 hover:border-green-600 cursor-pointer"
          >
            <span>{{ item[labelField] }}</span>
            <input
              :type="multiple ? 'checkbox' : 'radio'"
              :name="multiple ? 'multi-select[]' : 'single-select'"
              :checked="isSelected(item)"
              class="ml-auto accent-green-600 w-5 h-5"
              @change="toggleSelection(item)"
            />
          </label>
        </template>

        <!-- 결과 없을 때 -->
        <template v-else>
          <div class="flex flex-col items-center justify-center text-gray-500 mt-20">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              class="h-16 w-16 text-gray-400 mb-4"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M21 21l-5.2-5.2m0 0A7 7 0 1010 17a7 7 0 005.8-2.8z"
              />
            </svg>
            <p class="text-lg font-semibold">검색 결과가 없습니다.</p>
            <p class="text-sm">검색어를 확인해 주세요.</p>
          </div>
        </template>
      </div>

      <!-- ✅ 하단 고정 버튼 -->
      <div class="absolute bottom-0 left-0 w-full p-4 bg-white border-t">
        <button
          class="w-full bg-green-700 text-white py-3 rounded-md hover:bg-green-800 transition"
          @click="$emit('confirm', selectedItems)"
        >
          확인
        </button>
      </div>
    </div>
  </transition>
</template>

<script setup>
/* eslint-disable */
import { ref, watch, defineProps, defineEmits } from 'vue'

const props = defineProps({
  visible: Boolean,
  title: { type: String, default: '사이드 메뉴' },
  showSearch: { type: Boolean, default: false },
  keyword: { type: String, default: '' },
  hasResults: { type: Boolean, default: true },
  isLoading: { type: Boolean, default: false },
  isError: { type: Boolean, default: false },
  multiple: { type: Boolean, default: false }, // ✅ 다중 선택 여부
  items: { type: Array, default: () => [] }, // ✅ 표시할 리스트
  keyField: { type: String, default: 'id' }, // ✅ item 고유키 필드명
  labelField: { type: String, default: 'label' }, // ✅ item 표시 필드명
  modelValue: { type: [Array, Object, String, Number], default: () => [] },
})

const emit = defineEmits(['close', 'search', 'confirm', 'update:modelValue'])
const srchParam = ref(props.keyword)
const selectedItems = ref(props.multiple ? [...props.modelValue] : props.modelValue)

watch(srchParam, (v) => emit('search', v))

// ✅ 검색 실행
function onSearch() {
  emit('search', srchParam.value)
}

// ✅ 항목 선택 여부 판단
function isSelected(item) {
  if (props.multiple) {
    return selectedItems.value.some((i) => i[props.keyField] === item[props.keyField])
  } else {
    return selectedItems.value && selectedItems.value[props.keyField] === item[props.keyField]
  }
}

// ✅ 선택 토글
function toggleSelection(item) {
  if (props.multiple) {
    const exists = selectedItems.value.find((i) => i[props.keyField] === item[props.keyField])
    if (exists) {
      selectedItems.value = selectedItems.value.filter(
        (i) => i[props.keyField] !== item[props.keyField]
      )
    } else {
      selectedItems.value.push(item)
    }
  } else {
    selectedItems.value = item
  }
  emit('update:modelValue', selectedItems.value)
}
</script>

<style scoped>
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease;
}
.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
}
</style>
