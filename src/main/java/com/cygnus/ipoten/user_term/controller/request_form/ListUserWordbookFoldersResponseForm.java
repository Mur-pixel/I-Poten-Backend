package com.cygnus.ipoten.user_term.controller.request_form;

import com.cygnus.ipoten.user_term.entity.UserWordbookFolder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListUserWordbookFoldersResponseForm {
    private List<Item> items;
    private int total;
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Item { private Long id; private String folderName; private Integer sortOrder; }

    public static ListUserWordbookFoldersResponseForm from(List<UserWordbookFolder> list) {
        var items = list.stream()
                .map(f -> new Item(f.getId(), f.getFolderName(), f.getSortOrder()))
                .toList();
        return new ListUserWordbookFoldersResponseForm(items, items.size());
    }
}