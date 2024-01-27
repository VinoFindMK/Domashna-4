package com.ukim.finki.domashna2.repository;

import com.ukim.finki.domashna2.model.WineryInfo;
import com.ukim.finki.domashna2.model.WineryReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WineryRepository extends JpaRepository<WineryInfo, Long> {
    Page<WineryInfo> findAll(Pageable pageable);
    Optional<WineryInfo> findByName(String name);
    Page<WineryInfo> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT r.rating FROM WineryInfo w JOIN w.reviews r WHERE w.id = :wineryId")
    List<Float> getRatingsByWineryId(@Param("wineryId") Long wineryId);
}