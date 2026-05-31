package com.abhishri.escape.service;

import com.abhishri.escape.config.seed.CombinationPuzzleSeed;
import com.abhishri.escape.config.seed.ItemSeed;
import com.abhishri.escape.config.seed.ItemUsePuzzleSeed;
import com.abhishri.escape.config.seed.PuzzleSeed;
import com.abhishri.escape.config.seed.RiddlePuzzleSeed;
import com.abhishri.escape.config.seed.RoomObjectSeed;
import com.abhishri.escape.config.seed.RoomSeed;
import com.abhishri.escape.config.seed.SequencePuzzleSeed;
import com.abhishri.escape.config.seed.WorldSeed;
import com.abhishri.escape.domain.InventoryItem;
import com.abhishri.escape.domain.ObjectType;
import com.abhishri.escape.domain.Room;
import com.abhishri.escape.domain.RoomObject;

import java.util.ArrayList;
import java.util.List;
import com.abhishri.escape.domain.puzzle.CombinationPuzzle;
import com.abhishri.escape.domain.puzzle.ItemUsePuzzle;
import com.abhishri.escape.domain.puzzle.Puzzle;
import com.abhishri.escape.domain.puzzle.RiddlePuzzle;
import com.abhishri.escape.domain.puzzle.SequencePuzzle;
import com.abhishri.escape.repository.InventoryItemRepository;
import com.abhishri.escape.repository.PuzzleRepository;
import com.abhishri.escape.repository.RoomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class WorldSeedService {

    private static final Logger log = LoggerFactory.getLogger(WorldSeedService.class);

    @Value("${escape.world.seed-file}")
    private String seedFile;

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final WorldSeedValidator validator;
    private final RoomRepository roomRepository;
    private final PuzzleRepository puzzleRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public WorldSeedService(ResourceLoader resourceLoader,
                            ObjectMapper objectMapper,
                            WorldSeedValidator validator,
                            RoomRepository roomRepository,
                            PuzzleRepository puzzleRepository,
                            InventoryItemRepository inventoryItemRepository) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.roomRepository = roomRepository;
        this.puzzleRepository = puzzleRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @PostConstruct
    public void init() {
        seedIfEmpty();
    }

    public void seedIfEmpty() {
        if (roomRepository.count() > 0) {
            log.debug("World already seeded — skipping");
            return;
        }

        try {
            Resource resource = resourceLoader.getResource(seedFile);
            WorldSeed seed = objectMapper.readValue(resource.getInputStream(), WorldSeed.class);
            validator.validate(seed);

            for (ItemSeed s : seed.items) {
                InventoryItem item = new InventoryItem();
                item.setId(s.id);
                item.setName(s.name);
                item.setDescription(s.description);
                item.setAssetKey(s.assetKey);
                inventoryItemRepository.save(item);
            }

            for (RoomSeed s : seed.rooms) {
                Room room = new Room();
                room.setId(s.id);
                room.setName(s.name);
                room.setDescription(s.description);
                room.setConnectedRoomIds(new ArrayList<>(s.connectedRoomIds));
                room.setPuzzleIds(new ArrayList<>(s.puzzleIds));
                List<RoomObject> objects = new ArrayList<>();
                if (s.objects != null) {
                    for (RoomObjectSeed os : s.objects) {
                        RoomObject obj = new RoomObject();
                        obj.setId(os.id);
                        obj.setLabel(os.label);
                        obj.setObjectType(ObjectType.valueOf(os.objectType));
                        obj.setPuzzleId(os.puzzleId);
                        obj.setPickupItemId(os.pickupItemId);
                        obj.setInteractable(os.interactable);
                        objects.add(obj);
                    }
                }
                room.setObjects(objects);
                roomRepository.save(room);
            }

            for (PuzzleSeed s : seed.puzzles) {
                puzzleRepository.save(mapPuzzle(s));
            }

            log.info("World seeded: {} rooms, {} puzzles, {} items",
                seed.rooms.size(), seed.puzzles.size(), seed.items.size());

        } catch (IOException e) {
            throw new IllegalStateException("Failed to read world seed file: " + seedFile, e);
        }
    }

    private Puzzle mapPuzzle(PuzzleSeed s) {
        Puzzle p;
        if (s instanceof CombinationPuzzleSeed c) {
            CombinationPuzzle cp = new CombinationPuzzle();
            cp.setExpectedCode(c.expectedCode);
            cp.setDigitCount(c.digitCount);
            p = cp;
        } else if (s instanceof RiddlePuzzleSeed r) {
            RiddlePuzzle rp = new RiddlePuzzle();
            rp.setQuestionText(r.questionText);
            rp.setExpectedAnswer(r.expectedAnswer);
            rp.setCaseSensitive(r.caseSensitive);
            p = rp;
        } else if (s instanceof SequencePuzzleSeed sq) {
            SequencePuzzle sp = new SequencePuzzle();
            sp.setExpectedSequence(new ArrayList<>(sq.expectedSequence));
            sp.setAvailableItems(new ArrayList<>(sq.availableItems));
            p = sp;
        } else if (s instanceof ItemUsePuzzleSeed i) {
            ItemUsePuzzle ip = new ItemUsePuzzle();
            ip.setRequiredItemId(i.requiredItemId);
            ip.setTargetObjectId(i.targetObjectId);
            ip.setOutcomeMessage(i.outcomeMessage);
            p = ip;
        } else {
            throw new IllegalStateException("Unknown puzzle seed type: " + s.getClass());
        }

        p.setId(s.id);
        p.setRoomId(s.roomId);
        p.setDescription(s.description);
        p.setRewardItemId(s.rewardItemId);
        if (s.prerequisitePuzzleIds != null) {
            p.setPrerequisitePuzzleIds(new ArrayList<>(s.prerequisitePuzzleIds));
        }
        return p;
    }
}
