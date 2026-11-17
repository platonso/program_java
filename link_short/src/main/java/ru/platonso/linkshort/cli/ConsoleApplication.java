package ru.platonso.linkshort.cli;

import ru.platonso.linkshort.config.AppConfig;
import ru.platonso.linkshort.domain.ShortLink;
import ru.platonso.linkshort.service.LinkResult;
import ru.platonso.linkshort.service.LinkService;
import java.awt.Desktop;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Text UI. A UUID is printed once and must be kept to manage an owner's links later. */
public final class ConsoleApplication {
    private final LinkService service;
    private final AppConfig config;
    private UUID currentUser;
    public ConsoleApplication(LinkService service, AppConfig config) { this.service = service; this.config = config; }
    public void run() {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
        try (Scanner scanner = new Scanner(System.in)) {
            cleaner.scheduleAtFixedRate(service::cleanupExpired, config.cleanupPeriod().toSeconds(), config.cleanupPeriod().toSeconds(), TimeUnit.SECONDS);
            System.out.println("Сокращатель ссылок. Напишите help для списка команд.");
            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) return;
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) continue;
                try {
                    if (!execute(input)) return;
                } catch (RuntimeException exception) { System.out.println("Ошибка: " + exception.getMessage()); }
            }
        } finally { cleaner.shutdownNow(); }
    }
    private boolean execute(String input) {
        String[] parts = input.split("\\s+", 3);
        switch (parts[0].toLowerCase()) {
            case "help" -> help();
            case "new-user" -> newUser();
            case "use" -> use(parts);
            case "create" -> create(parts);
            case "open" -> open(parts);
            case "list" -> list();
            case "limit" -> limit(parts);
            case "delete" -> delete(parts);
            case "exit", "quit" -> { return false; }
            default -> System.out.println("Неизвестная команда. Напишите help.");
        }
        return true;
    }
    private void help() { System.out.println("""
            Команды:
              new-user                          создать нового пользователя (UUID)
              use <uuid>                        переключиться на существующего пользователя
              create <URL> [лимит]              создать короткую ссылку
              open <код или clck.ru/код>        засчитать переход и открыть URL в браузере
              list                              показать свои ссылки
              limit <код> <новый лимит>         изменить лимит своей ссылки
              delete <код>                      удалить свою ссылку
              exit                              завершить программу
            """); }
    private void newUser() { currentUser = UUID.randomUUID(); System.out.println("Ваш UUID: " + currentUser + ". Сохраните его для управления ссылками."); }
    private void use(String[] parts) { require(parts, 2, "use <uuid>"); currentUser = UUID.fromString(parts[1]); System.out.println("Текущий пользователь: " + currentUser); }
    private void create(String[] parts) {
        require(parts, 2, "create <URL> [лимит]");
        ensureUserForCreation();
        String[] urlAndLimit = parts.length == 3 ? (parts[1] + " " + parts[2]).split("\\s+") : new String[]{parts[1]};
        Integer limit = urlAndLimit.length > 1 ? Integer.valueOf(urlAndLimit[1]) : null;
        ShortLink link = service.create(currentUser, urlAndLimit[0], limit);
        System.out.println("Готово: " + service.toShortUrl(link) + ", лимит: " + link.clickLimit() + ", действует до " + link.expiresAt());
    }
    private void open(String[] parts) {
        require(parts, 2, "open <код>"); LinkResult result = service.open(parts[1]);
        System.out.println("Переход " + result.clicks() + "/" + result.clickLimit() + ": " + result.target());
        try { if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) Desktop.getDesktop().browse(result.target()); else System.out.println("Браузер недоступен: откройте URL вручную."); }
        catch (Exception exception) { System.out.println("Не удалось открыть браузер: " + exception.getMessage()); }
    }
    private void list() { requireUser(); service.list(currentUser).forEach(link -> System.out.printf("%s -> %s | %d/%d | до %s%n", service.toShortUrl(link), link.target(), link.clicks(), link.clickLimit(), link.expiresAt())); }
    private void limit(String[] parts) { requireUser(); require(parts, 3, "limit <код> <новый лимит>"); service.changeLimit(currentUser, parts[1], Integer.parseInt(parts[2])); System.out.println("Лимит изменён."); }
    private void delete(String[] parts) { requireUser(); require(parts, 2, "delete <код>"); service.delete(currentUser, parts[1]); System.out.println("Ссылка удалена."); }
    private void requireUser() { if (currentUser == null) throw new IllegalStateException("Сначала используйте new-user или use <uuid>"); }
    private void ensureUserForCreation() {
        if (currentUser == null) {
            currentUser = UUID.randomUUID();
            System.out.println("Создан новый пользователь. Ваш UUID: " + currentUser + ". Сохраните его для управления ссылками.");
        }
    }
    private static void require(String[] parts, int count, String usage) { if (parts.length < count) throw new IllegalArgumentException("Использование: " + usage); }
}
