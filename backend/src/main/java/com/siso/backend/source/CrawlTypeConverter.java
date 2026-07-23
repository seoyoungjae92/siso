package com.siso.backend.source;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CrawlTypeConverter implements AttributeConverter<CrawlType, String> {

    @Override
    public String convertToDatabaseColumn(CrawlType attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public CrawlType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CrawlType.fromValue(dbData);
    }
}
