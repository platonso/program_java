package ru.platonso.linkshort.domain;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Aggregate containing the ownership, TTL and click-budget rules of a link. */
public final class ShortLink {
    private final String code;
    private final URI target;
    private final UUID ownerId;
    private final Instant createdAt;
    private final Instant expiresAt;
    private int clickLimit;
    private int clicks;

    public ShortLink(String code, URI target, UUID ownerId, Instant createdAt, Instant expiresAt, int clickLimit) {
        this.code = Objects.requireNonNull(code);
        this.target = Objects.requireNonNull(target);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        if (clickLimit < 1 || !expiresAt.isAfter(createdAt)) throw new IllegalArgumentException("Некорректные параметры ссылки");
        this.clickLimit = clickLimit;
    }
    public String code() { return code; }
    public URI target() { return target; }
    public UUID ownerId() { return ownerId; }
    public Instant createdAt() { return createdAt; }
    public Instant expiresAt() { return expiresAt; }
    public int clickLimit() { return clickLimit; }
    public int clicks() { return clicks; }
    public boolean isExpired(Instant now) { return !now.isBefore(expiresAt); }
    public boolean isLimitReached() { return clicks >= clickLimit; }
    public void registerClick(Instant now) {
        if (isExpired(now)) throw new LinkExpiredException();
        if (isLimitReached()) throw new LinkLimitReachedException();
        clicks++;
    }
    public void changeLimit(int newLimit) {
        if (newLimit < clicks || newLimit < 1) throw new IllegalArgumentException("Лимит не может быть меньше уже сделанных переходов");
        clickLimit = newLimit;
    }
}
