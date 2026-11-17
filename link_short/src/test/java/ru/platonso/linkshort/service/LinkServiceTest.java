package ru.platonso.linkshort.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.platonso.linkshort.config.AppConfig;
import ru.platonso.linkshort.domain.LinkExpiredException;
import ru.platonso.linkshort.domain.LinkLimitReachedException;
import ru.platonso.linkshort.domain.ShortLink;
import ru.platonso.linkshort.repository.InMemoryLinkRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class LinkServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z");
    private LinkService service;
    private InMemoryLinkRepository repository;
    private UUID alice;
    @BeforeEach void setUp() {
        repository = new InMemoryLinkRepository();
        service = new LinkService(repository, new AppConfig("clck.ru", 3, 10, Duration.ofHours(24), Duration.ofSeconds(30)), Clock.fixed(NOW, ZoneOffset.UTC));
        alice = UUID.randomUUID();
    }
    @Test void createsShortLink() { assertTrue(service.toShortUrl(service.create(alice, "https://example.org/a", null)).startsWith("clck.ru/")); }
    @Test void acceptsHttpUrl() { assertEquals("http", service.create(alice, "http://example.org", null).target().getScheme()); }
    @Test void rejectsUrlWithoutScheme() { assertThrows(IllegalArgumentException.class, () -> service.create(alice, "example.org", null)); }
    @Test void rejectsNonHttpUrl() { assertThrows(IllegalArgumentException.class, () -> service.create(alice, "ftp://example.org", null)); }
    @Test void defaultLimitIsConfigured() { assertEquals(3, service.create(alice, "https://example.org", null).clickLimit()); }
    @Test void customLimitIsStored() { assertEquals(7, service.create(alice, "https://example.org", 7).clickLimit()); }
    @Test void rejectsZeroLimit() { assertThrows(IllegalArgumentException.class, () -> service.create(alice, "https://example.org", 0)); }
    @Test void rejectsLimitAboveConfiguredMaximum() { assertThrows(IllegalArgumentException.class, () -> service.create(alice, "https://example.org", 11)); }
    @Test void givesDifferentCodesForSameUrlAndDifferentUsers() { assertNotEquals(service.create(alice, "https://example.org", null).code(), service.create(UUID.randomUUID(), "https://example.org", null).code()); }
    @Test void givesDifferentCodesForOneUsersLinks() { assertNotEquals(service.create(alice, "https://example.org", null).code(), service.create(alice, "https://example.org", null).code()); }
    @Test void openingIncrementsCounter() { ShortLink link = service.create(alice, "https://example.org", 2); assertEquals(1, service.open(link.code()).clicks()); }
    @Test void blocksAfterLimit() { ShortLink link = service.create(alice, "https://example.org", 1); service.open(link.code()); assertThrows(LinkLimitReachedException.class, () -> service.open(link.code())); }
    @Test void ownerCanChangeLimit() { ShortLink link = service.create(alice, "https://example.org", 3); service.changeLimit(alice, link.code(), 5); assertEquals(5, link.clickLimit()); }
    @Test void nonOwnerCannotChangeLimit() { ShortLink link = service.create(alice, "https://example.org", 3); assertThrows(SecurityException.class, () -> service.changeLimit(UUID.randomUUID(), link.code(), 4)); }
    @Test void nonOwnerCannotDelete() { ShortLink link = service.create(alice, "https://example.org", 3); assertThrows(SecurityException.class, () -> service.delete(UUID.randomUUID(), link.code())); }
    @Test void ownerCanDelete() { ShortLink link = service.create(alice, "https://example.org", 3); service.delete(alice, link.code()); assertThrows(IllegalArgumentException.class, () -> service.open(link.code())); }
    @Test void returnsOnlyOwnersLinks() { service.create(alice, "https://one.org", null); service.create(UUID.randomUUID(), "https://two.org", null); assertEquals(1, service.list(alice).size()); }
    @Test void expiredLinkIsRemovedBeforeOpen() {
        LinkService oldService = new LinkService(repository, new AppConfig("clck.ru", 1, 10, Duration.ofHours(1), Duration.ofSeconds(30)), Clock.fixed(NOW, ZoneOffset.UTC));
        ShortLink link = oldService.create(alice, "https://example.org", null);
        LinkService laterService = new LinkService(repository, new AppConfig("clck.ru", 1, 10, Duration.ofHours(1), Duration.ofSeconds(30)), Clock.fixed(NOW.plus(Duration.ofHours(2)), ZoneOffset.UTC));
        assertThrows(LinkExpiredException.class, () -> laterService.open(link.code()));
        assertTrue(repository.findByCode(link.code()).isEmpty());
    }
    @Test void cleanupDeletesExpiredLinks() {
        ShortLink link = service.create(alice, "https://example.org", null);
        LinkService laterService = new LinkService(repository, new AppConfig("clck.ru", 1, 10, Duration.ofHours(1), Duration.ofSeconds(30)), Clock.fixed(NOW.plus(Duration.ofHours(25)), ZoneOffset.UTC));
        assertEquals(1, laterService.cleanupExpired()); assertTrue(repository.findByCode(link.code()).isEmpty());
    }
}
