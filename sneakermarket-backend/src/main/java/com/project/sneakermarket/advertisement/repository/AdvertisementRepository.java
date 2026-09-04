package com.project.sneakermarket.advertisement.repository;

import com.project.sneakermarket.advertisement.entity.Advertisement;
import com.project.sneakermarket.advertisement.entity.AdvertisementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {
    List<Advertisement> findByAdStatusAndEndAtBefore(
            AdvertisementStatus adStatus,
            LocalDateTime endAt
    );
}
