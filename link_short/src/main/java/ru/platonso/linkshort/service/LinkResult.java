package ru.platonso.linkshort.service;
import java.net.URI;
public record LinkResult(String shortUrl, URI target, int clicks, int clickLimit) { }
