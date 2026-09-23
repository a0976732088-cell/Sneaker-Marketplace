import { computed, ref, watch } from "vue";
import { getCurrentMember } from "@/service/authService";
import { claimMemberPopup } from "@/utils/advertisementPopupMemory";

export function useAdvertisementPopup({
  popupAds,
  hasPopupPlacement,
}) {
  const isLoggedIn = ref(false);
  const sessionChecked = ref(false);
  const allowPopupThisMount = ref(false);

  async function loadLoginStatus() {
    if (!hasPopupPlacement()) {
      sessionChecked.value = true;
      return;
    }

    try {
      await getCurrentMember();
      isLoggedIn.value = true;
    } catch (error) {
      isLoggedIn.value = false;

      if (error.response?.status !== 401) {
        console.error("會員登入狀態讀取失敗：", error);
      }
    } finally {
      sessionChecked.value = true;
    }
  }

  watch(
    [sessionChecked, isLoggedIn, popupAds],
    ([checked, loggedIn, popups]) => {
      if (!checked) return;

      if (!loggedIn) {
        allowPopupThisMount.value = true;
        return;
      }

      if (allowPopupThisMount.value) return;

      const hasMemberPopup = popups.some(
        (ad) => Number(ad.sortOrder) === 1,
      );

      if (!hasMemberPopup) return;

      allowPopupThisMount.value = claimMemberPopup();
    },
    { immediate: true },
  );

  const currentPopup = computed(() => {
    if (!sessionChecked.value || !allowPopupThisMount.value) {
      return null;
    }

    const popupOrder = isLoggedIn.value ? 1 : 0;

    return (
      popupAds.value.find(
        (ad) => Number(ad.sortOrder) === popupOrder,
      ) || null
    );
  });

  return {
    isLoggedIn,
    currentPopup,
    loadLoginStatus,
  };
}