<template>
  <div class="location-picker">
    <div class="picker-header">
      <div>
        <strong>活動位置</strong>

        <p>有完整地址可直接定位；沒有詳細地址時，也可以點擊地圖標記。</p>
      </div>

      <div class="picker-actions">
        <button
          type="button"
          class="locate-button"
          :disabled="geocoding || (!normalizedAddress && !normalizedVenueName)"
          @click="locateAddress"
        >
          {{ geocoding ? "定位中…" : "搜尋定位" }}
        </button>

        <button
          v-if="hasCoordinates"
          type="button"
          class="clear-button"
          @click="clearLocation"
        >
          清除位置
        </button>
      </div>
    </div>

    <p
      v-if="geocodeMessage"
      class="geocode-message"
      :class="geocodeMessageType"
      aria-live="polite"
    >
      {{ geocodeMessage }}
    </p>

    <div
      ref="mapElement"
      class="picker-map"
      aria-label="活動位置選擇地圖"
    ></div>

    <p v-if="hasCoordinates" class="selected-location">
      位置已設定；若位置不精確，可直接點擊地圖重新標記。
    </p>

    <p class="map-attribution">
      地址搜尋與地圖資料 © OpenStreetMap contributors
    </p>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";

import L from "leaflet";
import "leaflet/dist/leaflet.css";

const props = defineProps({
  venueName: {
    type: String,
    default: "",
  },
  address: {
    type: String,
    default: "",
  },

  latitude: {
    type: [Number, String],
    default: null,
  },

  longitude: {
    type: [Number, String],
    default: null,
  },
});

const emit = defineEmits(["update:latitude", "update:longitude"]);

const mapElement = ref(null);
const geocoding = ref(false);
const geocodeMessage = ref("");
const geocodeMessageType = ref("success");

let map = null;
let selectedMarker = null;
let resizeObserver = null;
let resizeFrameId = null;
let lastGeocodeRequestAt = 0;

const geocodeCache = new Map();

const geocodingBaseUrl =
  import.meta.env.VITE_NOMINATIM_BASE_URL ||
  "https://nominatim.openstreetmap.org";

function toCoordinate(value) {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const number = Number(value);

  return Number.isFinite(number) ? number : null;
}

const normalizedVenueName = computed(() =>
  String(props.venueName || "").trim(),
);

const normalizedAddress = computed(() => String(props.address || "").trim());

const normalizedLatitude = computed(() => toCoordinate(props.latitude));

const normalizedLongitude = computed(() => toCoordinate(props.longitude));

const hasCoordinates = computed(
  () => normalizedLatitude.value !== null && normalizedLongitude.value !== null,
);

function updateMarker(latitude, longitude, moveMap = false) {
  if (!map) {
    return;
  }

  const lat = toCoordinate(latitude);

  const lng = toCoordinate(longitude);

  if (lat === null || lng === null) {
    if (selectedMarker) {
      selectedMarker.remove();
      selectedMarker = null;
    }

    return;
  }

  const latLng = [lat, lng];

  if (selectedMarker) {
    selectedMarker.setLatLng(latLng);
  } else {
    selectedMarker = L.circleMarker(latLng, {
      radius: 9,
      color: "#ffffff",
      weight: 3,
      fillColor: "#e7664e",
      fillOpacity: 1,
    }).addTo(map);
  }

  if (moveMap) {
    map.flyTo(latLng, 16, {
      duration: 0.7,
    });
  }
}

function applyCoordinates(latitude, longitude, moveMap = true) {
  emit("update:latitude", latitude);

  emit("update:longitude", longitude);

  updateMarker(latitude, longitude, moveMap);
}

function selectLocation(event) {
  const latitude = Number(event.latlng.lat.toFixed(6));

  const longitude = Number(event.latlng.lng.toFixed(6));

  applyCoordinates(latitude, longitude, false);

  geocodeMessage.value = "已使用地圖標記活動位置。";

  geocodeMessageType.value = "success";
}

function clearLocation() {
  emit("update:latitude", null);

  emit("update:longitude", null);

  updateMarker(null, null);

  geocodeMessage.value = "";
}

async function waitForRateLimit() {
  const elapsed = Date.now() - lastGeocodeRequestAt;

  const remaining = 1000 - elapsed;

  if (remaining > 0) {
    await new Promise((resolve) => window.setTimeout(resolve, remaining));
  }

  lastGeocodeRequestAt = Date.now();
}

async function searchAddress(address) {
  if (geocodeCache.has(address)) {
    return geocodeCache.get(address);
  }

  await waitForRateLimit();

  const query = new URLSearchParams({
    q: address,
    format: "jsonv2",
    limit: "1",
    countrycodes: "tw",
    "accept-language": "zh-TW",
  });

  const response = await fetch(
    `${geocodingBaseUrl}/search?${query.toString()}`,
    {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
    },
  );

  if (!response.ok) {
    throw new Error(`地址搜尋失敗：${response.status}`);
  }

  const results = await response.json();

  const result =
    Array.isArray(results) && results.length > 0 ? results[0] : null;

  geocodeCache.set(address, result);

  return result;
}

function simplifyTaiwanAddress(address) {
  if (!address) {
    return "";
  }

  return (
    address
      // 移除郵遞區號，例如 920。
      .replace(/^\d{3,6}\s*/, "")

      // 移除里名，例如新興里。
      .replace(/[\u4e00-\u9fff]{1,6}里/g, "")

      // 移除門牌號碼及後續內容。
      .replace(/\d+(?:之\d+)?號.*$/, "")

      // 移除巷弄以下資訊，保留主要道路。
      .replace(/\d+巷.*$/, "")

      .trim()
  );
}

async function locateAddress() {
  const address = normalizedAddress.value;

  const venueName = normalizedVenueName.value;

  const searchCandidates = [];

  if (address) {
    searchCandidates.push({
      value: address,
      source: "address",
    });
  }

  const roadFallback = simplifyTaiwanAddress(address);

  const alreadyIncluded = searchCandidates.some(
    (candidate) => candidate.value === roadFallback,
  );

  if (roadFallback && !alreadyIncluded) {
    searchCandidates.push({
      value: roadFallback,
      source: "road",
    });
  }

  if (venueName && venueName !== address) {
    searchCandidates.push({
      value: venueName,
      source: "venue",
    });
  }

  if (!searchCandidates.length) {
    geocodeMessage.value = "請先填寫活動地址或場地名稱。";

    geocodeMessageType.value = "error";

    return;
  }

  geocoding.value = true;
  geocodeMessage.value = "";

  try {
    let matchedResult = null;
    let matchedSource = null;

    for (const candidate of searchCandidates) {
      const result = await searchAddress(candidate.value);

      if (result) {
        matchedResult = result;
        matchedSource = candidate.source;

        break;
      }
    }

    if (!matchedResult) {
      geocodeMessage.value = "找不到這個地址或場地，請直接點擊地圖標記。";

      geocodeMessageType.value = "error";

      return;
    }

    const latitude = Number(matchedResult.lat);

    const longitude = Number(matchedResult.lon);

    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      throw new Error("搜尋結果沒有有效座標");
    }

    applyCoordinates(
      Number(latitude.toFixed(6)),
      Number(longitude.toFixed(6)),
      true,
    );

    let sourceMessage = "已依地址定位";

    if (matchedSource === "venue") {
      sourceMessage = "完整地址查無結果，已改用場地名稱定位";
    }

    if (matchedSource === "road") {
      sourceMessage = "只能定位到道路附近，請點擊地圖微調位置";
    }

    geocodeMessage.value = `${sourceMessage}：${
      matchedResult.display_name || venueName || address
    }`;

    geocodeMessageType.value = matchedSource === "road" ? "warning" : "success";
  } catch (error) {
    geocodeMessage.value = "目前無法搜尋位置，請稍後再試，或直接點擊地圖標記。";

    geocodeMessageType.value = "error";

    console.error("位置搜尋失敗：", error);
  } finally {
    geocoding.value = false;
  }
}

onMounted(async () => {
  await nextTick();

  const initialCenter = hasCoordinates.value
    ? [normalizedLatitude.value, normalizedLongitude.value]
    : [23.7, 120.96];

  const initialZoom = hasCoordinates.value ? 16 : 7;

  map = L.map(mapElement.value).setView(initialCenter, initialZoom);

  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution:
      '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
  }).addTo(map);

  map.on("click", selectLocation);

  updateMarker(props.latitude, props.longitude);

  resizeObserver = new ResizeObserver(() => {
    if (resizeFrameId !== null) {
      window.cancelAnimationFrame(resizeFrameId);
    }

    resizeFrameId = window.requestAnimationFrame(() => {
      map?.invalidateSize({
        pan: false,
      });

      resizeFrameId = null;
    });
  });

  resizeObserver.observe(mapElement.value);

  window.setTimeout(() => {
    map?.invalidateSize({
      pan: false,
    });
  }, 300);
});

watch(
  () => [props.latitude, props.longitude],
  ([latitude, longitude]) => {
    updateMarker(latitude, longitude);
  },
);

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect();
    resizeObserver = null;
  }

  if (resizeFrameId !== null) {
    window.cancelAnimationFrame(resizeFrameId);

    resizeFrameId = null;
  }

  if (map) {
    map.off("click", selectLocation);

    map.remove();
    map = null;
  }

  selectedMarker = null;
});
</script>

<style scoped>
.location-picker {
  margin-top: 14px;
}

.picker-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.picker-header strong {
  display: block;
  color: #30363a;
  font-size: 14px;
}

.picker-header p {
  margin: 4px 0 0;
  color: #747c82;
  font-size: 12px;
  line-height: 1.6;
}

.picker-actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}

.locate-button,
.clear-button {
  padding: 7px 11px;
  border-radius: 7px;
  background: #fff;
  font-size: 12px;
  cursor: pointer;
}

.locate-button {
  border: 1px solid #4285f4;
  color: #2767c5;
}

.locate-button:hover:not(:disabled) {
  background: #eef5ff;
}

.locate-button:disabled {
  border-color: #d8dcde;
  color: #9aa0a4;
  cursor: not-allowed;
}

.clear-button {
  border: 1px solid #d4d8da;
  color: #596166;
}

.clear-button:hover {
  border-color: #e7664e;
  color: #c84f3a;
}

.geocode-message {
  margin: 0 0 10px;
  padding: 9px 11px;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.6;
}

.geocode-message.success {
  background: #e8f5ec;
  color: #276d3d;
}

.geocode-message.error {
  background: #f8e9e5;
  color: #8c3b32;
}

.picker-map {
  width: 100%;
  height: 280px;
  overflow: hidden;
  border: 1px solid #dfe3e5;
  border-radius: 10px;
}

.selected-location {
  margin: 8px 0 0;
  color: #596166;
  font-size: 12px;
}

.map-attribution {
  margin: 4px 0 0;
  color: #949a9e;
  font-size: 10px;
}

.geocode-message.warning {
  background: #fff5e8;
  color: #8a5b1f;
}

@media (max-width: 650px) {
  .picker-header {
    flex-direction: column;
  }

  .picker-actions {
    width: 100%;
  }

  .locate-button,
  .clear-button {
    flex: 1;
  }
}
</style>
