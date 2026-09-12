package com.example.hms.repository;

import com.example.hms.domain.Person;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** Reusable, literal substring search over person directories. */
public final class DirectorySearch {
    private DirectorySearch() { }

    public static <T extends Person> Specification<T> matching(String keyword, String codeField) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String escaped = keyword.strip().toLowerCase(Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_");
            String pattern = "%" + escaped + "%";
            return cb.or(
                    cb.like(cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))), pattern, '!'),
                    cb.like(cb.lower(root.get("phone")), pattern, '!'),
                    cb.like(cb.lower(root.get("email")), pattern, '!'),
                    cb.like(cb.lower(root.get(codeField)), pattern, '!'));
        };
    }
}
