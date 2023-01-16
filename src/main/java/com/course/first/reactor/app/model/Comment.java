package com.course.first.reactor.app.model;

import java.util.ArrayList;
import java.util.List;

public class Comment {

    private List<String> comments;

    public Comment(){
        this.comments = new ArrayList<>();
    }

    public void addComment(String comment) {
        this.comments.add(comment);
    }

    @Override
    public String toString() {
        return "Comment{" +
                "comments=" + comments +
                '}';
    }
}
