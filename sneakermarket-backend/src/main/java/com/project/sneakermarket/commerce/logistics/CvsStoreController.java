package com.project.sneakermarket.commerce.logistics;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/logistics/cvs")
public class CvsStoreController {

    private final CvsStoreImportService cvsStoreImportService;
    private final CvsStoreRepository cvsStoreRepository;

    /**
     * 一次匯入全台 22 個縣市，
     * 每個縣市最多 20 間 7-ELEVEN。
     */
    @GetMapping("/import/all")
    public String importAllTaiwanStores() {

        int count =
                cvsStoreImportService.importAllTaiwanStores();

        return "成功匯入全台 7-ELEVEN 門市，共 " +
                count + " 間";
    }

    /**
     * 查詢指定縣市的門市。
     */
    @GetMapping("/stores")
    public List<CvsStore> getStores(
            @RequestParam(defaultValue = "UNIMART") String chain,
            @RequestParam(defaultValue = "高雄市") String city
    ) {
        return cvsStoreRepository.findByChainAndCity(
                chain,
                city
        );
    }
}