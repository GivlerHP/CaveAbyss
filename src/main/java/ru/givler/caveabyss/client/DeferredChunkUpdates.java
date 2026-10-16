package ru.givler.caveabyss.client;

import java.util.*;
import java.util.function.LongPredicate;

/** Keeps packet order until the real client chunk exists; never applies to an EmptyChunk. */
public final class DeferredChunkUpdates {
    private Object world;
    private final Map<Long, List<Runnable>> pending = new LinkedHashMap<>();

    public void submit(Object currentWorld, long chunk, Runnable update, LongPredicate ready) {
        useWorld(currentWorld);
        pending.computeIfAbsent(chunk, key -> new ArrayList<Runnable>()).add(update);
        drain(currentWorld, ready);
    }

    public void drain(Object currentWorld, LongPredicate ready) {
        useWorld(currentWorld);
        Iterator<Map.Entry<Long, List<Runnable>>> iterator = pending.entrySet().iterator();
        List<Runnable> apply = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<Long, List<Runnable>> entry = iterator.next();
            if (!ready.test(entry.getKey())) continue;
            apply.addAll(entry.getValue());
            iterator.remove();
        }
        for (Runnable update : apply) update.run();
    }

    public void unload(long chunk) { pending.remove(chunk); }
    private void useWorld(Object currentWorld) {
        if (world == currentWorld) return;
        pending.clear();
        world = currentWorld;
    }
}
