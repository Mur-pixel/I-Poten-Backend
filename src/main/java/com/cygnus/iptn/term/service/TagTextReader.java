package com.cygnus.iptn.term.service;

import com.cygnus.iptn.term.support.TagTextColumnResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TagTextReader {

    private final JdbcTemplate jdbc;
    private final TagTextColumnResolver resolver;

    public Optional<String> readRaw(Long termId) {
        return resolver.resolve().flatMap(loc -> {
            String sql = "SELECT `" + loc.getTagColumn() + "` FROM `" + loc.getTable()
                    + "` WHERE `" + loc.getKeyColumn() + "` = ?";
            try {
                String s = jdbc.queryForObject(sql, String.class, termId); // 바인딩
                return Optional.ofNullable(s);
            } catch (EmptyResultDataAccessException e) {
                return Optional.empty();
            }
        });
    }
}
