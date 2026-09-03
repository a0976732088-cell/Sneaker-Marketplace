<!-- 查看詳情彈窗 -->
<template>
  <div
    class="modal fade show d-block"
    tabindex="-1"
    style="background-color: rgba(0, 0, 0, 0.5); z-index: 1050"
    @click.self="$emit('close')"
  >
    <div class="modal-dialog modal-dialog-centered modal-lg">
      <div class="modal-content shadow-lg border-0 rounded-4 position-relative">
        <button
          type="button"
          class="btn-close position-absolute top-0 end-0 m-3"
          style="z-index: 1060; cursor: pointer"
          @click="$emit('close')"
        ></button>

        <div class="modal-body text-center pt-4 px-4 pb-4">
          <p class="text-uppercase text-muted fw-bold tracking-wider mb-1">
            {{ event?.eventType || "EVENT DETAILS" }}
          </p>
          <h2 class="fw-bold mb-4">{{ event?.title }}</h2>

          <div
            class="event-image-container mb-4 overflow-hidden rounded-3 shadow-sm"
          >
            <img
              :src="`/api/calendar/event/${event?.eventId}/photo?t=${timestamp}`"
              class="img-fluid w-100 object-fit-cover"
              style="max-height: 350px"
              alt="Event Image"
              @error="(e) => (e.target.src = 'https://picsum.photos/800/400')"
            />
          </div>

          <div class="text-start bg-light p-3 rounded-3">
            <p class="mb-2"><strong>狀態：</strong> {{ event?.status }}</p>
            <p class="mb-2">
              <strong>開始時間：</strong> {{ formatDate(event?.startAt) }}
            </p>
            <p class="mb-2">
              <strong>結束時間：</strong> {{ formatDate(event?.endAt) }}
            </p>
            <p class="mb-2">
              <strong>描述：</strong> {{ event?.description || "無詳細描述" }}
            </p>
            <p v-if="event?.targetUrl" class="mb-0">
              <strong>連結：</strong>
              <a :href="event.targetUrl" target="_blank" rel="noopener">
                {{ event.targetUrl }}
              </a>
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";


defineProps({
  event: { type: Object, required: true },
});

defineEmits(["close"]);

const timestamp = ref(Date.now());

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-TW", { hour12: false });
};
</script>
