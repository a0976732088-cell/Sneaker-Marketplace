package com.project.sneakermarket.commerce.logistics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CvsStoreImportService {

    private final EcpayStoreListClient ecpayStoreListClient;
    private final CvsStoreRepository cvsStoreRepository;

    /**
     * 一次匯入全台 22 個縣市的 7-ELEVEN 門市。
     * 每個縣市最多匯入 20 間。
     */
    public int importAllTaiwanStores() {

        // ECPay 只呼叫一次
        List<EcpayStoreListClient.CvsStore> stores =
                ecpayStoreListClient.getStores("UNIMART");

        // 每個縣市目前已經匯入幾間
        Map<String, Integer> cityCount = new HashMap<>();

        // 22 個縣市
        List<String> cities = List.of(
                "基隆市",
                "台北市",
                "新北市",
                "桃園市",
                "新竹市",
                "新竹縣",
                "苗栗縣",
                "台中市",
                "彰化縣",
                "南投縣",
                "雲林縣",
                "嘉義市",
                "嘉義縣",
                "台南市",
                "高雄市",
                "屏東縣",
                "宜蘭縣",
                "花蓮縣",
                "台東縣",
                "澎湖縣",
                "金門縣",
                "連江縣"
        );

        // 初始化每個縣市的數量
        for (String city : cities) {
            cityCount.put(city, 0);
        }

        int totalCount = 0;

        for (EcpayStoreListClient.CvsStore store : stores) {

            String city = findCity(store.address(), cities);

            // 找不到縣市就跳過
            if (city == null) {
                continue;
            }

            // 每個縣市最多 20 間
            if (cityCount.get(city) >= 20) {
                continue;
            }

            // 如果資料庫已經有這間門市，就跳過
            boolean exists = cvsStoreRepository
                    .findByChainAndCity("UNIMART", city)
                    .stream()
                    .anyMatch(existing ->
                            existing.getStoreId().equals(store.storeId())
                    );

            if (exists) {
                // 已存在的資料也算在這個縣市的 20 間額度裡
                cityCount.put(
                        city,
                        cityCount.get(city) + 1
                );
                continue;
            }

            CvsStore cvsStore = new CvsStore();

            cvsStore.setStoreId(store.storeId());
            cvsStore.setStoreName(store.storeName());
            cvsStore.setAddress(store.address());
            cvsStore.setPhone(store.phone());

            cvsStore.setChain("UNIMART");
            cvsStore.setCity(city);

            // 經緯度之後補
            cvsStore.setLatitude(null);
            cvsStore.setLongitude(null);

            cvsStoreRepository.save(cvsStore);

            cityCount.put(
                    city,
                    cityCount.get(city) + 1
            );

            totalCount++;
        }

        return totalCount;
    }

    /**
     * 從門市地址判斷所在縣市。
     */
    private String findCity(
            String address,
            List<String> cities
    ) {

        for (String city : cities) {
            if (address.contains(city)) {
                return city;
            }
        }

        return null;
    }
}