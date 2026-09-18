import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import {
  getMyEventFollows,
  followEvent,
  unfollowEvent,
} from "@/service/calendarService";

export function useEventFollows() {
  const router = useRouter();
  const followedIds = ref(new Set());
  const followLoadingIds = ref(new Set());
  const followError = ref("");
  const followStatus = ref("loading"); // loading、ready、guest、error

  async function loadMyFollows() {
    followStatus.value = "loading";
    followError.value = "";

    try {
      const response = await getMyEventFollows();
      const rows = Array.isArray(response.data) ? response.data : [];

      followedIds.value = new Set(rows.map((item) => item.eventId));
      followStatus.value = "ready";
    } catch (error) {
      followedIds.value = new Set();

      if (error.response?.status === 401) {
        followStatus.value = "guest";
        return;
      }

      followStatus.value = "error";
      followError.value = "無法讀取關注活動。";
      console.error("讀取關注活動失敗：", error);
    }
  }

  async function toggleFollow(event) {
    if (followLoadingIds.value.has(event.id)) return;
    const startTime = new Date(
      String(event.startAt).replace(" ", "T"),
    ).getTime();

    if (
      !followedIds.value.has(event.id) &&
      Number.isFinite(startTime) &&
      startTime <= Date.now()
    ) {
      followError.value = "活動已開始或結束，無法新增關注。";
      return;
    }

    followLoadingIds.value = new Set([...followLoadingIds.value, event.id]);
    followError.value = "";

    try {
      const nextIds = new Set(followedIds.value);

      if (nextIds.has(event.id)) {
        await unfollowEvent(event.id);
        nextIds.delete(event.id);
      } else {
        await followEvent(event.id, 60);
        nextIds.add(event.id);
      }

      followedIds.value = nextIds;
    } catch (error) {
      if (error.response?.status === 401) {
        await router.push({
          path: "/login",
          query: { redirect: "/calendar" },
        });
        return;
      }

      followError.value = "更新關注狀態失敗，請再試一次。";
      console.error("更新關注狀態失敗：", error);
    } finally {
      const nextLoadingIds = new Set(followLoadingIds.value);
      nextLoadingIds.delete(event.id);
      followLoadingIds.value = nextLoadingIds;
    }
  }

  onMounted(loadMyFollows);

  return {
    followedIds,
    followLoadingIds,
    followError,
    toggleFollow,
    followStatus,
  };
}
