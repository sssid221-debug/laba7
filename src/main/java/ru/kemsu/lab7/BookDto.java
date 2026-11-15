package ru.kemsu.lab7;

/** Книга так, как она описана в books.json. */
public record BookDto(String name, String author, int publishingYear, String isbn, String publisher) {
}
