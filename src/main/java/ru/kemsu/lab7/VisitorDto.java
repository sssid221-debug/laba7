package ru.kemsu.lab7;

import java.util.List;

/** Посетитель и его любимые книги так, как они описаны в books.json. */
public record VisitorDto(String name, String surname, String phone, boolean subscribed,
                         List<BookDto> favoriteBooks) {
}
