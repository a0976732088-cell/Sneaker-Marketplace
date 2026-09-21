package com.project.sneakermarket.calendar.repository;

import com.project.sneakermarket.calendar.entity.GoogleCalendarConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleCalendarConnectionRepository
        extends JpaRepository<GoogleCalendarConnection, Long> {

    Optional<GoogleCalendarConnection>
    findByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);

    boolean existsByMemberId(Long memberId);
}