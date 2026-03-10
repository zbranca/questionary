package com.questionary.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answerText;

    // null = unanswered, "SUCCESS", "FAILED"
    @Column
    private String status;

    @Column(nullable = false)
    private int sortOrder;

    public Question() {}

    public Question(String questionText, String answerText, int sortOrder) {
        this.questionText = questionText;
        this.answerText = answerText;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isUnanswered() { return status == null; }
    public boolean isSuccess()    { return "SUCCESS".equals(status); }
    public boolean isFailed()     { return "FAILED".equals(status); }
}
