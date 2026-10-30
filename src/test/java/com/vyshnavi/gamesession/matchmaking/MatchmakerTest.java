package com.vyshnavi.gamesession.matchmaking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MatchmakerTest {

    private AtomicLong clock;
    private Matchmaker matchmaker;

    @BeforeEach
    void setUp() {
        clock = new AtomicLong(0);
        // 1v1, start window 100, +50 per second waited, capped at 400
        matchmaker = new Matchmaker(2, 100, 50, 400, clock::get);
    }

    @Test
    void matchesPlayersInsideRatingWindow() {
        matchmaker.enqueue("a", 1000);
        matchmaker.enqueue("b", 1080);

        List<Matchmaker.Match> matches = matchmaker.drainMatches();

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).players()).extracting(Matchmaker.Ticket::playerId)
                .containsExactlyInAnyOrder("a", "b");
        assertThat(matchmaker.size()).isZero();
    }

    @Test
    void doesNotMatchPlayersOutsideWindow() {
        matchmaker.enqueue("a", 1000);
        matchmaker.enqueue("b", 1300);

        assertThat(matchmaker.drainMatches()).isEmpty();
        assertThat(matchmaker.size()).isEqualTo(2);
    }

    @Test
    void windowWidensTheLongerAPlayerWaits() {
        matchmaker.enqueue("a", 1000);
        matchmaker.enqueue("b", 1300);
        clock.set(4_000); // window = 100 + 4 * 50 = 300

        assertThat(matchmaker.drainMatches()).hasSize(1);
    }

    @Test
    void windowIsCappedAtMaximum() {
        matchmaker.enqueue("a", 1000);
        matchmaker.enqueue("b", 1500);
        clock.set(600_000); // would be huge without the cap; cap is 400

        assertThat(matchmaker.drainMatches()).isEmpty();
    }

    @Test
    void formsMultipleMatchesInOnePass() {
        matchmaker.enqueue("a", 1000);
        matchmaker.enqueue("b", 1010);
        matchmaker.enqueue("c", 2000);
        matchmaker.enqueue("d", 2010);

        assertThat(matchmaker.drainMatches()).hasSize(2);
    }

    @Test
    void rejectsDuplicateEnqueue() {
        assertThat(matchmaker.enqueue("a", 1000)).isTrue();
        assertThat(matchmaker.enqueue("a", 1000)).isFalse();
        assertThat(matchmaker.size()).isEqualTo(1);
    }

    @Test
    void cancelRemovesWaitingPlayer() {
        matchmaker.enqueue("a", 1000);

        assertThat(matchmaker.cancel("a")).isTrue();
        assertThat(matchmaker.cancel("a")).isFalse();
        assertThat(matchmaker.isQueued("a")).isFalse();
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> new Matchmaker(1, 100, 50, 400, clock::get))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Matchmaker(2, 500, 50, 400, clock::get))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void concurrentRequestsNeverAssignAPlayerToTwoMatches() throws Exception {
        Matchmaker shared = new Matchmaker(2, 5000, 0, 5000, System::currentTimeMillis);
        int players = 2000;
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch start = new CountDownLatch(1);
        Set<String> matched = ConcurrentHashMap.newKeySet();
        AtomicInteger duplicates = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < players; i++) {
            final int n = i;
            futures.add(pool.submit(() -> {
                start.await();
                shared.enqueue("p" + n, 1000 + n % 50);
                if (n % 100 == 0) {
                    record(shared, matched, duplicates);
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        record(shared, matched, duplicates);
        pool.shutdown();

        assertThat(duplicates.get()).isZero();
        assertThat(matched).hasSize(players);
        assertThat(shared.size()).isZero();
    }

    private static void record(Matchmaker m, Set<String> matched, AtomicInteger duplicates) {
        for (Matchmaker.Match match : m.drainMatches()) {
            for (Matchmaker.Ticket t : match.players()) {
                if (!matched.add(t.playerId())) {
                    duplicates.incrementAndGet();
                }
            }
        }
    }
}
