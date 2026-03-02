// useFieldWatcher.js
import { watch } from "vue";

/**
 * 배열 내 객체들의 모든 필드를 자동 감시하는 유틸
 *
 * @param {Ref<Array<Object>>} listRef - 감시할 리스트 ref([])
 * @param {Function} onFieldChange - 필드 변경 시 실행할 콜백 (item, key, oldVal, newVal)
 * @param {Array<String>} excludeKeys - 감시 제외할 필드 (예: ['CHK'])
 */
export function useFieldWatcher(listRef, onFieldChange, excludeKeys = []) {
  // 이미 등록한 watcher 방지용 (메모리 누수 방지)
  const watchers = [];

  const registerWatchers = () => {
    // 기존 watcher 해제
    watchers.forEach((unwatch) => unwatch());
    watchers.length = 0;

    listRef.value.forEach((item) => {
      Object.keys(item).forEach((key) => {
        if (excludeKeys.includes(key)) return;

        const unwatch = watch(
          () => item[key],
          (newVal, oldVal) => {
            if (newVal !== oldVal) {
              onFieldChange(item, key, oldVal, newVal);
            }
          }
        );

        watchers.push(unwatch);
      });
    });
  };

  // 처음에 한번 등록
  registerWatchers();

  // 리스트 자체가 바뀌면 다시 등록
  watch(
    listRef,
    () => {
      registerWatchers();
    },
    { deep: false } // 배열 자체만 감지
  );
}
