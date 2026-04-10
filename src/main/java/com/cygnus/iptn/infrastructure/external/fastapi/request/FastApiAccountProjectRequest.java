package com.cygnus.iptn.infrastructure.external.fastapi.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FastApiAccountProjectRequest {

    private String projectName;
    private String projectDescription;
}
