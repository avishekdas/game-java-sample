package com.abhishri.escape;

import com.abhishri.escape.domain.GameSession;
import com.abhishri.escape.domain.GameStatus;
import com.abhishri.escape.domain.PlayerInventory;
import com.abhishri.escape.repository.GameSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GameSessionEntityTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Test
    void roundTrip_persistsAllFields() {
        PlayerInventory inv = new PlayerInventory();
        inv.setId(UUID.randomUUID());

        GameSession session = new GameSession();
        session.setId(UUID.randomUUID());
        session.setCurrentRoomId("room_foyer");
        session.setStatus(GameStatus.IN_PROGRESS);
        session.setCreatedAt(LocalDateTime.now());
        session.setLastUpdatedAt(LocalDateTime.now());
        session.setInventory(inv);

        em.persistAndFlush(session);
        em.clear();

        GameSession loaded = gameSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(loaded.getCurrentRoomId()).isEqualTo("room_foyer");
        assertThat(loaded.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(loaded.getSolvedPuzzleIds()).isEmpty();
        assertThat(loaded.getInventory()).isNotNull();
        assertThat(loaded.getInventory().getId()).isEqualTo(inv.getId());
    }

    @Test
    void isComplete_returnsTrueWhenComplete() {
        GameSession session = new GameSession();
        session.setStatus(GameStatus.COMPLETE);
        assertThat(session.isComplete()).isTrue();
    }

    @Test
    void isComplete_returnsFalseWhenInProgress() {
        GameSession session = new GameSession();
        session.setStatus(GameStatus.IN_PROGRESS);
        assertThat(session.isComplete()).isFalse();
    }
}
