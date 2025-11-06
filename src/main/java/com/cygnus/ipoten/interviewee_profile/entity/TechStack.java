package com.cygnus.ipoten.interviewee_profile.entity;

import lombok.Getter;

@Getter
public enum TechStack {
    FULLSTACK("풀스택"),
    BACKEND("백엔드/서버개발"),
    FRONTEND("프론트엔드"),
    WEB("웹개발"),
    FLUTTER("Flutter"),
    JAVA("Java"),
    JAVASCRIPT("JavaScript"),
    PYTHON("Python"),
    VUEJS("Vue.js"),
    API("API"),
    MYSQL("MYSQL"),
    AWS("AWS"),
    REACTJS("ReactJS"),
    ASP("ASP"),
    ANGULAR("Angular"),
    BOOTSTRAP("Bootstrap"),
    NODEJS("Node.js"),
    JQUERY("jQuery"),
    PHP("PHP"),
    JSP("JSP"),
    GRAPHQL("GraphQL"),
    HTML5("HTML5");

    private final String displayName;

    TechStack(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
