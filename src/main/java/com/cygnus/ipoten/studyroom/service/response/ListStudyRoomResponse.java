package com.cygnus.ipoten.studyroom.service.response;

import com.cygnus.ipoten.studyroom.entity.StudyRoom;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class ListStudyRoomResponse {
    private final List<Map<String, Object>> studyRoomList;
    private final boolean hasNext;

    public ListStudyRoomResponse(Slice<StudyRoom> studyRoomSlice) {
        this.studyRoomList = studyRoomSlice.getContent().stream()
                .map(this::convertStudyRoomToMap)
                .collect(Collectors.toList());
        this.hasNext = studyRoomSlice.hasNext();
    }

    private Map<String, Object> convertStudyRoomToMap(StudyRoom room){
        Map<String, Object> roomMap = new HashMap<>();
        roomMap.put("id", room.getId());
        roomMap.put("title", room.getTitle());
        roomMap.put("status", room.getStatus().name());
        roomMap.put("location", room.getLocation().getKoreanName());
        roomMap.put("studyLevel", room.getStudyLevel().name());
        roomMap.put("recruitingRoles", room.getRecruitingRoles());
        roomMap.put("skillStack", room.getSkillStack());
        roomMap.put("maxMembers", room.getMaxMembers());
        roomMap.put("currentMembers", room.getStudyMembers().size());

        roomMap.put("hostNickname", room.getHost().getNickname());
        roomMap.put("createdAt", room.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME));

        return roomMap;
    }

}