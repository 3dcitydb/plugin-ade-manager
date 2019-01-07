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
package org.citydb.plugins.ade_manager.transformation.graph;

public interface GraphNodeArcType {
	
	// Nodes
	String Schema = "Schema";
    String ComplexType = "ComplexType";
    String ComplexTypeProperty = "ComplexTypeProperty";
    String Extension = "Extension";    
    String SimpleAttribute = "SimpleAttribute";
    String EnumerationProperty = "EnumerationProperty";
    String ComplexAttribute = "ComplexAttribute"; 
    String GenericAttribute = "GenericAttribute"; 
    String GenericDataColumn = "GenericDataColumn"; 
    String BrepGeometryProperty = "BrepGeometryProperty";
    String PointOrLineGeometryProperty = "PointOrLineGeometryProperty";
    String HybridGeometryProperty = "HybridGeometryProperty";
    String ImplicitGeometryProperty = "ImplicitGeometryProperty";
    
    String DatabaseObject = "DatabaseObject";
    String Table = "Table";
    String DataTable = "DataTable";
    String Join = "Join";
    String JoinTable = "JoinTable";
    String Column = "Column";
    String PrimaryKeyColumn = "PrimaryKeyColumn";
    String JoinColumn = "JoinColumn";
    String ObjectClassIDColumn = "ObjectClassIDColumn";
    String NormalDataColumn = "NormalDataColumn";
    String RefGeometryColumn = "RefGeometryColumn";
    String InlineGeometryColumn = "InlineGeometryColumn";
    
    
    String Index = "Index";
    String Sequence = "Sequence";
    
    // Arcs
    String TargetColumn = "targetColumn";
    String TargetTable = "targetTable";
    String Contains = "contains";
    String InlineType = "inlineType";
    String MapsTo = "mapsTo";
    String Extends = "extends";
    String TargetType = "targetType";
    String BaseType = "baseType";
    String BelongsTo = "belongsTo";
    String JoinFrom = "joinFrom";
    String JoinTo = "joinTo";
    String ReverseProperty = "reverseProperty";
    String TreeHierarchy = "treeHierarchy";
}