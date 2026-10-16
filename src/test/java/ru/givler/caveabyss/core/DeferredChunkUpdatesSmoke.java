package ru.givler.caveabyss.core;

import java.util.*;
import ru.givler.caveabyss.client.DeferredChunkUpdates;

public final class DeferredChunkUpdatesSmoke {
    static void check() {
        DeferredChunkUpdates queue = new DeferredChunkUpdates();
        Object firstWorld = new Object(), secondWorld = new Object();
        Set<Long> loaded = new HashSet<>();
        List<Integer> applied = new ArrayList<>();
        for (int section = 0; section < 4; section++) {
            final int index = section;
            queue.submit(firstWorld, 1L, () -> applied.add(index), loaded::contains);
        }
        queue.submit(firstWorld, 1L, () -> applied.add(4), loaded::contains); // Delta after snapshots.
        if (!applied.isEmpty()) throw new AssertionError("Snapshot applied to an unloaded placeholder");
        loaded.add(1L);
        queue.drain(firstWorld, loaded::contains);
        if (!applied.equals(Arrays.asList(0, 1, 2, 3, 4))) throw new AssertionError("Snapshot/delta ordering changed");
        queue.drain(firstWorld, loaded::contains);
        if (applied.size() != 5) throw new AssertionError("Data applied twice");
        queue.submit(firstWorld, 2L, () -> applied.add(99), loaded::contains);
        queue.unload(2L);
        loaded.add(2L);
        queue.drain(firstWorld, loaded::contains);
        queue.submit(firstWorld, 3L, () -> applied.add(99), loaded::contains);
        loaded.add(3L);
        queue.drain(secondWorld, loaded::contains);
        if (applied.size() != 5) throw new AssertionError("Stale world/chunk data survived unload");
        System.out.println("Deferred snapshots: placeholder delay, ordered delivery, unload and world transition passed");
    }
}
