package org.jfleet.avro;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.SchemaBuilder.FieldAssembler;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.jfleet.ColumnInfo;
import org.jfleet.EntityFieldType.FieldTypeEnum;
import org.jfleet.EntityInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

public class AvroWriter<T> {
    private final AvroConfiguration avroConfiguration;

    public AvroWriter(AvroConfiguration avroConfiguration) {
        this.avroConfiguration = avroConfiguration;
    }

    public void writeAll(OutputStream output, Collection<T> collection) throws IOException {
        EntityInfo entityInfo = avroConfiguration.getEntityInfo();

        Schema schema = buildSchema(avroConfiguration);

        DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema);
        try (DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter)) {
            dataFileWriter.create(schema, output);
            for (T entity : collection) {
                GenericRecord genericRecord = new GenericData.Record(schema);
                for (ColumnInfo columnInfo : entityInfo.getColumns()) {
                    Object value = columnInfo.getAccessor().apply(entity);
                    if (value != null) {
                        value = extractValue(columnInfo, value);
                        genericRecord.put(columnInfo.getColumnName(), value);
                    }
                }
                dataFileWriter.append(genericRecord);
            }
        }
    }

    private Object extractValue(ColumnInfo columnInfo, Object value) {
        FieldTypeEnum fieldType = columnInfo.getFieldType().getFieldType();
        if (fieldType == FieldTypeEnum.BYTE) {
            value = ((Byte) value).intValue();
        } else if (fieldType == FieldTypeEnum.SHORT) {
            value = ((Short) value).intValue();
        } else if (fieldType == FieldTypeEnum.ENUMSTRING) {
            value = ((Enum<?>) value).name();
        } else if (fieldType == FieldTypeEnum.ENUMORDINAL) {
            value = ((Enum<?>) value).ordinal();
        }
        return value;
    }

    private Schema buildSchema(AvroConfiguration avroConfiguration) {
        EntityInfo entityInfo = avroConfiguration.getEntityInfo();
        FieldAssembler<Schema> fields = SchemaBuilder.record(entityInfo.getEntityClass().getName())
                .namespace(entityInfo.getEntityClass().getPackage().getName()).fields();

        for (ColumnInfo columnInfo : entityInfo.getColumns()) {
            fields = getFieldSchema(columnInfo, fields);
        }

        return fields.endRecord();
    }

    private FieldAssembler<Schema> getFieldSchema(ColumnInfo columnInfo, FieldAssembler<Schema> fields) {
        SchemaBuilder.FieldTypeBuilder<Schema> type = fields.name(columnInfo.getColumnName()).type();
        boolean isPrimitive = columnInfo.getFieldType().isPrimitive();

        switch (columnInfo.getFieldType().getFieldType()) {
            case STRING:
            case ENUMSTRING:
                return buildAsString(type);
            case ENUMORDINAL:
                return buildAsInteger(type, false);
            case INT:
            case SHORT:
            case BYTE:
                return buildAsInteger(type, isPrimitive);
            case DOUBLE:
                return buildAsDouble(type, isPrimitive);
            case LONG:
                return buildAsLong(type, isPrimitive);
            case FLOAT:
                return buildAsFloat(type, isPrimitive);
            case BOOLEAN:
                return buildAsBoolean(type, isPrimitive);
            default:
                throw new UnsupportedTypeException(String.format("Unsupported type: %s", columnInfo.getFieldType().getFieldType()));
        }

    }

    private FieldAssembler<Schema> buildAsString(SchemaBuilder.FieldTypeBuilder<Schema> type) {
        return type.unionOf().stringType().and().nullType().endUnion().noDefault();
    }

    private FieldAssembler<Schema> buildAsBoolean(SchemaBuilder.FieldTypeBuilder<Schema> type, boolean isPrimitive) {
        if (isPrimitive) {
            return type.booleanType().noDefault();
        }
        return type.unionOf().booleanType().and().nullType().endUnion().noDefault();
    }

    private FieldAssembler<Schema> buildAsFloat(SchemaBuilder.FieldTypeBuilder<Schema> type, boolean isPrimitive) {
        if (isPrimitive) {
            return type.floatType().noDefault();
        }
        return type.unionOf().floatType().and().nullType().endUnion().noDefault();
    }

    private FieldAssembler<Schema> buildAsLong(SchemaBuilder.FieldTypeBuilder<Schema> type, boolean isPrimitive) {
        if (isPrimitive) {
            return type.longType().noDefault();
        }
        return type.unionOf().longType().and().nullType().endUnion().noDefault();
    }

    private FieldAssembler<Schema> buildAsInteger(SchemaBuilder.FieldTypeBuilder<Schema> type, boolean isPrimitive) {
        if (isPrimitive) {
            return type.intType().noDefault();
        }
        return type.unionOf().intType().and().nullType().endUnion().noDefault();
    }

    private FieldAssembler<Schema> buildAsDouble(SchemaBuilder.FieldTypeBuilder<Schema> type, boolean isPrimitive) {
        if (isPrimitive) {
            return type.doubleType().noDefault();
        }
        return type.unionOf().doubleType().and().nullType().endUnion().noDefault();
    }
}
