package ru.platonso.linkshort.domain;
public final class LinkLimitReachedException extends RuntimeException { public LinkLimitReachedException() { super("Лимит переходов исчерпан"); } }
