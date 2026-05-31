package com.abhishri.escape.repository;

import com.abhishri.escape.domain.GameSession;
import com.abhishri.escape.domain.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {

    Optional<GameSession> findByStatus(GameStatus status);
}
