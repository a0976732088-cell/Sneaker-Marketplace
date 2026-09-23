import { onBeforeUnmount, onMounted, ref } from "vue";

export function useAdBlockDetection() {
  const adBlockStatus = ref("checking");

  let baitElement = null;

  function isBaitBlocked(element) {
    if (!element || !document.body.contains(element)) {
      return true;
    }

    const style = window.getComputedStyle(element);
    const rect = element.getBoundingClientRect();

    return (
      style.display === "none" ||
      style.visibility === "hidden" ||
      Number.parseFloat(style.opacity) === 0 ||
      rect.width === 0 ||
      rect.height === 0
    );
  }

  async function detectAdBlock() {
    adBlockStatus.value = "checking";

    baitElement?.remove();

    baitElement = document.createElement("div");
    baitElement.className = "ad ads adsbox advertisement ad-banner";
    baitElement.setAttribute("aria-hidden", "true");

    Object.assign(baitElement.style, {
      position: "absolute",
      left: "-10000px",
      top: "-10000px",
      width: "1px",
      height: "1px",
      pointerEvents: "none",
    });

    document.body.appendChild(baitElement);

    await new Promise((resolve) => requestAnimationFrame(resolve));
    await new Promise((resolve) => window.setTimeout(resolve, 100));

    adBlockStatus.value = isBaitBlocked(baitElement) ? "detected" : "clear";

    baitElement?.remove();
    baitElement = null;
  }

  onMounted(detectAdBlock);

  onBeforeUnmount(() => {
    baitElement?.remove();
    baitElement = null;
  });

  return { adBlockStatus, detectAdBlock };
}
