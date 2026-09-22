// 前台追蹤
import {
  recordAdvertisementClick,
  recordAdvertisementView,
} from "@/service/advertisementService";

export function useAdvertisementTracking() {
  // 至少 50% 可見
  const VIEWABILITY_THRESHOLD = 0.5;
  // 連續可見一秒
  const VIEWABILITY_DURATION_MS = 1000;
  // set去重viewed
  const viewedAdIds = new Set();
  // 儲存每個 HTML 元素的廣告、比例與計時器
  const viewabilityStates = new WeakMap();
  const activeStates = new Set();

  // 瀏覽器的可視區域觀察器
  let viewabilityObserver = null;
  let visibilityListenerRegistered = false;

  // 上報view
  function trackAdvertisementView(ad) {
    const adId = Number(ad?.adId);

    if (!Number.isInteger(adId) || adId <= 0 || viewedAdIds.has(adId)) {
      return;
    }

    viewedAdIds.add(adId);

    recordAdvertisementView(adId).catch((error) => {
      viewedAdIds.delete(adId);
      console.error("廣告曝光紀錄失敗：", error);
    });
  }
  // 上報clicks，點擊不需要使用set
  function trackAdvertisementClick(ad) {
    const adId = Number(ad?.adId);

    if (!Number.isInteger(adId) || adId <= 0) {
      return;
    }

    recordAdvertisementClick(adId).catch((error) => {
      console.error("廣告點擊紀錄失敗：", error);
    });
  }

  // 計時器清除，當廣告低於 50% 可見時，取消尚未完成的一秒計時。
  function clearViewabilityTimer(state) {
    if (state.timerId === null) {
      return;
    }

    window.clearTimeout(state.timerId);
    state.timerId = null;
  }
  // 一秒計時器，讓廣告不會立即曝光，只會啟動一秒倒數
  function startViewabilityTimer(state) {
    const adId = Number(state.ad?.adId);

    if (
      !Number.isInteger(adId) ||
      adId <= 0 ||
      viewedAdIds.has(adId) ||
      state.timerId !== null ||
      document.visibilityState !== "visible"
    ) {
      return;
    }

    state.timerId = window.setTimeout(() => {
      state.timerId = null;

      if (
        state.intersectionRatio >= VIEWABILITY_THRESHOLD &&
        document.visibilityState === "visible"
      ) {
        trackAdvertisementView(state.ad);
      }
    }, VIEWABILITY_DURATION_MS);
  }

  // 元件卸載後未滿一秒取消計算，回來後重新計算
  function handleVisibilityChange() {
    activeStates.forEach((state) => {
      if (document.visibilityState !== "visible") {
        clearViewabilityTimer(state);
        return;
      }

      if (state.intersectionRatio >= VIEWABILITY_THRESHOLD) {
        startViewabilityTimer(state);
      }
    });
  }
  // IntersectionObserver瀏覽器內建的網頁 API
  function getViewabilityObserver() {
    if (viewabilityObserver !== null) {
      return viewabilityObserver;
    }

    viewabilityObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          const state = viewabilityStates.get(entry.target);

          if (!state) {
            return;
          }

          state.intersectionRatio = entry.intersectionRatio;

          if (
            entry.isIntersecting &&
            entry.intersectionRatio >= VIEWABILITY_THRESHOLD
          ) {
            startViewabilityTimer(state);
          } else {
            clearViewabilityTimer(state);
          }
        });
      },
      {
        threshold: [0, VIEWABILITY_THRESHOLD],
      },
    );

    if (!visibilityListenerRegistered) {
      document.addEventListener("visibilitychange", handleVisibilityChange);

      visibilityListenerRegistered = true;
    }

    return viewabilityObserver;
  }

  const vViewableAd = {
    mounted(element, binding) {
      const state = {
        ad: binding.value,
        intersectionRatio: 0,
        timerId: null,
      };

      viewabilityStates.set(element, state);
      activeStates.add(state);

      getViewabilityObserver().observe(element);
    },

    updated(element, binding) {
      const state = viewabilityStates.get(element);

      if (!state) {
        return;
      }

      const previousAdId = Number(state.ad?.adId);
      const nextAdId = Number(binding.value?.adId);

      state.ad = binding.value;

      if (previousAdId === nextAdId) {
        return;
      }

      clearViewabilityTimer(state);

      if (state.intersectionRatio >= VIEWABILITY_THRESHOLD) {
        startViewabilityTimer(state);
      }
    },

    beforeUnmount(element) {
      const state = viewabilityStates.get(element);

      if (state) {
        clearViewabilityTimer(state);
        activeStates.delete(state);
      }

      viewabilityObserver?.unobserve(element);
      viewabilityStates.delete(element);
    },
  };

  function disposeAdvertisementTracking() {
    activeStates.forEach((state) => {
      clearViewabilityTimer(state);
    });

    activeStates.clear();

    if (viewabilityObserver !== null) {
      viewabilityObserver.disconnect();
      viewabilityObserver = null;
    }

    if (visibilityListenerRegistered) {
      document.removeEventListener("visibilitychange", handleVisibilityChange);

      visibilityListenerRegistered = false;
    }
  }

  return {
    trackAdvertisementClick,
    vViewableAd,
    disposeAdvertisementTracking,
  };
}
