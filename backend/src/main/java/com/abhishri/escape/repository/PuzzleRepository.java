package com.abhishri.escape.repository;

import com.abhishri.escape.domain.puzzle.Puzzle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PuzzleRepository extends JpaRepository<Puzzle, String> {

    List<Puzzle> findByRoomId(String roomId);
}
