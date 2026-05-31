package com.abhishri.escape.service;

import com.abhishri.escape.config.seed.PuzzleSeed;
import com.abhishri.escape.config.seed.WorldSeed;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class WorldSeedValidator {

    public void validate(WorldSeed seed) {
        Set<String> roomIds = seed.rooms.stream().map(r -> r.id).collect(Collectors.toSet());
        Set<String> itemIds = seed.items.stream().map(i -> i.id).collect(Collectors.toSet());
        Set<String> puzzleIds = seed.puzzles.stream().map(p -> p.id).collect(Collectors.toSet());

        for (PuzzleSeed p : seed.puzzles) {
            if (!roomIds.contains(p.roomId)) {
                throw new IllegalStateException(
                    "Seed error: puzzle '" + p.id + "' references unknown roomId '" + p.roomId + "'");
            }
            if (p.rewardItemId != null && !itemIds.contains(p.rewardItemId)) {
                throw new IllegalStateException(
                    "Seed error: puzzle '" + p.id + "' references unknown rewardItemId '" + p.rewardItemId + "'");
            }
            for (String prereqId : p.prerequisitePuzzleIds) {
                if (!puzzleIds.contains(prereqId)) {
                    throw new IllegalStateException(
                        "Seed error: puzzle '" + p.id + "' has unknown prerequisite '" + prereqId + "'");
                }
            }
        }
    }
}
