<!-- 活動列表與分頁 -->
<template>
  <div class="table-responsive">
    <table class="table table-hover align-middle event-table">
      <thead>
        <tr>
          <th class="event-name-heading">活動名稱</th>
          <th>類型</th>
          <th class="event-period-heading">活動期間</th>
          <th>關注人數</th>
          <th>通知狀態</th>
          <th>狀態</th>
          <th class="event-action-heading">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(event, index) in events" :key="event.eventId">
          <td class="event-name-cell">
            <strong>{{ event.title }}</strong>
            <small>#{{ event.eventId }}</small>
          </td>

          <td>
            <span
              class="event-type-badge"
              :style="{ backgroundColor: eventTypeInfo(event.eventType).color }"
            >
              {{ eventTypeLabel(event.eventType) }}
            </span>
          </td>

          <td class="event-period-cell">
            <span>{{ formatDate(event.startAt) }}</span>
            <span>{{ formatDate(event.endAt) }}</span>
          </td>

          <td>
            <span class="follow-count">
              {{ event.followCount ?? "—" }}
            </span>
          </td>

          <td>
            <span
              class="notice-status"
              :class="
                event.notificationCreated ? 'notice-ready' : 'notice-missing'
              "
            >
              <i></i>
              {{ event.notificationCreated ? "已排程" : "尚未建立" }}
            </span>
          </td>

          <td>
            <span class="event-status" :class="eventStatusClass(event)">
              {{ eventStatusLabel(event) }}
            </span>
          </td>

          <td
            class="event-action-cell"
            :class="{ 'menu-open': openMenuId === event.eventId }"
          >
            <div class="event-actions">
              <div class="more-menu">
                <button
                  type="button"
                  class="more-action"
                  aria-label="更多操作"
                  :aria-expanded="openMenuId === event.eventId"
                  :aria-controls="`event-menu-${event.eventId}`"
                  @click.stop="toggleMenu(event.eventId)"
                >
                  •••
                </button>

                <div
                  v-if="openMenuId === event.eventId"
                  :id="`event-menu-${event.eventId}`"
                  class="action-menu"
                  :class="{ 'open-up': index >= events.length - 2 }"
                  role="menu"
                  @click.stop
                >
                  <button
                    type="button"
                    class="action-menu-item"
                    role="menuitem"
                    @click="handleOpenDetail(event)"
                  >
                    查看詳情
                  </button>

                  <button
                    v-if="event.status !== 'CANCELLED'"
                    type="button"
                    class="action-menu-item"
                    role="menuitem"
                    @click="handleOpenEdit(event)"
                  >
                    編輯活動
                  </button>

                  <button
                    v-if="event.status !== 'CANCELLED'"
                    type="button"
                    class="action-menu-item danger"
                    role="menuitem"
                    @click="handleDelete(event.eventId)"
                  >
                    取消活動
                  </button>
                </div>
              </div>
            </div>
          </td>
        </tr>

        <tr v-if="events.length === 0">
          <td colspan="7" class="text-center text-muted py-4">
            查無相關活動資料
          </td>
        </tr>
      </tbody>
    </table>
  </div>

</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from "vue";
import { getEventType } from "@/constants/eventTypes";

defineProps({
  events: { type: Array, default: () => [] },
});

const emit = defineEmits(["open-detail", "open-edit", "delete"]);

const openMenuId = ref(null);

const closeMenu = () => {
  openMenuId.value = null;
};

const toggleMenu = (eventId) => {
  openMenuId.value = openMenuId.value === eventId ? null : eventId;
};

const handleOpenDetail = (event) => {
  closeMenu();
  emit("open-detail", event);
};

const handleOpenEdit = (event) => {
  closeMenu();
  emit("open-edit", event);
};

const handleDelete = (eventId) => {
  closeMenu();
  emit("delete", eventId);
};

onMounted(() => {
  document.addEventListener("click", closeMenu);
});

onBeforeUnmount(() => {
  document.removeEventListener("click", closeMenu);
});

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-TW", { hour12: false });
};

const eventTypeInfo = (type) => getEventType(type);
const eventTypeLabel = (type) => eventTypeInfo(type).label;

const eventStatusLabel = (event) => {
  if (event.status === "CANCELLED") return "已取消";
  if (event.status === "DRAFT") return "草稿";
  if (event.status === "INACTIVE") return "已停用";

  const now = new Date();
  const startAt = event.startAt ? new Date(event.startAt) : null;
  const endAt = event.endAt ? new Date(event.endAt) : null;

  if (startAt && now < startAt) return "即將開始";
  if (endAt && now > endAt) return "已結束";

  return "進行中";
};

const eventStatusClass = (event) => {
  const label = eventStatusLabel(event);

  return {
    即將開始: "status-upcoming",
    進行中: "status-active",
    已結束: "status-ended",
    草稿: "status-draft",
    已停用: "status-inactive",
    已取消: "status-cancelled",
  }[label];
};
</script>

<style scoped>
.table-responsive {
  max-width: 100%;
  min-height: 668px;
  overflow-x: auto;
  border: 1px solid #e3e4e0;
  border-radius: 5px;
  background: #ffffff;
}

.event-table {
  width: 100%;
  min-width: 900px;
  margin-bottom: 0;
}

.event-table thead th {
  padding: 10px 11px;
  border-bottom: 1px solid #dedfdb;
  color: #6c7069;
  background: #f7f7f5;
  font-size: 11px;
  font-weight: 650;
  white-space: nowrap;
}

.event-table tbody td {
  padding: 10px 11px;
  border-bottom: 1px solid #ecece9;
  color: #383b37;
  background: #ffffff;
  font-size: 11px;
  vertical-align: middle;
}

.event-table tbody tr {
  height: 62px;
}

.event-table tbody tr:last-child td {
  border-bottom: 0;
}

.event-table tbody tr:hover td {
  background: #fafaf8;
}

.event-name-heading {
  width: 25%;
}

.event-name-cell strong {
  display: block;
  max-width: 230px;
  overflow: hidden;
  color: #222420;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-name-cell small {
  display: block;
  margin-top: 3px;
  color: #949791;
  font-size: 10px;
}

.event-period-heading {
  width: 150px;
}

.event-period-cell span {
  display: block;
  white-space: nowrap;
}

.event-period-cell span + span {
  margin-top: 2px;
  color: #747870;
}

.event-type-badge,
.event-status,
.notice-status {
  display: inline-flex;
  align-items: center;
  font-size: 10px;
  font-weight: 650;
  white-space: nowrap;
}

.event-type-badge,
.event-status {
  padding: 5px 9px;
  border-radius: 999px;
}

.event-type-badge {
  color: #39403d;
}

.follow-count {
  color: #4f534d;
  font-weight: 700;
}

.notice-status {
  gap: 5px;
}

.notice-status i {
  width: 7px;
  height: 7px;
  flex: 0 0 7px;
  border-radius: 50%;
}

.notice-ready {
  color: #586d54;
}

.notice-ready i {
  background: #79a278;
}

.notice-missing {
  color: #84695b;
}

.notice-missing i {
  background: #d4a98d;
}

.status-upcoming {
  color: #526953;
  background: #deebdd;
}

.status-active {
  color: #526a82;
  background: #dce9f4;
}

.status-ended {
  color: #665966;
  background: #ebe4e9;
}

.status-draft {
  color: #6d706b;
  background: #ebebe8;
}

.status-inactive {
  color: #75633f;
  background: #f1e8d6;
}

.status-cancelled {
  color: #805653;
  background: #f0dddb;
}

.event-action-heading,
.event-action-cell {
  position: sticky;
  right: 0;
  width: 104px;
  min-width: 104px;
}

.event-action-heading {
  z-index: 3;
  background: #f7f7f5 !important;
  text-align: center;
}

.event-action-cell.menu-open {
  z-index: 10;
}

.event-action-cell {
  z-index: 2;
  background: #ffffff !important;
  box-shadow: -7px 0 10px rgb(17 17 17 / 4%);
}

.event-table tbody tr:hover .event-action-cell {
  background: #fafaf8 !important;
}

.event-actions {
  display: flex;
  align-items: center;
  justify-content: center;
}

.more-action {
  height: 28px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 650;
  cursor: pointer;
}

.more-action {
  width: 30px;
  padding: 0;
  border: 1px solid #dedfdb;
  color: #5c6059;
  background: #ffffff;
  letter-spacing: 1px;
}

.more-action:hover {
  border-color: #bfc1bb;
  background: #f3f3f0;
}

.more-menu {
  position: relative;
}

.action-menu {
  position: absolute;
  z-index: 20;
  top: 34px;
  right: 0;
  width: 126px;
  overflow: hidden;
  padding: 4px;
  border: 1px solid #dedfdb;
  border-radius: 5px;
  background: #ffffff;
  box-shadow: 0 10px 28px rgb(17 17 17 / 12%);
}

.action-menu.open-up {
  top: auto;
  bottom: 34px;
}

.action-menu-item {
  display: block;
  width: 100%;
  padding: 8px 10px;
  border: 0;
  border-radius: 3px;
  color: #464943;
  background: transparent;
  font-size: 11px;
  text-align: left;
  cursor: pointer;
}

.action-menu-item:hover {
  background: #f2f2ef;
}

.action-menu-item.danger {
  color: #9a554f;
}

.action-menu-item.danger:hover {
  background: #f7eae8;
}

@media (max-width: 768px) {
  .table-responsive {
    min-height: 0;
  }

  .event-table {
    min-width: 820px;
  }
}
</style>
