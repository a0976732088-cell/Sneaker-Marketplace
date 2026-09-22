<template>
  <section class="map-view">
    <aside class="map-sidebar">
      <div class="map-heading">
        <div>
          <span class="eyebrow">EVENT MAP</span>
          <h2>活動地圖</h2>
        </div>

        <span class="event-count">
          {{ mapEvents.length }} 個地點
        </span>
      </div>

      <p v-if="eventsWithoutLocation > 0" class="location-hint">
        另有 {{ eventsWithoutLocation }} 個活動尚未提供地圖座標。
      </p>

      <div v-if="mapEvents.length" class="location-list">
        <button
          v-for="event in mapEvents"
          :key="event.id"
          type="button"
          class="location-card"
          :class="{ active: selectedEventId === event.id }"
          @click="focusEvent(event)"
        >
          <span
            class="type-dot"
            :style="{ backgroundColor: event.color }"
          ></span>

          <span class="card-content">
            <strong>{{ event.title }}</strong>

            <span>
              {{ event.venueName || "未提供場地名稱" }}
            </span>

            <small>
              {{ event.address || "未提供詳細地址" }}
            </small>

            <small>
              {{ formatDateTime(event.startAt) }}
            </small>
          </span>
        </button>
      </div>

      <div v-else class="empty-location">
        <strong>本月份沒有可顯示的活動地點</strong>
        <p>活動必須同時具有緯度與經度，才會出現在地圖上。</p>
      </div>
    </aside>

    <div class="map-panel">
      <div
        ref="mapElement"
        class="leaflet-map"
        aria-label="活動地圖"
      ></div>
    </div>
  </section>
</template>

<script setup>
import {
  computed,
  nextTick,
  onMounted,
  onUnmounted,
  ref,
  watch,
} from "vue";

import L from "leaflet";
import "leaflet/dist/leaflet.css";

const props = defineProps({
  events: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits([
  "open-detail",
]);

const mapElement = ref(null);
const selectedEventId = ref(null);

let map = null;
let markerLayer = null;

const markerByEventId = new Map();

function hasCoordinateValue(value) {
  return (
    value !== null &&
    value !== undefined &&
    value !== ""
  );
}

const mapEvents = computed(() =>
  props.events.filter((event) => {
    if (
      !hasCoordinateValue(event.latitude) ||
      !hasCoordinateValue(event.longitude)
    ) {
      return false;
    }

    const latitude =
      Number(event.latitude);

    const longitude =
      Number(event.longitude);

    return (
      Number.isFinite(latitude) &&
      Number.isFinite(longitude) &&
      latitude >= -90 &&
      latitude <= 90 &&
      longitude >= -180 &&
      longitude <= 180
    );
  }),
);
const eventsWithoutLocation = computed(
  () =>
    props.events.length -
    mapEvents.value.length,
);

function formatDateTime(value) {
  if (!value) {
    return "未提供活動時間";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "未提供活動時間";
  }

  return date.toLocaleString("zh-TW", {
    month: "numeric",
    day: "numeric",
    weekday: "short",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
}

function createPopupContent(event) {
  const container =
    document.createElement("div");

  container.className =
    "event-map-popup";

  const title =
    document.createElement("strong");

  title.textContent =
    event.title || "活動";

  const venue =
    document.createElement("span");

  venue.textContent =
    event.venueName ||
    event.address ||
    "未提供活動地點";

  const time =
    document.createElement("small");

  time.textContent =
    formatDateTime(event.startAt);

  const detailButton =
    document.createElement("button");

  detailButton.type = "button";
  detailButton.textContent = "查看活動";
  detailButton.addEventListener(
    "click",
    () => emit(
      "open-detail",
      event,
    ),
  );

  container.append(
    title,
    venue,
    time,
    detailButton,
  );

  return container;
}

function renderMarkers() {
  if (!map || !markerLayer) {
    return;
  }

  markerLayer.clearLayers();
  markerByEventId.clear();

  const bounds = [];

  for (const event of mapEvents.value) {
    const latitude =
      Number(event.latitude);

    const longitude =
      Number(event.longitude);

    const marker =
      L.circleMarker(
        [latitude, longitude],
        {
          radius: 9,
          color: "#ffffff",
          weight: 3,
          fillColor:
            event.color || "#e7664e",
          fillOpacity: 1,
        },
      );

    marker.bindPopup(
      createPopupContent(event),
      {
        minWidth: 210,
      },
    );

    marker.on("click", () => {
      selectedEventId.value =
        event.id;
    });

    marker.addTo(markerLayer);

    markerByEventId.set(
      event.id,
      marker,
    );

    bounds.push([
      latitude,
      longitude,
    ]);
  }

  if (bounds.length === 1) {
    map.setView(
      bounds[0],
      14,
    );

    return;
  }

  if (bounds.length > 1) {
    map.fitBounds(bounds, {
      padding: [36, 36],
      maxZoom: 14,
    });

    return;
  }

  /*
   * 沒有座標時顯示台灣全景。
   */
  map.setView(
    [23.7, 120.96],
    7,
  );
}

function focusEvent(event) {
  if (!map) {
    return;
  }

  const marker =
    markerByEventId.get(
      event.id,
    );

  if (!marker) {
    return;
  }

  selectedEventId.value =
    event.id;

  map.flyTo(
    marker.getLatLng(),
    15,
    {
      duration: 0.7,
    },
  );

  marker.openPopup();
}

onMounted(async () => {
  await nextTick();

  map = L.map(
    mapElement.value,
    {
      zoomControl: true,
    },
  ).setView(
    [23.7, 120.96],
    7,
  );

  L.tileLayer(
    "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
    {
      maxZoom: 19,
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    },
  ).addTo(map);

  markerLayer =
    L.layerGroup().addTo(map);

  renderMarkers();

  /*
   * 避免剛切換分頁時 Leaflet 尺寸判斷錯誤。
   */
  window.setTimeout(() => {
    map?.invalidateSize();
  }, 0);
});

watch(
  mapEvents,
  async () => {
    await nextTick();
    renderMarkers();
    map?.invalidateSize();
  },
  {
    deep: true,
  },
);

onUnmounted(() => {
  markerByEventId.clear();

  if (map) {
    map.remove();
    map = null;
  }

  markerLayer = null;
});
</script>

<style scoped>
.map-view {
  display: grid;
  grid-template-columns: minmax(250px, 320px) minmax(0, 1fr);
  min-height: 620px;
  overflow: hidden;
  border: 1px solid #e3e6e8;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 12px 30px rgb(31 41 55 / 8%);
}

.map-sidebar {
  display: flex;
  min-height: 0;
  flex-direction: column;
  border-right: 1px solid #e3e6e8;
  background: #fff;
}

.map-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 22px 20px 16px;
}

.eyebrow {
  color: #8b9297;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.map-heading h2 {
  margin: 4px 0 0;
  color: #202326;
  font-size: 23px;
}

.event-count {
  flex-shrink: 0;
  padding: 5px 10px;
  border-radius: 999px;
  background: #eef1f0;
  color: #596166;
  font-size: 12px;
  font-weight: 700;
}

.location-hint {
  margin: 0 20px 14px;
  padding: 10px 12px;
  border-radius: 9px;
  background: #fff5e8;
  color: #8a5b1f;
  font-size: 12px;
  line-height: 1.6;
}

.location-list {
  display: grid;
  min-height: 0;
  gap: 8px;
  padding: 0 12px 16px;
  overflow-y: auto;
}

.location-card {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  gap: 10px;
  width: 100%;
  padding: 14px;
  border: 1px solid #e4e6e7;
  border-radius: 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    transform 0.2s;
}

.location-card:hover,
.location-card.active {
  border-color: #e7664e;
  box-shadow: 0 8px 20px rgb(231 102 78 / 12%);
  transform: translateY(-1px);
}

.type-dot {
  width: 10px;
  height: 10px;
  margin-top: 5px;
  border-radius: 50%;
}

.card-content {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.card-content strong {
  overflow: hidden;
  color: #202326;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-content span {
  color: #4f585e;
  font-size: 13px;
}

.card-content small {
  color: #858c91;
  font-size: 12px;
  line-height: 1.5;
}

.empty-location {
  margin: auto 20px;
  padding: 24px 14px;
  color: #697079;
  text-align: center;
}

.empty-location strong {
  display: block;
  margin-bottom: 8px;
  color: #30363a;
}

.empty-location p {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
}

.map-panel {
  min-width: 0;
  min-height: 620px;
}

.leaflet-map {
  width: 100%;
  height: 100%;
  min-height: 620px;
}

:deep(.event-map-popup) {
  display: grid;
  gap: 7px;
  min-width: 180px;
}

:deep(.event-map-popup strong) {
  color: #202326;
  font-size: 14px;
}

:deep(.event-map-popup span),
:deep(.event-map-popup small) {
  color: #697079;
}

:deep(.event-map-popup button) {
  margin-top: 4px;
  padding: 8px 12px;
  border: 0;
  border-radius: 7px;
  background: #e7664e;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

@media (max-width: 820px) {
  .map-view {
    grid-template-columns: minmax(0, 1fr);
  }

  .map-sidebar {
    max-height: 330px;
    border-right: 0;
    border-bottom: 1px solid #e3e6e8;
  }

  .map-panel,
  .leaflet-map {
    min-height: 480px;
  }
}
</style>