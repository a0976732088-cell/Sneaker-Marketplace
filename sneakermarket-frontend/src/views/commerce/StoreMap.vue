<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

let map = null

// ============================================================
// 往外通知父層（結帳頁）：使用者選好門市了
// ============================================================

const emit = defineEmits(['storeSelected'])

const loading = ref(true)

// ============================================================
// 展示用門市清單（高雄，20 間，涵蓋多個行政區）
// ============================================================

const mapStores = [
  {
    storeId: '893312',
    storeName: '強民門市',
    address: '高雄市小港區小港路182號',
    phone: '07-812-4098',
    type: '7-ELEVEN',
    latitude: 22.564791,
    longitude: 120.337043
  },
  {
    storeId: '政德',
    storeName: '政德門市',
    address: '高雄市左營區政德路130、132號',
    phone: '07-345-1959',
    type: '7-ELEVEN',
    latitude: 22.67388,
    longitude: 120.29749
  },
  {
    storeId: '林德',
    storeName: '林德門市',
    address: '高雄市苓雅區林德街37號',
    phone: '07-721-3735',
    type: '7-ELEVEN',
    latitude: 22.624,
    longitude: 120.317466
  },
  {
    storeId: '前鎮',
    storeName: '高雄門市',
    address: '高雄市前鎮區復興三路116號',
    phone: '07-336-7915',
    type: '7-ELEVEN',
    latitude: 22.610935,
    longitude: 120.310728
  },
  {
    storeId: '鳳山',
    storeName: '善志門市',
    address: '高雄市鳳山區善志街27號',
    phone: '07-726-0748',
    type: '7-ELEVEN',
    latitude: 22.601749,
    longitude: 120.335487
  },
  {
    storeId: '後驛',
    storeName: '後驛門市',
    address: '高雄市三民區察哈爾一街148號1樓',
    phone: '07-322-8514',
    type: '7-ELEVEN',
    latitude: 22.647715,
    longitude: 120.303685
  },
  {
    storeId: '新本館',
    storeName: '新本館門市',
    address: '高雄市三民區大裕路119號',
    phone: '07-392-4430',
    type: '7-ELEVEN',
    latitude: 22.656121,
    longitude: 120.32851
  },
  {
    storeId: '楠梓',
    storeName: '楠梓門市',
    address: '高雄市楠梓區藍昌路356、358號',
    phone: '07-364-8856',
    type: '7-ELEVEN',
    latitude: 22.722855,
    longitude: 120.29137
  },
  {
    storeId: '鼓山',
    storeName: '美珍門市',
    address: '高雄市鼓山區明誠四路23號',
    phone: '07-550-2901',
    type: '7-ELEVEN',
    latitude: 22.660897,
    longitude: 120.291354
  },
  {
    storeId: '新興',
    storeName: '興村門市',
    address: '高雄市新興區五福二路92號',
    phone: '07-282-9812',
    type: '7-ELEVEN',
    latitude: 22.625634,
    longitude: 120.308063
  },
  {
    storeId: '仁武',
    storeName: '雄鼎門市',
    address: '高雄市仁武區仁雄路346號',
    phone: '07-374-7409',
    type: '7-ELEVEN',
    latitude: 22.674416,
    longitude: 120.335243
  },
  {
    storeId: '橋頭',
    storeName: '橋鄰門市',
    address: '高雄市橋頭區成功南路151號',
    phone: '07-611-2945',
    type: '7-ELEVEN',
    latitude: 22.752265,
    longitude: 120.312758
  },
  {
    storeId: '大寮',
    storeName: '開封門市',
    address: '高雄市大寮區大寮路688號',
    phone: '07-788-0424',
    type: '7-ELEVEN',
    latitude: 22.591398,
    longitude: 120.414872
  },
  {
    storeId: '鳥松',
    storeName: '翔盛門市',
    address: '高雄市鳥松區神農路479號',
    phone: '07-735-1489',
    type: '7-ELEVEN',
    latitude: 22.658928,
    longitude: 120.381481
  },
  {
    storeId: '旗津',
    storeName: '旗津門市',
    address: '高雄市旗津區海安路9號',
    phone: '07-571-9252',
    type: '7-ELEVEN',
    latitude: 22.613937,
    longitude: 120.269367
  },
  {
    storeId: '鹽埕',
    storeName: '駁藝門市',
    address: '高雄市鹽埕區大勇路29號',
    phone: '07-521-2477',
    type: '7-ELEVEN',
    latitude: 22.62124,
    longitude: 120.282363
  },
  {
    storeId: '前金',
    storeName: '前金門市',
    address: '高雄市前金區自強二路172號1樓',
    phone: '07-241-9355',
    type: '7-ELEVEN',
    latitude: 22.627881,
    longitude: 120.29449
  },
  {
    storeId: '大社',
    storeName: '大民門市',
    address: '高雄市大社區三民路213號',
    phone: '07-352-8550',
    type: '7-ELEVEN',
    latitude: 22.724882,
    longitude: 120.348639
  },
  {
    storeId: '岡山',
    storeName: '岡好門市',
    address: '高雄市岡山區岡山路612-2號',
    phone: '07-621-6214',
    type: '7-ELEVEN',
    latitude: 22.802841,
    longitude: 120.291109
  },
  {
    storeId: '林園',
    storeName: '艾文門市',
    address: '高雄市林園區東林西路99號',
    phone: '07-641-4848',
    type: '7-ELEVEN',
    latitude: 22.503744,
    longitude: 120.393146
  }
]

// ============================================================
// 建立地圖
// ============================================================

onMounted(() => {

  // ----------------------------------------------------------
  // 1. 建立 Leaflet 地圖
  // ----------------------------------------------------------

  map = L.map('store-map').setView(
    [22.6273, 120.3014],
    10
  )

  // ----------------------------------------------------------
  // 2. OpenStreetMap
  // ----------------------------------------------------------

  L.tileLayer(
    'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    {
      attribution: '&copy; OpenStreetMap contributors'
    }
  ).addTo(map)

  // ----------------------------------------------------------
  // 3. 在地圖上放置門市
  // ----------------------------------------------------------

  mapStores.forEach(store => {

    const marker = L.marker([
      store.latitude,
      store.longitude
    ])
      .addTo(map)

    // --------------------------------------------------------
    // 點擊 Marker 後顯示門市資訊
    // --------------------------------------------------------

    marker.bindPopup(`
      <div>
        <strong>7-ELEVEN ${store.storeName}</strong>
        <br>
        地址：${store.address}
        <br>
        電話：${store.phone}
        <br><br>
        <button
          id="select-${store.storeId}"
          style="
            padding: 6px 12px;
            border: none;
            border-radius: 4px;
            background: #198754;
            color: white;
            cursor: pointer;
          "
        >
          選擇此門市
        </button>
      </div>
    `)

    // --------------------------------------------------------
    // Popup 開啟後，讓「選擇此門市」按鈕可以使用
    // --------------------------------------------------------

    marker.on('popupopen', () => {

      const button = document.getElementById(
        `select-${store.storeId}`
      )

      if (button) {

        button.addEventListener('click', () => {

          console.log('選擇的門市：', store)

          // 往外通知父層（結帳頁）使用者選了哪間門市
          emit('storeSelected', store)

          marker.closePopup()

        })

      }

    })

  })

  // ----------------------------------------------------------
  // 4. 地圖自動調整範圍，讓所有門市都看得到
  // ----------------------------------------------------------

  const bounds = L.latLngBounds(
    mapStores.map(store => [
      store.latitude,
      store.longitude
    ])
  )

  map.fitBounds(bounds, {
    padding: [40, 40]
  })

  loading.value = false

})


// ============================================================
// 元件離開時清除 Leaflet
// ============================================================

onBeforeUnmount(() => {

  if (map) {

    map.remove()
    map = null

  }

})
</script>


<template>

  <div>

    <!-- ========================================================
         地圖
         ======================================================== -->

    <div
      id="store-map"
      style="
        width: 100%;
        height: 500px;
        border-radius: 8px;
      "
    ></div>


    <!-- ========================================================
         載入狀態 / 可選門市數量
         ======================================================== -->

    <p
      v-if="loading"
      class="text-secondary mt-2"
    >
      門市資料載入中...
    </p>

    <p
      v-else
      class="text-secondary mt-2"
    >
      目前開放 {{ mapStores.length }} 間門市可供選擇
    </p>

  </div>

</template>