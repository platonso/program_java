package ru.platonso.linkshort.repository;

import ru.platonso.linkshort.domain.ShortLink;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryLinkRepository implements LinkRepository {
    private final ConcurrentHashMap<String, ShortLink> links = new ConcurrentHashMap<>();
    public void save(ShortLink link) { links.put(link.code(), link); }
    public Optional<ShortLink> findByCode(String code) { return Optional.ofNullable(links.get(code)); }
    public boolean delete(String code) { return links.remove(code) != null; }
    public Collection<ShortLink> findByOwnerId(UUID ownerId) {
        return links.values().stream().filter(link -> link.ownerId().equals(ownerId)).toList();
    }
    public int deleteExpired(Instant now) {
        List<String> expired = links.values().stream().filter(link -> link.isExpired(now)).map(ShortLink::code).toList();
        expired.forEach(links::remove);
        return expired.size();
    }
}
