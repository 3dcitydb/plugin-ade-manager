/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 *
 * Copyright 2013 - 2019
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.gis.bgu.tum.de/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
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
package org.citydb.plugins.ade_manager.util;

public class GlobalConstants {
	public static final int MAX_ADE_NAME_LENGTH = 1000;
	public static final int MAX_ADE_DESCRIPTION_LENGTH = 4000;
	public static final int MAX_ADE_VERSION_LENGTH = 50;
	public static final int MAX_DB_PREFIX_LENGTH = 4;
	public static final int MIN_ADE_OBJECTCLASSID = 10000;
	
	public static final int SURFACE_GEOMETRY_OBJECTCLASSID = 106;
	public static final int IMPLICIT_GEOMETRY_OBJECTCLASSID = 59;
	
	public static final String INPUT_GRAPH_PATH = "/org/citydb/plugins/ade_manager/graph/Working_Graph.ggx";
	public static final String TMP_GRAPH_FOLDER_NAME = "graph";
	public static final String TMP_INPUT_GRAPH_FILE_NAME = "Input_Graph_Tmp.ggx";
	public static final String TMP_OUTPUT_GRAPH_FILE_NAME = "Output_Graph_Tmp.ggx";
	
	public static final int MAX_TABLE_NAME_LENGTH = 25;
	public static final int MAX_COLUMN_NAME_LENGTH = 28;
	public static final int MAX_INDEX_NAME_LENGTH = 26;
	public static final int MAX_CONSTRAINT_NAME_LENGTH = 26;
	public static final int MAX_SEQEUNCE_NAME_LENGTH = 25;
}
