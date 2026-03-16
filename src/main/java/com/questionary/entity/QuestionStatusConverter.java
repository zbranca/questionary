package com.questionary.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class QuestionStatusConverter implements AttributeConverter<QuestionStatus, String> {

    @Override
    public String convertToDatabaseColumn(QuestionStatus status) {
        if (status == null || status == QuestionStatus.UNANSWERED) return null;
        return status.name();
    }

    @Override
    public QuestionStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return QuestionStatus.UNANSWERED;
        return QuestionStatus.valueOf(dbData);
    }
}
