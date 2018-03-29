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