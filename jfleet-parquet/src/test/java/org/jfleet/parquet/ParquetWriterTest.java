/**
 * Copyright 2022 Jerónimo López Bezanilla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jfleet.parquet;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.InputFile;
import org.jfleet.EntityFieldType.FieldTypeEnum;
import org.jfleet.EntityInfo;
import org.jfleet.EntityInfoBuilder;
import org.jfleet.avro.UnsupportedTypeException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;

import static org.jfleet.parquet.TestEntityWithEnum.WeekDays.FRIDAY;
import static org.jfleet.parquet.TestEntityWithEnum.WeekDays.SATURDAY;
import static org.junit.jupiter.api.Assertions.*;

class ParquetWriterTest {

    @Test
    public void shouldWrite() throws IOException {
        EntityInfo entityInfo = new EntityInfoBuilder<>(TestEntity.class)
                .addColumn("fooInt", FieldTypeEnum.INT, TestEntity::getFooInt)
                .addColumn("fooShort", FieldTypeEnum.SHORT, TestEntity::getFooShort)
                .addColumn("fooByte", FieldTypeEnum.BYTE, TestEntity::getFooByte)
                .addColumn("fooDouble", FieldTypeEnum.DOUBLE, TestEntity::getFooDouble)
                .addColumn("fooLong", FieldTypeEnum.LONG, TestEntity::getFooLong)
                .addColumn("fooFloat", FieldTypeEnum.FLOAT, TestEntity::getFooFloat)
                .build();

        TestEntity testEntity = new TestEntity();
        testEntity.setFooInt(1);
        testEntity.setFooShort((short) 2);
        testEntity.setFooByte((byte) 3);
        testEntity.setFooDouble(10.2);
        testEntity.setFooLong(100L);
        testEntity.setFooFloat(50.1F);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ParquetConfiguration<TestEntity> parquetConfiguration = new ParquetConfiguration.Builder<TestEntity>(outputStream, entityInfo).build();
        ParquetWriter<TestEntity> parquetWriter = new ParquetWriter<>(parquetConfiguration);
        parquetWriter.writeAll(Arrays.asList(testEntity));

    }

    @Test
    void shouldFillOutputStream() throws IOException {
        EntityInfo entityInfo = new EntityInfoBuilder<>(TestEntity.class)
                .addColumn("foo", FieldTypeEnum.STRING, TestEntity::getFooString)
                .build();
        TestEntity testEntity = new TestEntity();
        testEntity.setFooString("foo");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ParquetConfiguration<TestEntity> parquetConfiguration = new ParquetConfiguration.Builder<TestEntity>(outputStream, entityInfo).build();
        ParquetWriter<TestEntity> parquetWriter = new ParquetWriter<>(parquetConfiguration);
        parquetWriter.writeAll(Arrays.asList(testEntity));

        assertTrue(outputStream.size() > 0);
    }

    @Test
    void shouldConvertEntityInfoWithStringTypesToParquet() throws IOException {
        EntityInfo entityInfo = new EntityInfoBuilder<>(TestEntity.class)
                .addColumn("foo", FieldTypeEnum.STRING, TestEntity::getFooString)
                .build();
        TestEntity testEntity = new TestEntity();
        testEntity.setFooString("foo");

        try (ParquetReader<GenericRecord> parquetReader = serializeAndRead(entityInfo, testEntity)) {
            GenericRecord genericRecord = parquetReader.read();
            assertNotNull(genericRecord);
            assertEquals(new Utf8("foo"), genericRecord.get("foo"));
        }
    }

    @Test
    void shouldConvertEntityInfoWithNullStringType() throws IOException {
        EntityInfo entityInfo = new EntityInfoBuilder<>(TestEntity.class)
                .addColumn("foo", FieldTypeEnum.STRING, TestEntity::getFooString)
                .build();

        TestEntity testEntity = new TestEntity();

        try (ParquetReader<GenericRecord> parquetReader = serializeAndRead(entityInfo, testEntity)) {
            GenericRecord genericRecord = parquetReader.read();
            assertNotNull(genericRecord);
            assertNull(genericRecord.get("foo"));
        }
    }

    @Test
    void shouldConvertEntityInfoWithBooleanType() throws IOException {
        EntityInfo entityInfo = new EntityInfoBuilder<>(TestEntity.class)
                .addColumn("fooBoolean", FieldTypeEnum.BOOLEAN, TestEntity::getFooBoolean)
                .build();

        TestEntity testEntity = new TestEntity();
        testEntity.setFooBoolean(true);

        try (ParquetReader<GenericRecord> parquetReader = serializeAndRead(entityInfo, testEntity)) {
            GenericRecord genericRecord = parquetReader.read();
            assertNotNull(genericRecord);
            assertTrue((Boolean) genericRecord.get("fooBoolean"));
        }
    }

    @Test
    void shouldConvertEntityWithPrimitives() throws IOException {
        EntityInfo entityInfo = new EntityInfoBuilder<>(TestEntityWithPrimitives.class)
                .addColumn("fooBoolean", FieldTypeEnum.BOOLEAN, true, TestEntityWithPrimitives::isFooBoolean)
                .addColumn("fooInt", FieldTypeEnum.INT, true, TestEntityWithPrimitives::getFooInt)
                .addColumn("fooShort", FieldTypeEnum.SHORT, true, TestEntityWithPrimitives::getFooShort)
                .addColumn("fooByte", FieldTypeEnum.BYTE, true, TestEntityWithPrimitives::getFooByte)
                .addColumn("fooDouble", FieldTypeEnum.DOUBLE, true, TestEntityWithPrimitives::getFooDouble)
                .addColumn("fooLong", FieldTypeEnum.LONG, true, TestEntityWithPrimitives::getFooLong)
                .addColumn("fooFloat", FieldTypeEnum.FLOAT, true, TestEntityWithPrimitives::getFooFloat)
                .build();

        TestEntityWithPrimitives testEntity = new TestEntityWithPrimitives();
        testEntity.setFooBoolean(true);
        testEntity.setFooInt(1);
        testEntity.setFooShort((short) 2);
        testEntity.setFooByte((byte) 3);
        testEntity.setFooDouble(10.2);
        testEntity.setFooLong(100L);
        testEntity.setFooFloat(50.1F);

        try (ParquetReader<GenericRecord> parquetReader = serializeAndRead(entityInfo, testEntity)) {
            GenericRecord genericRecord = parquetReader.read();
            assertNotNull(genericRecord);
            assertTrue((Boolean) genericRecord.get("fooBoolean"));
            assertEquals(1, genericRecord.get("fooInt"));
            assertEquals(2, genericRecord.get("fooShort"));
            assertEquals(3, genericRecord.get("fooByte"));
            assertEquals(10.2, genericRecord.get("fooDouble"));
            assertEquals(100L, genericRecord.get("fooLong"));
            assertEquals(50.1F, genericRecord.get("fooFloat"));
        }
    }

    @Test
    void shouldCreateSchemaWithoutNullTypesForPrimitives() throws IOException {
        EntityInfo entityInfo = new EntityInfoBuilder<>(TestEntityWithPrimitives.class)
                .addColumn("fooBoolean", FieldTypeEnum.BOOLEAN, true, TestEntityWithPrimitives::isFooBoolean)
                .addColumn("fooInt", FieldTypeEnum.INT, true, TestEntityWithPrimitives::getFooInt)
                .addColumn("fooShort", FieldTypeEnum.SHORT, true, TestEntityWithPrimitives::getFooShort)
                .addColumn("fooByte", FieldTypeEnum.BYTE, true, TestEntityWithPrimitives::getFooByte)
                .addColumn("fooDouble", FieldTypeEnum.DOUBLE, true, TestEntityWithPrimitives::getFooDouble)
                .addColumn("fooLong", FieldTypeEnum.LONG, true, TestEntityWithPrimitives::getFooLong)
                .addColumn("fooFloat", FieldTypeEnum.FLOAT, true, TestEntityWithPrimitives::getFooFloat)
                .build();

        TestEntityWithPrimitives testEntity = new TestEntityWithPrimitives();
        testEntity.setFooBoolean(true);
        testEntity.setFooInt(1);
        testEntity.setFooShort((short) 2);
        testEntity.setFooByte((byte) 3);
        testEntity.setFooDouble(10.2);
        testEntity.setFooLong(100L);
        testEntity.setFooFloat(50.1F);

        try (ParquetReader<GenericRecord> parquetReader = serializeAndRead(entityInfo, testEntity)) {
            Schema schema = parquetReader.read().getSchema();
            assertFieldIsNotUnionType(schema, "fooBoolean");
            assertFieldIsNotUnionType(schema, "fooInt");
            assertFieldIsNotUnionType(schema, "fooShort");
            assertFieldIsNotUnionType(schema, "fooByte");
            assertFieldIsNotUnionType(schema, "fooDouble");
            assertFieldIsNotUnionType(schema, "fooLong");
            assertFieldIsNotUnionType(schema, "fooFloat");
        }
    }

    private void assertFieldIsNotUnionType(Schema schema, String fieldName) {
        assertNotEquals(Schema.Type.UNION, schema.getField(fieldName).schema().getType());
    }

    @Test
    void shouldCreateSchemaWithNullTypesForObjects() throws IOException {
        EntityInfo entityInfo = new EntityInfoBuilder<>(TestEntity.class)
                .addColumn("fooBoolean", FieldTypeEnum.BOOLEAN, TestEntity::getFooBoolean)
                .addColumn("fooInt", FieldTypeEnum.INT, TestEntity::getFooInt)
                .addColumn("fooShort", FieldTypeEnum.SHORT, TestEntity::getFooShort)
                .addColumn("fooByte", FieldTypeEnum.BYTE, TestEntity::getFooByte)
                .addColumn("fooDouble", FieldTypeEnum.DOUBLE, TestEntity::getFooDouble)
                .addColumn("fooLong", FieldTypeEnum.LONG, TestEntity::getFooLong)
                .addColumn("fooFloat", FieldTypeEnum.FLOAT, TestEntity::getFooFloat)
                .build();

        TestEntity testEntity = new TestEntity();
        testEntity.setFooBoolean(true);
        testEntity.setFooInt(1);
        testEntity.setFooShort((short) 2);
        testEntity.setFooByte((byte) 3);
        testEntity.setFooDouble(10.2);
        testEntity.setFooLong(100L);
        testEntity.setFooFloat(50.1F);

        try (ParquetReader<GenericRecord> parquetReader = serializeAndRead(entityInfo, testEntity)) {
            Schema schema = parquetReader.read().getSchema();
            assertUnionFieldContainsNullType(schema, "fooBoolean");
            assertUnionFieldContainsNullType(schema, "fooInt");
            assertUnionFieldContainsNullType(schema, "fooShort");
            assertUnionFieldContainsNullType(schema, "fooByte");
            assertUnionFieldContainsNullType(schema, "fooDouble");
            assertUnionFieldContainsNullType(schema, "fooLong");
            assertUnionFieldContainsNullType(schema, "fooFloat");
        }
    }

    private void assertUnionFieldContainsNullType(Schema schema, String fieldName) {
        Schema fieldSchema = schema.getField(fieldName).schema();
        assertEquals(Schema.Type.UNION, fieldSchema.getType());
        assertTrue(fieldSchema.getTypes().contains(Schema.create(Schema.Type.NULL)));
    }

    @Test
    void shouldConvertEntityInfoWithNullBoolean() throws IOException {
        EntityInfo entityInfo = new EntityInfoBuilder<>(TestEntity.class)
                .addColumn("fooBoolean", FieldTypeEnum.BOOLEAN, TestEntity::getFooBoolean)
                .build();

        TestEntity testEntity = new TestEntity();

        try (ParquetReader<GenericRecord> parquetReader = serializeAndRead(entityInfo, testEntity)) {
            GenericRecord genericRecord = parquetReader.read();
            assertNotNull(genericRecord);
            assertNull(genericRecord.get("fooBoolean"));
        }
    }

    @Test
    void shouldConvertEntityInfoWithNumericTypesToParquet() throws IOException {
        EntityInfo entityInfo = new EntityInfoBuilder<>(TestEntity.class)
                .addColumn("fooInt", FieldTypeEnum.INT, TestEntity::getFooInt)
                .addColumn("fooShort", FieldTypeEnum.SHORT, TestEntity::getFooShort)
                .addColumn("fooByte", FieldTypeEnum.BYTE, TestEntity::getFooByte)
                .addColumn("fooDouble", FieldTypeEnum.DOUBLE, TestEntity::getFooDouble)
                .addColumn("fooLong", FieldTypeEnum.LONG, TestEntity::getFooLong)
                .addColumn("fooFloat", FieldTypeEnum.FLOAT, TestEntity::getFooFloat)
                .build();

        TestEntity testEntity = new TestEntity();
        testEntity.setFooInt(1);
        testEntity.setFooShort((short) 2);
        testEntity.setFooByte((byte) 3);
        testEntity.setFooDouble(10.2);
        testEntity.setFooLong(100L);
        testEntity.setFooFloat(50.1F);

        try (ParquetReader<GenericRecord> parquetReader = serializeAndRead(entityInfo, testEntity)) {
            GenericRecord genericRecord = parquetReader.read();
            assertNotNull(genericRecord);
            assertEquals(1, genericRecord.get("fooInt"));
            assertEquals(2, genericRecord.get("fooShort"));
            assertEquals(3, genericRecord.get("fooByte"));
            assertEquals(10.2, genericRecord.get("fooDouble"));
            assertEquals(100L, genericRecord.get("fooLong"));
            assertEquals(50.1F, genericRecord.get("fooFloat"));
        }
    }

    @Test
    void shouldConvertEntityInfoWithNullNumericTypesToParquet() throws IOException {
        EntityInfo entityInfo = new EntityInfoBuilder<>(TestEntity.class)
                .addColumn("fooInt", FieldTypeEnum.INT, TestEntity::getFooInt)
                .addColumn("fooShort", FieldTypeEnum.SHORT, TestEntity::getFooShort)
                .addColumn("fooByte", FieldTypeEnum.BYTE, TestEntity::getFooByte)
                .addColumn("fooDouble", FieldTypeEnum.DOUBLE, TestEntity::getFooDouble)
                .addColumn("fooLong", FieldTypeEnum.LONG, TestEntity::getFooLong)
                .addColumn("fooFloat", FieldTypeEnum.FLOAT, TestEntity::getFooFloat)
                .build();

        TestEntity testEntity = new TestEntity();

        try (ParquetReader<GenericRecord> parquetReader = serializeAndRead(entityInfo, testEntity)) {

            GenericRecord genericRecord = parquetReader.read();
            assertNotNull(genericRecord);
            assertNull(genericRecord.get("fooInt"));
            assertNull(genericRecord.get("fooShort"));
            assertNull(genericRecord.get("fooByte"));
            assertNull(genericRecord.get("fooDouble"));
            assertNull(genericRecord.get("fooLong"));
            assertNull(genericRecord.get("fooFloat"));
        }
    }

    @Test
    void shouldThrowUnsupportedTypeException() {
        EntityInfo entityInfo = new EntityInfoBuilder<>(TestEntity.class)
                .addColumn("foo", FieldTypeEnum.BIGDECIMAL, a -> BigDecimal.ZERO)
                .build();

        ParquetConfiguration<TestEntity> parquetConfiguration = new ParquetConfiguration.Builder<TestEntity>(new ByteArrayOutputStream(), entityInfo).build();
        assertThrows(UnsupportedTypeException.class, () -> new ParquetWriter<>(parquetConfiguration));
    }

    @Test
    void shouldConvertEnumTypesToParquet() throws IOException {
        EntityInfo entityInfo = new EntityInfoBuilder<>(TestEntityWithEnum.class)
                .addColumn("foo", FieldTypeEnum.ENUMORDINAL, TestEntityWithEnum::getFoo)
                .addColumn("bar", FieldTypeEnum.ENUMSTRING, TestEntityWithEnum::getBar)
                .build();

        TestEntityWithEnum testEntityWithEnum = new TestEntityWithEnum();
        testEntityWithEnum.setFoo(FRIDAY);
        testEntityWithEnum.setBar(SATURDAY);

        try (ParquetReader<GenericRecord> parquetReader = serializeAndRead(entityInfo, testEntityWithEnum)) {

            GenericRecord genericRecord = parquetReader.read();
            assertNotNull(genericRecord);
            assertEquals(4, genericRecord.get("foo"));
            assertEquals(new Utf8("SATURDAY"), genericRecord.get("bar"));
        }
    }

    @Test
    void shouldConvertNullableEnumTypesToParquet() throws IOException {
        EntityInfo entityInfo = new EntityInfoBuilder<>(TestEntityWithEnum.class)
                .addColumn("foo", FieldTypeEnum.ENUMORDINAL, TestEntityWithEnum::getFoo)
                .addColumn("bar", FieldTypeEnum.ENUMSTRING, TestEntityWithEnum::getBar)
                .build();

        TestEntityWithEnum testEntityWithEnum = new TestEntityWithEnum();

        try (ParquetReader<GenericRecord> parquetReader = serializeAndRead(entityInfo, testEntityWithEnum)) {

            GenericRecord genericRecord = parquetReader.read();
            assertNotNull(genericRecord);
            assertNull(genericRecord.get("foo"));
            assertNull(genericRecord.get("bar"));
        }
    }

    private <T> ParquetReader<GenericRecord> serializeAndRead(EntityInfo entityInfo, T testEntity) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream("/tmp/foo.parquet")) {
            ParquetConfiguration<TestEntity> parquetConfiguration = new ParquetConfiguration.Builder<TestEntity>(outputStream, entityInfo)
                    .withCompressionCodec(CompressionCodecName.SNAPPY)
                    .withRowGroupSize(org.apache.parquet.hadoop.ParquetWriter.DEFAULT_BLOCK_SIZE)
                    .withPageSize(org.apache.parquet.hadoop.ParquetWriter.DEFAULT_PAGE_SIZE)
                    .withValidation(true)
                    .withDictionaryEncoding(true)
                    .build();
            ParquetWriter<T> parquetWriter = new ParquetWriter<>(parquetConfiguration);
            parquetWriter.writeAll(Arrays.asList(testEntity));
        }
        InputFile file = HadoopInputFile.fromPath(new Path("/tmp/foo.parquet"), new Configuration());
        return AvroParquetReader.<GenericRecord>builder(file)
                .withDataModel(GenericData.get())
                .build();
    }

}
