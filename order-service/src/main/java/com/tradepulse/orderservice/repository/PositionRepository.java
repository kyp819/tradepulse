package com.tradepulse.orderservice.repository;

import com.tradepulse.orderservice.model.Position;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Position p WHERE p.symbol = :symbol")
    Optional<Position> findBySymbolWithLock(@Param("symbol") String symbol);
}
