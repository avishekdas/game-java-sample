package com.abhishri.escape.repository;

import com.abhishri.escape.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, String> {
}
