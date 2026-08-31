<template>
  <component :is="tag" class="intro-sentences">
    <span v-for="(line, i) in lines" :key="i" class="intro-sentences__line">{{
      line
    }}</span>
  </component>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  text: { type: String, default: "" },
  tag: { type: String, default: "p" },
});

/**
 * 마침표(.)·쉼표(,) 뒤에서 줄을 나눈다(2026-08-31 사용자 지시).
 * 구분자 뒤에 공백이 올 때만 자르므로 "800~5,000자"의 천단위 쉼표나
 * 문장 끝 마침표는 쪼개지지 않는다. (lookbehind 미지원 브라우저 대비로 정규식 대신 순회)
 */
function splitSentences(text) {
  const out = [];
  let buf = "";
  for (let i = 0; i < text.length; i += 1) {
    const ch = text[i];
    buf += ch;
    const next = text[i + 1] || "";
    if ((ch === "." || ch === ",") && /\s/.test(next)) {
      out.push(buf.trim());
      buf = "";
    }
  }
  if (buf.trim()) out.push(buf.trim());
  return out;
}

const lines = computed(() => splitSentences(props.text));
</script>

<style scoped>
.intro-sentences {
  margin: 0;
}
.intro-sentences__line {
  display: block;
  /* 한 줄이 화면 폭을 넘어 접힐 때 한국어가 음절 단위로 갈라지지 않게 한다 */
  word-break: keep-all;
}
</style>
