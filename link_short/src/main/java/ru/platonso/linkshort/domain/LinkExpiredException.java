package ru.platonso.linkshort.domain;
public final class LinkExpiredException extends RuntimeException { public LinkExpiredException() { super("Срок действия ссылки истёк"); } }
