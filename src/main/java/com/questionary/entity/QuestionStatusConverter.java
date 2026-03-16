package com.questionary.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class QuestionStatusConverter implements AttributeConverter<QuestionStatus, String> {

    @Override
    public String convertToDatabaseColumn(QuestionStatus status) {
        return (status == null ? QuestionStatus.UNANSWERED : status).name();
    }

    @Override
    public QuestionStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return QuestionStatus.UNANSWERED; // backward-compat for legacy null rows
        try {
            return QuestionStatus.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            return QuestionStatus.UNANSWERED;
        }
    }
}
