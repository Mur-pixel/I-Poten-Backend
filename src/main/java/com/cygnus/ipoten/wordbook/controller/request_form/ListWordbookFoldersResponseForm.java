package com.cygnus.ipoten.wordbook.controller.request_form;

import com.cygnus.ipoten.wordbook.entity.WordbookFolder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListWordbookFoldersResponseForm {
    private List<Item> items;
    private int total;
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Item { private Long id; private String folderName; private Integer sortOrder; }

    public static ListWordbookFoldersResponseForm from(List<WordbookFolder> list) {
        var items = list.stream()
                .map(f -> new Item(f.getId(), f.getFolderName(), f.getSortOrder()))
                .toList();
        return new ListWordbookFoldersResponseForm(items, items.size());
    }
}