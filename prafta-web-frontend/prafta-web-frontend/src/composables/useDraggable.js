// useDraggable.js
import { ref, onUnmounted } from "vue";

export function useDraggable(initialX = 200, initialY = 150) {
  const position = ref({ x: initialX, y: initialY });
  let offsetX = 0;
  let offsetY = 0;
  let dragging = false;

  function startDrag(e) {
    dragging = true;
    offsetX = e.clientX - position.value.x;
    offsetY = e.clientY - position.value.y;
    document.addEventListener("mousemove", onDrag);
    document.addEventListener("mouseup", stopDrag);
  }

  function onDrag(e) {
    if (!dragging) return;
    position.value = {
      x: e.clientX - offsetX,
      y: e.clientY - offsetY,
    };
  }

  function stopDrag() {
    dragging = false;
    document.removeEventListener("mousemove", onDrag);
    document.removeEventListener("mouseup", stopDrag);
  }

  onUnmounted(() => stopDrag());

  return { position, startDrag };
}
