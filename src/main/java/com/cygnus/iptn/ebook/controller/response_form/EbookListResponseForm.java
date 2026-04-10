package com.cygnus.iptn.ebook.controller.response_form;

import com.cygnus.iptn.ebook.entity.Ebook;

import java.time.Instant;
import java.util.List;

public record EbookListResponseForm(List<Item> items) {
    public record Item(Long id, String title, boolean isPublic, Instant createdAt) {
        public static Item from(Ebook e) {
            return new Item(e.getId(), e.getTitle(), e.isPublic(), e.getCreatedAt());
        }
    }

    public static EbookListResponseForm from(List<Ebook> list) {
        return new EbookListResponseForm(list.stream().map(Item::from).toList());
    }
}
