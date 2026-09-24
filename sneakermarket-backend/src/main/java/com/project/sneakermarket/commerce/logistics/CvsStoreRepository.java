package com.project.sneakermarket.commerce.logistics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CvsStoreRepository extends JpaRepository<CvsStore, Long> {

    List<CvsStore> findByChainAndCity(String chain, String city);

    List<CvsStore> findByChainAndCityAndStoreNameContaining(
            String chain,
            String city,
            String keyword
    );
}