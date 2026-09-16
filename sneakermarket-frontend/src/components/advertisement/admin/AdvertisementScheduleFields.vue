<script setup>
import { computed } from "vue";
import { VueDatePicker } from "@vuepic/vue-datepicker";
import "@vuepic/vue-datepicker/dist/main.css";
import { zhTW } from "date-fns/locale";

const props = defineProps({
  startAt: {
    type: [String, Date],
    default: "",
  },

  endAt: {
    type: [String, Date],
    default: "",
  },

  minimumStartAt: {
    type: Date,
    default: null,
  },

  isStartAtLocked: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits([
  "update:startAt",
  "update:endAt",
]);

const minimumEndAt = computed(() => {
  if (!props.startAt) {
    return props.startAt;
  }

  const startAt = new Date(props.startAt);

  return Number.isNaN(startAt.getTime())
    ? props.minimumStartAt
    : startAt;
});
</script>

<template>
  <div class="schedule-grid">
    <label>
      開始時間

      <VueDatePicker
        :model-value="startAt"
        :disabled="isStartAtLocked"
        :min-date="minimumStartAt"
        model-type="yyyy-MM-dd'T'HH:mm"
        :locale="zhTW"
        :teleport="true"
        six-weeks
        :formats="{ input: 'yyyy/MM/dd HH:mm' }"
        :time-config="{
          is24: true,
          timePickerInline: true,
        }"
        placeholder="選擇日期與時間"
        @update:model-value="
          emit('update:startAt', $event)
        "
      />
    </label>

    <label>
      結束時間

      <VueDatePicker
        :model-value="endAt"
        :disabled="!startAt"
        :min-date="minimumEndAt"
        model-type="yyyy-MM-dd'T'HH:mm"
        :locale="zhTW"
        :teleport="true"
        :transitions="false"
        six-weeks
        :formats="{ input: 'yyyy/MM/dd HH:mm' }"
        :time-config="{
          is24: true,
          timePickerInline: true,
        }"
        placeholder="選擇日期與時間"
        @update:model-value="
          emit('update:endAt', $event)
        "
      />
    </label>
  </div>
</template>

<style scoped>
.schedule-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.schedule-grid label {
  display: block;
  margin-bottom: 17px;
  color: #495567;
  font-size: 14px;
  font-weight: 700;
}

@media (max-width: 520px) {
  .schedule-grid {
    grid-template-columns: 1fr;
  }
}
</style>