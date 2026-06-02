<template>
  <div class="viewHeader">
    <div class="title-group">
      <button class="icon-button">
        <img :src="favoritImgSrc" alt="icon" />
      </button>
      <span>{{ title }}</span>
    </div>

    <div class="btn-group">
      <button
        v-if="buttons?.search === 'Y'"
        class="btn btn-sm btn-primary"
        @click="$emit('search')"
      >
        조회
      </button>
      <button
        v-if="buttons?.create === 'Y'"
        class="btn btn-sm btn-primary"
        @click="$emit('create')"
      >
        생성
      </button>
      <button
        v-if="buttons?.save === 'Y'"
        class="btn btn-sm btn-primary"
        @click="$emit('save')"
      >
        저장
      </button>
      <button
        v-if="buttons?.delete === 'Y'"
        class="btn btn-sm btn-primary"
        @click="$emit('delete')"
      >
        삭제
      </button>
      <button
        v-if="buttons?.excel === 'Y'"
        class="btn btn-sm btn-primary"
        @click="$emit('excel')"
      >
        엑셀
      </button>
    </div>
  </div>
</template>

<script setup>
import { defineProps } from "vue";
import favorite_blank from "@/assets/img/favorite_blank.png";

defineProps({
  title: String,
  buttons: Object,
});

const favoritImgSrc = favorite_blank;
</script>

<style scoped>
.viewHeader {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
.title-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
</style>
