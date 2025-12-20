package com.cygnus.ipoten.wordbook.controller.request_form;

import com.cygnus.ipoten.wordbook.entity.Wordbook;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListWordbookResponseForm {
    private List<Item> items;
    private int total;
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Item { private Long id; private String wordbookName; private Integer sortOrder; }

    public static ListWordbookResponseForm from(List<Wordbook> list) {
        var items = list.stream()
                .map(f -> new Item(f.getId(), f.getWordbookName(), f.getSortOrder()))
                .toList();
        return new ListWordbookResponseForm(items, items.size());
    }
}