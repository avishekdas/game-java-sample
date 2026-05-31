package com.abhishri.escape;

import com.abhishri.escape.domain.Room;
import com.abhishri.escape.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RoomEntityTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    void roundTrip_persistsAllCollections() {
        Room room = new Room();
        room.setId("room_foyer");
        room.setName("The Entry Foyer");
        room.setDescription("Cold marble floor.");
        room.setConnectedRoomIds(List.of("room_reading_hall"));
        room.setObjectIds(List.of("wall_clock", "reception_desk"));
        room.setPuzzleIds(List.of("puzzle_clock"));

        em.persistAndFlush(room);
        em.clear();

        Room loaded = roomRepository.findById("room_foyer").orElseThrow();
        assertThat(loaded.getName()).isEqualTo("The Entry Foyer");
        assertThat(loaded.getConnectedRoomIds()).containsExactlyInAnyOrder("room_reading_hall");
        assertThat(loaded.getObjectIds()).containsExactlyInAnyOrder("wall_clock", "reception_desk");
        assertThat(loaded.getPuzzleIds()).containsExactlyInAnyOrder("puzzle_clock");
    }

    @Test
    void isConnectedTo_returnsTrueForAdjacentRoom() {
        Room room = new Room();
        room.setId("room_test");
        room.setName("Test Room");
        room.setConnectedRoomIds(List.of("room_other"));

        assertThat(room.isConnectedTo("room_other")).isTrue();
        assertThat(room.isConnectedTo("room_nope")).isFalse();
    }
}
