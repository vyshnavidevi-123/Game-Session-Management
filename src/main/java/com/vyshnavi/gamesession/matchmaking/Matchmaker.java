package com.vyshnavi.gamesession.matchmaking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;

/**
 * Thread-safe, skill-based matchmaking queue. Pure Java (no framework) so the algorithm can be
 * unit-tested in isolation.
 *
 * <p>Tickets are kept sorted by rating. A match is a run of {@code playersPerMatch} adjacent
 * tickets whose rating spread fits inside an acceptable window. The window starts at
 * {@code baseWindow} and widens by {@code widenPerSecond} for every second the longest-waiting
 * player in the group has queued, capped at {@code maxWindow}, so nobody waits forever.
 *
 * <p>Complexity: enqueue/cancel O(log n); a matching pass is O(n * k) for k players per match.
 */
public final class Matchmaker {

    public record Ticket(String playerId, int rating, long enqueuedAtMillis) {}

    public record Match(List<Ticket> players) {}

    private static final Comparator<Ticket> ORDER =
            Comparator.comparingInt(Ticket::rating).thenComparing(Ticket::playerId);

    private final int playersPerMatch;
    private final int baseWindow;
    private final int widenPerSecond;
    private final int maxWindow;
    private final LongSupplier clock;

    private final ReentrantLock lock = new ReentrantLock();
    private final TreeSet<Ticket> queue = new TreeSet<>(ORDER);
    private final Map<String, Ticket> byPlayer = new HashMap<>();

    public Matchmaker(int playersPerMatch, int baseWindow, int widenPerSecond, int maxWindow,
                      LongSupplier clock) {
        if (playersPerMatch < 2) {
            throw new IllegalArgumentException("playersPerMatch must be >= 2");
        }
        if (baseWindow < 0 || widenPerSecond < 0 || maxWindow < baseWindow) {
            throw new IllegalArgumentException("invalid rating window configuration");
        }
        this.playersPerMatch = playersPerMatch;
        this.baseWindow = baseWindow;
        this.widenPerSecond = widenPerSecond;
        this.maxWindow = maxWindow;
        this.clock = clock;
    }

    /** Adds a player to the queue. Returns false if the player is already queued. */
    public boolean enqueue(String playerId, int rating) {
        lock.lock();
        try {
            if (byPlayer.containsKey(playerId)) {
                return false;
            }
            Ticket t = new Ticket(playerId, rating, clock.getAsLong());
            byPlayer.put(playerId, t);
            queue.add(t);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /** Removes a waiting player. Returns true if the player was queued. */
    public boolean cancel(String playerId) {
        lock.lock();
        try {
            Ticket t = byPlayer.remove(playerId);
            return t != null && queue.remove(t);
        } finally {
            lock.unlock();
        }
    }

    public boolean isQueued(String playerId) {
        lock.lock();
        try {
            return byPlayer.containsKey(playerId);
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    /** Forms every currently possible match and removes the matched players from the queue. */
    public List<Match> drainMatches() {
        lock.lock();
        try {
            long now = clock.getAsLong();
            List<Ticket> sorted = new ArrayList<>(queue);
            List<Match> matches = new ArrayList<>();
            int i = 0;
            while (i + playersPerMatch <= sorted.size()) {
                List<Ticket> group = sorted.subList(i, i + playersPerMatch);
                if (fits(group, now)) {
                    matches.add(new Match(List.copyOf(group)));
                    i += playersPerMatch;
                } else {
                    i++;
                }
            }
            for (Match m : matches) {
                for (Ticket t : m.players()) {
                    queue.remove(t);
                    byPlayer.remove(t.playerId());
                }
            }
            return matches;
        } finally {
            lock.unlock();
        }
    }

    private boolean fits(List<Ticket> group, long now) {
        int spread = group.get(group.size() - 1).rating() - group.get(0).rating();
        long longestWaitMillis = 0;
        for (Ticket t : group) {
            longestWaitMillis = Math.max(longestWaitMillis, now - t.enqueuedAtMillis());
        }
        long window = baseWindow + (longestWaitMillis / 1000) * widenPerSecond;
        return spread <= Math.min(window, maxWindow);
    }
}
