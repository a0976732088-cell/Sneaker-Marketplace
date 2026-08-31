package com.project.sneakermarket.advertisement.repository;

import com.project.sneakermarket.advertisement.entity.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {
}
