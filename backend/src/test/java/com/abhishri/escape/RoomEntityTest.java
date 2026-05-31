package com.abhishri.escape;

import com.abhishri.escape.domain.ObjectType;
import com.abhishri.escape.domain.Room;
import com.abhishri.escape.domain.RoomObject;
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
        RoomObject clock = new RoomObject();
        clock.setId("wall_clock");
        clock.setLabel("Wall Clock");
        clock.setObjectType(ObjectType.PUZZLE);
        clock.setPuzzleId("puzzle_clock");
        clock.setInteractable(true);

        RoomObject desk = new RoomObject();
        desk.setId("reception_desk");
        desk.setLabel("Reception Desk");
        desk.setObjectType(ObjectType.ITEM);
        desk.setPickupItemId("desk_key");
        desk.setInteractable(true);

        Room room = new Room();
        room.setId("room_foyer");
        room.setName("The Entry Foyer");
        room.setDescription("Cold marble floor.");
        room.setConnectedRoomIds(List.of("room_reading_hall"));
        room.setObjects(List.of(clock, desk));
        room.setPuzzleIds(List.of("puzzle_clock"));

        em.persistAndFlush(room);
        em.clear();

        Room loaded = roomRepository.findById("room_foyer").orElseThrow();
        assertThat(loaded.getName()).isEqualTo("The Entry Foyer");
        assertThat(loaded.getConnectedRoomIds()).containsExactlyInAnyOrder("room_reading_hall");
        assertThat(loaded.getPuzzleIds()).containsExactlyInAnyOrder("puzzle_clock");
        assertThat(loaded.getObjects()).hasSize(2);
        assertThat(loaded.getObjectIds()).containsExactlyInAnyOrder("wall_clock", "reception_desk");

        RoomObject loadedClock = loaded.getObjects().stream()
                .filter(o -> "wall_clock".equals(o.getId())).findFirst().orElseThrow();
        assertThat(loadedClock.getLabel()).isEqualTo("Wall Clock");
        assertThat(loadedClock.getObjectType()).isEqualTo(ObjectType.PUZZLE);
        assertThat(loadedClock.getPuzzleId()).isEqualTo("puzzle_clock");
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

    @Test
    void containsObject_returnsTrueWhenObjectInRoom() {
        RoomObject obj = new RoomObject();
        obj.setId("cipher_wheel");

        Room room = new Room();
        room.setId("room_archives");
        room.setName("The Archives");
        room.setObjects(List.of(obj));

        assertThat(room.containsObject("cipher_wheel")).isTrue();
        assertThat(room.containsObject("nonexistent")).isFalse();
    }
}
