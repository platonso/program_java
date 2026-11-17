package ru.platonso.linkshort.service;

import ru.platonso.linkshort.config.AppConfig;
import ru.platonso.linkshort.domain.LinkExpiredException;
import ru.platonso.linkshort.domain.ShortLink;
import ru.platonso.linkshort.repository.LinkRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class LinkService {
    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private final LinkRepository repository;
    private final AppConfig config;
    private final Clock clock;
    private final AtomicLong sequence = new AtomicLong();
    public LinkService(LinkRepository repository, AppConfig config, Clock clock) {
        this.repository = repository; this.config = config; this.clock = clock;
    }
    public ShortLink create(UUID ownerId, String rawUrl, Integer requestedLimit) {
        int limit = requestedLimit == null ? config.defaultClickLimit() : requestedLimit;
        if (limit < 1 || limit > config.maxClickLimit()) throw new IllegalArgumentException("Лимит должен быть от 1 до " + config.maxClickLimit());
        Instant now = clock.instant();
        ShortLink link = new ShortLink(nextCode(ownerId), validateUrl(rawUrl), ownerId, now, now.plus(config.ttl()), limit);
        repository.save(link); return link;
    }
    public LinkResult open(String code) {
        ShortLink link = requireActive(code);
        link.registerClick(clock.instant());
        return result(link);
    }
    public void changeLimit(UUID ownerId, String code, int limit) {
        ShortLink link = requireOwner(ownerId, code);
        if (limit > config.maxClickLimit()) throw new IllegalArgumentException("Максимальный лимит: " + config.maxClickLimit());
        link.changeLimit(limit);
    }
    public void delete(UUID ownerId, String code) { repository.delete(requireOwner(ownerId, code).code()); }
    public Collection<ShortLink> list(UUID ownerId) { cleanupExpired(); return repository.findByOwnerId(ownerId); }
    public int cleanupExpired() { return repository.deleteExpired(clock.instant()); }
    public String toShortUrl(ShortLink link) { return config.baseUrl() + "/" + link.code(); }
    private ShortLink requireActive(String code) {
        ShortLink link = repository.findByCode(normalizeCode(code)).orElseThrow(() -> new IllegalArgumentException("Короткая ссылка не найдена или уже удалена"));
        if (link.isExpired(clock.instant())) { repository.delete(link.code()); throw new LinkExpiredException(); }
        return link;
    }
    private ShortLink requireOwner(UUID ownerId, String code) {
        ShortLink link = requireActive(code);
        if (!link.ownerId().equals(ownerId)) throw new SecurityException("Операция доступна только создателю ссылки");
        return link;
    }
    private LinkResult result(ShortLink link) { return new LinkResult(toShortUrl(link), link.target(), link.clicks(), link.clickLimit()); }
    private String nextCode(UUID ownerId) { return encode(sequence.incrementAndGet()) + encode(ownerId.getLeastSignificantBits() & 0xffffL); }
    private static String encode(long value) { long v = value; StringBuilder out = new StringBuilder(); do { out.append(ALPHABET[(int) (v % ALPHABET.length)]); v /= ALPHABET.length; } while (v > 0); return out.reverse().toString(); }
    private static String normalizeCode(String value) { int slash = value.lastIndexOf('/'); return value.substring(slash + 1).trim(); }
    private static URI validateUrl(String rawUrl) {
        try { URI uri = new URI(rawUrl.trim()); if (!uri.isAbsolute() || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) throw new IllegalArgumentException(); return uri; }
        catch (URISyntaxException | NullPointerException exception) { throw new IllegalArgumentException("Введите корректный http/https URL"); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Введите корректный http/https URL"); }
    }
}
