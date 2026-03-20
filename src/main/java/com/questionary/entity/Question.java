package com.questionary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

    @Column
    @Convert(converter = QuestionStatusConverter.class)
    private QuestionStatus status = QuestionStatus.UNANSWERED;

    @Column(nullable = false)
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private AppUser user;

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

    public QuestionStatus getStatus() { return status; }
    public void setStatus(QuestionStatus status) { this.status = status; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
}
