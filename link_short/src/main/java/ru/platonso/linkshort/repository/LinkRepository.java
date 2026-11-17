package ru.platonso.linkshort.repository;

import ru.platonso.linkshort.domain.ShortLink;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface LinkRepository {
    void save(ShortLink link);
    Optional<ShortLink> findByCode(String code);
    boolean delete(String code);
    Collection<ShortLink> findByOwnerId(java.util.UUID ownerId);
    int deleteExpired(Instant now);
}
