/**
 * Copyright 2017 Jerónimo López Bezanilla
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
package org.jfleet.citibikenyc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.jfleet.BulkInsert;
import org.jfleet.JFleetException;
import org.jfleet.citibikenyc.entities.TripFlatEntity;
import org.jfleet.jdbc.JdbcBulkInsert;
import org.jfleet.util.MySqlTestConnectionProvider;

/*
 * This example works with the dataset provided by Citi Bike NYC about each trip with their bikes.
 * The dataset can be downloaded from: https://s3.amazonaws.com/tripdata/index.html
 *
 * Parse all files located in /tmp directory and stream its content inserting into
 * the database.
 *
 * This version persist all records using plain JDBC batch inserts.
 *
 */
public class CitiBikeNycJdbc {

    public static void main(String[] args) throws IOException, SQLException {
        MySqlTestConnectionProvider connectionSuplier = new MySqlTestConnectionProvider();
        try (Connection connection = connectionSuplier.get()){
            TableHelper.createTable(connection);
            CitiBikeReader<TripFlatEntity> reader = new CitiBikeReader<>("/tmp", str -> new FlatTripParser(str));
            BulkInsert<TripFlatEntity> bulkInsert = new JdbcBulkInsert<>(TripFlatEntity.class, 100, false);
            reader.forEachCsvInZip(trips -> {
                try {
                    bulkInsert.insertAll(connection, trips);
                } catch (JFleetException | SQLException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}