package com.example.hms.util;

import com.example.hms.exception.InvalidRequestException;

public final class PageNumbers {
    private PageNumbers() { }
    public static int validate(int page, int size) {
        if (page < 0 || (long) page * size > Integer.MAX_VALUE) {
            throw new InvalidRequestException("Enter a valid page number or return to the first page of the directory.");
        }
        return page;
    }
}
