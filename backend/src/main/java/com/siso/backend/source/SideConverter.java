package com.siso.backend.source;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SideConverter implements AttributeConverter<Side, String> {

    @Override
    public String convertToDatabaseColumn(Side attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public Side convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Side.fromValue(dbData);
    }
}
