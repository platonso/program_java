package ru.platonso.linkshort;

import ru.platonso.linkshort.cli.ConsoleApplication;
import ru.platonso.linkshort.config.AppConfig;
import ru.platonso.linkshort.repository.InMemoryLinkRepository;
import ru.platonso.linkshort.service.LinkService;
import java.time.Clock;

public final class LinkShortApplication {
    private LinkShortApplication() { }
    public static void main(String[] args) {
        AppConfig config = AppConfig.load();
        LinkService service = new LinkService(new InMemoryLinkRepository(), config, Clock.systemUTC());
        new ConsoleApplication(service, config).run();
    }
}
