/*
 * 3D City Database - The Open Source CityGML Database
 * https://www.3dcitydb.org/
 *
 * Copyright 2013 - 2021
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.lrg.tum.de/gis/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * Virtual City Systems, Berlin <https://vc.systems/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Taufkirchen <http://www.moss.de/>
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
package org.citydb.plugins.ade_manager.registry.schema.adapter.postgis;

import org.citydb.config.project.database.DatabaseType;
import org.citydb.core.database.schema.mapping.SchemaMapping;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.schema.adapter.AbstractADEDBSchemaManager;
import org.citydb.plugins.ade_manager.util.PathResolver;
import org.citydb.util.log.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;

public class PostgisADEDBSchemaManager extends AbstractADEDBSchemaManager {
    private final Logger log = Logger.getInstance();

    public PostgisADEDBSchemaManager(Connection connection, ConfigImpl config) {
        super(connection, config);
    }

    public void createADEDatabaseSchema(SchemaMapping schemaMapping) throws SQLException {

        super.createADEDatabaseSchema(schemaMapping);

        // update SRID for geometry columns of cityGML core and ADE tables
        String schema = dbPool.getActiveDatabaseAdapter().getConnectionDetails().getSchema();
        int srid = dbPool.getActiveDatabaseAdapter().getUtil().getDatabaseInfo(schema).getReferenceSystem().getSrid();
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "select f_table_schema, f_table_name, f_geometry_column from geometry_columns where f_table_schema=? " +
                        "AND f_table_name LIKE '" + schemaMapping.getMetadata().getDBPrefix() + "\\_%' " +
                        "AND f_geometry_column <> 'implicit_geometry' " +
                        "AND f_geometry_column <> 'relative_other_geom'" +
                        "AND f_geometry_column <> 'texture_coordinates'")) {
            preparedStatement.setString(1, schema);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                try (CallableStatement callableStatement = connection.prepareCall("{call UpdateGeometrySRID(?, ?, ?, ?)}")) {
                    callableStatement.setString(1, rs.getString((1)));
                    callableStatement.setString(2, rs.getString((2)));
                    callableStatement.setString(3, rs.getString((3)));
                    callableStatement.setInt(4, srid);
                    callableStatement.execute();
                }
            }
        }
    }

    @Override
    protected String readCreateADEDBScript() throws IOException {
        String adeRegistryInputpath = config.getAdeRegistryInputPath();
        String createDBscriptPath = PathResolver.get_create_ade_db_filepath(adeRegistryInputpath, DatabaseType.POSTGIS);

        return new String(Files.readAllBytes(Paths.get(createDBscriptPath)));
    }

    @Override
    protected String processScript(String inputScript) {
        return inputScript;
    }

    @Override
    protected void dropCurrentFunctions() throws SQLException {
        String schema = dbPool.getActiveDatabaseAdapter().getConnectionDetails().getSchema();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("select proname, pg_catalog.pg_get_function_identity_arguments(oid) " +
                     "from pg_catalog.pg_proc " +
                     "where pronamespace = '" + schema + "'::regnamespace " +
                     "and (proname like 'del_%' or proname like 'env_%')")) {
            while (rs.next()) {
                String name = rs.getString(1);
                String args = rs.getString(2);
                try (PreparedStatement pstsmt = connection.prepareStatement(
                        "drop function if exists " + schema + "." + name + "(" + args + ")")) {
                    pstsmt.executeUpdate();
                }

                LOG.debug("DB-function '" + name + "' successfully dropped");
            }
        }
    }
}
