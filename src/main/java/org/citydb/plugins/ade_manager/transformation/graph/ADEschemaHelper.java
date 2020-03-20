/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 *
 * Copyright 2013 - 2020
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.citydb.database.schema.mapping.GeometryType;
import org.citydb.database.schema.mapping.SimpleType;

public class ADEschemaHelper {
	@SuppressWarnings("serial")
	public static final  Map<String, SimpleType> SimpleAttributeTypes = new HashMap<String, SimpleType>() {
		{
			// reference URL: https://www.webucator.com/tutorial/learn-xml-schema/simple-type-elements.cfm
			// XML Primitive Data Types
			put("string", SimpleType.STRING);
			put("boolean", SimpleType.BOOLEAN);
			put("decimal", SimpleType.DOUBLE);
			put("float", SimpleType.DOUBLE);
			put("double", SimpleType.DOUBLE);		
			put("duration", SimpleType.STRING);
			put("dateTime", SimpleType.TIMESTAMP);
			put("time", SimpleType.TIMESTAMP);		
			put("date", SimpleType.TIMESTAMP);
			put("gYearMonth", SimpleType.DATE);
			put("gYear", SimpleType.DATE);
			put("gMonthDay", SimpleType.DATE);
			put("gDay", SimpleType.DATE);
			put("gMonth", SimpleType.DATE);
			put("hexBinary", SimpleType.STRING);
			put("base64Binary", SimpleType.STRING);
			put("anyURI", SimpleType.STRING);	
			put("QName", SimpleType.STRING);	
			put("NOTATION", SimpleType.STRING);
			
			// Built-in Derived Data Types
			put("normalizedString", SimpleType.STRING);
			put("token", SimpleType.STRING);
			put("language", SimpleType.STRING);
			put("NMTOKEN", SimpleType.STRING);
			put("NMTOKENS", SimpleType.STRING);
			put("Name", SimpleType.STRING);
			put("NCName", SimpleType.STRING);
			put("ID", SimpleType.STRING);
			put("IDREF", SimpleType.STRING);
			put("IDREFS", SimpleType.STRING);
			put("ENTITY", SimpleType.STRING);
			put("ENTITIES", SimpleType.STRING);
			put("integer", SimpleType.INTEGER);
			put("nonPositiveInteger", SimpleType.INTEGER);
			put("long", SimpleType.INTEGER);
			put("int", SimpleType.INTEGER);
			put("short", SimpleType.INTEGER);
			put("byte", SimpleType.INTEGER);
			put("nonNegativeInteger", SimpleType.INTEGER);
			put("unsignedLong", SimpleType.INTEGER);
			put("unsignedInt", SimpleType.INTEGER);
			put("unsignedShort", SimpleType.INTEGER);
			put("unsignedByte", SimpleType.INTEGER);
			put("positiveInteger", SimpleType.INTEGER);
			
			// gml simple Type
			put("NilReasonType", SimpleType.STRING);
			put("NilReasonEnumeration", SimpleType.STRING);
			put("SignType", SimpleType.STRING);
			put("booleanOrNilReason", SimpleType.STRING);
			put("doubleOrNilReason", SimpleType.STRING);
			put("integerOrNilReason", SimpleType.STRING);
			put("NameOrNilReason", SimpleType.STRING);
			put("stringOrNilReason", SimpleType.STRING);
			put("UomIdentifier", SimpleType.STRING);
			put("UomSymbol", SimpleType.STRING);
			put("UomURI", SimpleType.STRING);
			put("booleanList", SimpleType.STRING);
			put("doubleList", SimpleType.STRING);
			put("integerList", SimpleType.STRING);
			put("NameList", SimpleType.STRING);
			put("NCNameList", SimpleType.STRING);
			put("QNameList", SimpleType.STRING);
			put("booleanOrNilReasonList", SimpleType.STRING);
			put("NameOrNilReasonList", SimpleType.STRING);
			put("doubleOrNilReasonList", SimpleType.STRING);
			put("integerOrNilReasonList", SimpleType.STRING);	
			
			// additional gml simply structured type
			put("UnitOfMeasureType", SimpleType.STRING);
			
			// CityGML simple data type
			put("doubleBetween0and1", SimpleType.DOUBLE);
			put("doubleBetween0and1List", SimpleType.STRING);
			put("TransformationMatrix4x4Type", SimpleType.STRING);
			put("TransformationMatrix2x2Type", SimpleType.STRING);
			put("TransformationMatrix3x4Type", SimpleType.STRING);
			put("integerBetween0and4", SimpleType.INTEGER);
		}
	};
		
	@SuppressWarnings("serial")
	public static final  Map<String, ComplexAttributeType> ComplexAttributeTypes = new HashMap<String, ComplexAttributeType>() {
		{
			put("CodeType", new ComplexAttributeType("CodeType",
					new ArrayList<SimpleAttribute>(Arrays.asList(new SimpleAttribute("codespace", "string", true, "http://www.opengis.net/gml"), new SimpleAttribute("", "string", false, null)))));
			
			put("VectorType", new ComplexAttributeType("VectorType",
					new ArrayList<SimpleAttribute>(Arrays.asList(new SimpleAttribute("uom", "string", true, "http://www.opengis.net/gml"), new SimpleAttribute("", "double", false, null)))));
			
			put("LengthType", new ComplexAttributeType("LengthType",
					new ArrayList<SimpleAttribute>(Arrays.asList(new SimpleAttribute("uom", "string", true, "http://www.opengis.net/gml"), new SimpleAttribute("", "double", false, null)))));
			
			put("AngleType", new ComplexAttributeType("AngleType",
					new ArrayList<SimpleAttribute>(Arrays.asList(new SimpleAttribute("uom", "string", true, "http://www.opengis.net/gml"), new SimpleAttribute("", "double", false, null)))));
			
			put("SpeedType", new ComplexAttributeType("SpeedType",
					new ArrayList<SimpleAttribute>(Arrays.asList(new SimpleAttribute("uom", "string", true, "http://www.opengis.net/gml"), new SimpleAttribute("", "double", false, null)))));

			put("ScaleType", new ComplexAttributeType("ScaleType",
					new ArrayList<SimpleAttribute>(Arrays.asList(new SimpleAttribute("uom", "string", true, "http://www.opengis.net/gml"), new SimpleAttribute("", "double", false, null)))));
			
			put("AreaType", new ComplexAttributeType("AreaType",
					new ArrayList<SimpleAttribute>(Arrays.asList(new SimpleAttribute("uom", "string", true, "http://www.opengis.net/gml"), new SimpleAttribute("", "double", false, null)))));
			
			put("VolumeType", new ComplexAttributeType("VolumeType",
					new ArrayList<SimpleAttribute>(Arrays.asList(new SimpleAttribute("uom", "string", true, "http://www.opengis.net/gml"), new SimpleAttribute("", "double", false, null)))));
			
			put("MeasureType", new ComplexAttributeType("MeasureType",
					new ArrayList<SimpleAttribute>(Arrays.asList(new SimpleAttribute("uom", "string", true, "http://www.opengis.net/gml"), new SimpleAttribute("", "double", false, null)))));
					
			put("QuantityExtentType", new ComplexAttributeType("QuantityExtentType",
					new ArrayList<SimpleAttribute>(Arrays.asList(new SimpleAttribute("uom", "string", true, "http://www.opengis.net/gml"), new SimpleAttribute("", "double", false, null)))));
			
			//GML temporal
			put("TimePositionType", new ComplexAttributeType("TimePositionType",
					new ArrayList<SimpleAttribute>(Arrays.asList(new SimpleAttribute("frame", "string", true, "http://www.opengis.net/gml"), new SimpleAttribute("calendarEraName", "string", true, "http://www.opengis.net/gml"),
							new SimpleAttribute("indeterminatePosition", "string", true, "http://www.opengis.net/gml"), new SimpleAttribute("", "string", false, null)))));
			
			put("TimeIntervalLengthType", new ComplexAttributeType("TimeIntervalLengthType",
					new ArrayList<SimpleAttribute>(Arrays.asList(new SimpleAttribute("unit", "string", true, "http://www.opengis.net/gml"), new SimpleAttribute("radix", "integer", true, "http://www.opengis.net/gml"),
							new SimpleAttribute("factor", "integer", true, "http://www.opengis.net/gml"), new SimpleAttribute("", "double", false, null)))));
		}
	};
	
	@SuppressWarnings("serial")
	public static final  Map<String, String> CityGML_Hooks = new HashMap<String, String>() {
		{
			// hooks for appearance	
			put("_GenericApplicationPropertyOfAppearance", "Appearance");
			put("_GenericApplicationPropertyOfSurfaceData", "_SurfaceData");
			put("_GenericApplicationPropertyOfTexture", "_Texture");
			put("_GenericApplicationPropertyOfParameterizedTexture", "ParameterizedTexture");
			put("_GenericApplicationPropertyOfGeoreferencedTexture", "GeoreferencedTexture");
			put("_GenericApplicationPropertyOfTextureParameterization", "_TextureParameterization");
			put("_GenericApplicationPropertyOfTexCoordList", "TexCoordList");
			put("_GenericApplicationPropertyOfTexCoordGen", "TexCoordGen");
			put("_GenericApplicationPropertyOfX3DMaterial", "X3DMaterial");
			
			// hooks for building	
			put("_GenericApplicationPropertyOfAbstractBuilding", "_AbstractBuilding");
			put("_GenericApplicationPropertyOfBuilding", "Building");
			put("_GenericApplicationPropertyOfBuildingPart", "BuildingPart");
			put("_GenericApplicationPropertyOfBuildingInstallation", "BuildingInstallation");
			put("_GenericApplicationPropertyOfIntBuildingInstallation", "IntBuildingInstallation");
			put("_GenericApplicationPropertyOfBoundarySurface", "_BoundarySurface");
			put("_GenericApplicationPropertyOfRoofSurface", "RoofSurface");
			put("_GenericApplicationPropertyOfWallSurface", "WallSurface");
			put("_GenericApplicationPropertyOfGroundSurface", "GroundSurface");
			put("_GenericApplicationPropertyOfClosureSurface", "ClosureSurface");
			put("_GenericApplicationPropertyOfFloorSurface", "FloorSurface");
			put("_GenericApplicationPropertyOfOuterFloorSurface", "OuterFloorSurface");
			put("_GenericApplicationPropertyOfInteriorWallSurface", "InteriorWallSurface");
			put("_GenericApplicationPropertyOfCeilingSurface", "CeilingSurface");
			put("_GenericApplicationPropertyOfOuterCeilingSurface", "OuterCeilingSurface");
			put("_GenericApplicationPropertyOfOpening", "_Opening");
			put("_GenericApplicationPropertyOfWindow", "Window");
			put("_GenericApplicationPropertyOfDoor", "Door");
			put("_GenericApplicationPropertyOfRoom", "Room");
			put("_GenericApplicationPropertyOfBuildingFurniture", "BuildingFurniture");
			
			// hooks for bridge
			put("_GenericApplicationPropertyOfAbstractBridge", "_AbstractBridge");
			put("_GenericApplicationPropertyOfBridge", "Bridge");
			put("_GenericApplicationPropertyOfBridgePart", "BridgePart");
			put("_GenericApplicationPropertyOfBridgeConstructionElement", "BridgeConstructionElement");
			put("_GenericApplicationPropertyOfBridgeInstallation", "BridgeInstallation");
			put("_GenericApplicationPropertyOfIntBridgeInstallation", "IntBridgeInstallation");
			put("_GenericApplicationPropertyOfBoundarySurface", "_BoundarySurface");
			put("_GenericApplicationPropertyOfRoofSurface", "RoofSurface");
			put("_GenericApplicationPropertyOfWallSurface", "WallSurface");
			put("_GenericApplicationPropertyOfGroundSurface", "GroundSurface");
			put("_GenericApplicationPropertyOfClosureSurface", "ClosureSurface");
			put("_GenericApplicationPropertyOfFloorSurface", "FloorSurface");
			put("_GenericApplicationPropertyOfOuterFloorSurface", "OuterFloorSurface");
			put("_GenericApplicationPropertyOfInteriorWallSurface", "InteriorWallSurface");
			put("_GenericApplicationPropertyOfCeilingSurface", "CeilingSurface");
			put("_GenericApplicationPropertyOfOuterCeilingSurface", "OuterCeilingSurface");
			put("_GenericApplicationPropertyOfOpening", "_Opening");
			put("_GenericApplicationPropertyOfWindow", "Window");
			put("_GenericApplicationPropertyOfDoor", "Door");
			put("_GenericApplicationPropertyOfBridgeRoom", "BridgeRoom");
			put("_GenericApplicationPropertyOfBridgeFurniture", "BridgeFurniture");

			// hooks for cityfunrniture
			put("_GenericApplicationPropertyOfCityFurniture", "CityFurniture");
			
			// hooks for core
			put("_GenericApplicationPropertyOfCityModel", "CityModel");
			put("_GenericApplicationPropertyOfCityObject", "_CityObject");
			put("_GenericApplicationPropertyOfSite", "_Site");
			put("_GenericApplicationPropertyOfAddress", "Address");
			
			// hooks for cityobjectgroup
			put("_GenericApplicationPropertyOfCityObjectGroup", "CityObjectGroup");
			
			// hooks for landuse
			put("_GenericApplicationPropertyOfLandUse", "LandUse");
			
			// hooks for relief
			put("_GenericApplicationPropertyOfReliefFeature", "ReliefFeature");
			put("_GenericApplicationPropertyOfReliefComponent", "_ReliefComponent");
			put("_GenericApplicationPropertyOfTinRelief", "TinRelief");
			put("_GenericApplicationPropertyOfRasterRelief", "RasterRelief");
			put("_GenericApplicationPropertyOfMassPointRelief", "MassPointRelief");
			put("_GenericApplicationPropertyOfBreaklineRelief", "BreaklineRelief");
			
			// hooks for transportation
			put("_GenericApplicationPropertyOfTransportationObject", "_TransportationObject");
			put("_GenericApplicationPropertyOfTransportationComplex", "TransportationComplex");
			put("_GenericApplicationPropertyOfTrafficArea", "TrafficArea");
			put("_GenericApplicationPropertyOfAuxiliaryTrafficArea", "AuxiliaryTrafficArea");
			put("_GenericApplicationPropertyOfTrack", "Track");
			put("_GenericApplicationPropertyOfRoad", "Road");
			put("_GenericApplicationPropertyOfRailway", "Railway");
			put("_GenericApplicationPropertyOfSquare", "Square");
			
			// hooks for tunnel
			put("_GenericApplicationPropertyOfAbstractTunnel", "_AbstractTunnel");
			put("_GenericApplicationPropertyOfTunnel", "Tunnel");
			put("_GenericApplicationPropertyOfTunnelPart", "TunnelPart");
			put("_GenericApplicationPropertyOfTunnelInstallation", "TunnelInstallation");
			put("_GenericApplicationPropertyOfIntTunnelInstallation", "IntTunnelInstallation");
			put("_GenericApplicationPropertyOfBoundarySurface", "_BoundarySurface");
			put("_GenericApplicationPropertyOfRoofSurface", "RoofSurface");
			put("_GenericApplicationPropertyOfWallSurface", "WallSurface");
			put("_GenericApplicationPropertyOfGroundSurface", "GroundSurface");
			put("_GenericApplicationPropertyOfClosureSurface", "ClosureSurface");
			put("_GenericApplicationPropertyOfFloorSurface", "FloorSurface");
			put("_GenericApplicationPropertyOfOuterFloorSurface", "OuterFloorSurface");
			put("_GenericApplicationPropertyOfInteriorWallSurface", "InteriorWallSurface");
			put("_GenericApplicationPropertyOfCeilingSurface", "CeilingSurface");
			put("_GenericApplicationPropertyOfOuterCeilingSurface", "OuterCeilingSurface");
			put("_GenericApplicationPropertyOfOpening", "_Opening");
			put("_GenericApplicationPropertyOfWindow", "Window");
			put("_GenericApplicationPropertyOfDoor", "Door");
			put("_GenericApplicationPropertyOfHollowSpace", "HollowSpace");
			put("_GenericApplicationPropertyOfTunnelFurniture", "TunnelFurniture");
			
			// hooks for vegetation
			put("_GenericApplicationPropertyOfVegetationObject", "_VegetationObject");
			put("_GenericApplicationPropertyOfPlantCover", "PlantCover");
			put("_GenericApplicationPropertyOfSolitaryVegetationObject", "SolitaryVegetationObject");
			
			// hooks for waterbody
			put("_GenericApplicationPropertyOfWaterObject", "_WaterObject");
			put("_GenericApplicationPropertyOfWaterBody", "WaterBody");
			put("_GenericApplicationPropertyOfWaterBoundarySurface", "_WaterBoundarySurface");
			put("_GenericApplicationPropertyOfWaterSurface", "WaterSurface");
			put("_GenericApplicationPropertyOfWaterGroundSurface", "WaterGroundSurface");
			put("_GenericApplicationPropertyOfWaterClosureSurface", "WaterClosureSurface");			
		}
	};
	
	@SuppressWarnings("serial")
	public static final  Map<QName, String> CityDB_Tables = new HashMap<QName, String>() {
		{	
			put(new QName("http://www.opengis.net/gml", "AbstractGMLType"), "cityobject");
			put(new QName("http://www.opengis.net/gml", "AbstractFeatureType"), "cityobject");
			put(new QName("http://www.opengis.net/citygml/1.0", "ObjectClass"), "objectclass");
			put(new QName("http://www.opengis.net/citygml/2.0", "ObjectClass"), "objectclass");
			put(new QName("http://www.opengis.net/citygml/1.0", "SurfaceGeometry"), "surface_geometry");
			put(new QName("http://www.opengis.net/citygml/2.0", "SurfaceGeometry"), "surface_geometry");
			put(new QName("http://www.opengis.net/citygml/1.0", "AddressType"), "address");
			put(new QName("http://www.opengis.net/citygml/2.0", "AddressType"), "address");
			put(new QName("http://www.opengis.net/citygml/1.0", "ExternalReferenceType"), "external_reference");
			put(new QName("http://www.opengis.net/citygml/2.0", "ExternalReferenceType"), "external_reference");
			put(new QName("http://www.opengis.net/citygml/1.0", "AbstractCityObjectType"), "cityobject");
			put(new QName("http://www.opengis.net/citygml/2.0", "AbstractCityObjectType"), "cityobject");
			put(new QName("http://www.opengis.net/citygml/1.0", "AbstractSiteType"), "cityobject");
			put(new QName("http://www.opengis.net/citygml/2.0", "AbstractSiteType"), "cityobject");
			put(new QName("http://www.opengis.net/citygml/1.0", "ImplicitGeometryType"), "implicit_geometry");
			put(new QName("http://www.opengis.net/citygml/2.0", "ImplicitGeometryType"), "implicit_geometry");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "AbstractBuildingType"), "building");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "AbstractBuildingType"), "building");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "BuildingPartType"), "building");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "BuildingPartType"), "building");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "BuildingType"), "building");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "BuildingType"), "building");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "AbstractBoundarySurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "AbstractBoundarySurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "CeilingSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "CeilingSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "InteriorWallSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "InteriorWallSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "FloorSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "FloorSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "RoofSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "RoofSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "WallSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "WallSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "GroundSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "GroundSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "ClosureSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "ClosureSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "OuterCeilingSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "OuterCeilingSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "OuterFloorSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "OuterFloorSurfaceType"), "thematic_surface");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "AbstractOpeningType"), "opening");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "AbstractOpeningType"), "opening");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "WindowType"), "opening");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "WindowType"), "opening");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "DoorType"), "opening");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "DoorType"), "opening");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "BuildingInstallationType"), "building_installation");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "BuildingInstallationType"), "building_installation");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "IntBuildingInstallationType"), "building_installation");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "IntBuildingInstallationType"), "building_installation");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "RoomType"), "room");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "RoomType"), "room");
			put(new QName("http://www.opengis.net/citygml/building/1.0", "BuildingFurnitureType"), "building_furniture");
			put(new QName("http://www.opengis.net/citygml/building/2.0", "BuildingFurnitureType"), "building_furniture");
			put(new QName("http://www.opengis.net/citygml/cityfurniture/1.0", "CityFurnitureType"), "city_furniture");
			put(new QName("http://www.opengis.net/citygml/cityfurniture/2.0", "CityFurnitureType"), "city_furniture");
			put(new QName("http://www.opengis.net/citygml/generics/1.0", "GenericCityObjectType"), "generic_cityobject");
			put(new QName("http://www.opengis.net/citygml/generics/2.0", "GenericCityObjectType"), "generic_cityobject");
			put(new QName("http://www.opengis.net/citygml/landuse/1.0", "LandUseType"), "land_use");
			put(new QName("http://www.opengis.net/citygml/landuse/2.0", "LandUseType"), "land_use");
			put(new QName("http://www.opengis.net/citygml/cityobjectgroup/1.0", "CityObjectGroupType"), "cityobjectgroup");
			put(new QName("http://www.opengis.net/citygml/cityobjectgroup/2.0", "CityObjectGroupType"), "cityobjectgroup");
			put(new QName("http://www.opengis.net/citygml/relief/1.0", "ReliefFeatureType"), "relief_feature");
			put(new QName("http://www.opengis.net/citygml/relief/2.0", "ReliefFeatureType"), "relief_feature");
			put(new QName("http://www.opengis.net/citygml/relief/1.0", "AbstractReliefComponentType"), "relief_component");
			put(new QName("http://www.opengis.net/citygml/relief/2.0", "AbstractReliefComponentType"), "relief_component");
			put(new QName("http://www.opengis.net/citygml/relief/1.0", "TINReliefType"), "tin_relief");
			put(new QName("http://www.opengis.net/citygml/relief/2.0", "TINReliefType"), "tin_relief");
			put(new QName("http://www.opengis.net/citygml/relief/1.0", "MassPointReliefType"), "masspoint_relief");
			put(new QName("http://www.opengis.net/citygml/relief/2.0", "MassPointReliefType"), "masspoint_relief");
			put(new QName("http://www.opengis.net/citygml/relief/1.0", "BreaklineReliefType"), "breakline_relief");
			put(new QName("http://www.opengis.net/citygml/relief/2.0", "BreaklineReliefType"), "breakline_relief");
			put(new QName("http://www.opengis.net/citygml/relief/1.0", "RasterReliefType"), "raster_relief");
			put(new QName("http://www.opengis.net/citygml/relief/2.0", "RasterReliefType"), "raster_relief");
			put(new QName("http://www.opengis.net/citygml/waterbody/1.0", "AbstractWaterObjectType"), "cityobject");
			put(new QName("http://www.opengis.net/citygml/waterbody/2.0", "AbstractWaterObjectType"), "cityobject");
			put(new QName("http://www.opengis.net/citygml/waterbody/1.0", "WaterBodyType"), "waterbody");
			put(new QName("http://www.opengis.net/citygml/waterbody/2.0", "WaterBodyType"), "waterbody");
			put(new QName("http://www.opengis.net/citygml/waterbody/1.0", "AbstractWaterBoundarySurfaceType"), "waterboundary_surface");
			put(new QName("http://www.opengis.net/citygml/waterbody/2.0", "AbstractWaterBoundarySurfaceType"), "waterboundary_surface");
			put(new QName("http://www.opengis.net/citygml/waterbody/1.0", "WaterSurfaceType"), "waterboundary_surface");
			put(new QName("http://www.opengis.net/citygml/waterbody/2.0", "WaterSurfaceType"), "waterboundary_surface");
			put(new QName("http://www.opengis.net/citygml/waterbody/1.0", "WaterGroundSurfaceType"), "waterboundary_surface");
			put(new QName("http://www.opengis.net/citygml/waterbody/2.0", "WaterGroundSurfaceType"), "waterboundary_surface");
			put(new QName("http://www.opengis.net/citygml/waterbody/1.0", "WaterClosureSurfaceType"), "waterboundary_surface");
			put(new QName("http://www.opengis.net/citygml/waterbody/2.0", "WaterClosureSurfaceType"), "waterboundary_surface");
			put(new QName("http://www.opengis.net/citygml/transportation/1.0", "AbstractTransportationObjectType"), "cityobject");
			put(new QName("http://www.opengis.net/citygml/transportation/2.0", "AbstractTransportationObjectType"), "cityobject");
			put(new QName("http://www.opengis.net/citygml/transportation/1.0", "TransportationComplexType"), "transportation_complex");
			put(new QName("http://www.opengis.net/citygml/transportation/2.0", "TransportationComplexType"), "transportation_complex");
			put(new QName("http://www.opengis.net/citygml/transportation/1.0", "TrackType"), "transportation_complex");
			put(new QName("http://www.opengis.net/citygml/transportation/2.0", "TrackType"), "transportation_complex");
			put(new QName("http://www.opengis.net/citygml/transportation/1.0", "RailwayType"), "transportation_complex");
			put(new QName("http://www.opengis.net/citygml/transportation/2.0", "RailwayType"), "transportation_complex");
			put(new QName("http://www.opengis.net/citygml/transportation/1.0", "RoadType"), "transportation_complex");
			put(new QName("http://www.opengis.net/citygml/transportation/2.0", "RoadType"), "transportation_complex");
			put(new QName("http://www.opengis.net/citygml/transportation/1.0", "SquareType"), "transportation_complex");
			put(new QName("http://www.opengis.net/citygml/transportation/2.0", "SquareType"), "transportation_complex");
			put(new QName("http://www.opengis.net/citygml/transportation/1.0", "TrafficAreaType"), "traffic_area");
			put(new QName("http://www.opengis.net/citygml/transportation/2.0", "TrafficAreaType"), "traffic_area");
			put(new QName("http://www.opengis.net/citygml/transportation/1.0", "AuxiliaryTrafficAreaType"), "traffic_area");
			put(new QName("http://www.opengis.net/citygml/transportation/2.0", "AuxiliaryTrafficAreaType"), "traffic_area");
			put(new QName("http://www.opengis.net/citygml/vegetation/1.0", "AbstractVegetationObjectType"), "cityobject");
			put(new QName("http://www.opengis.net/citygml/vegetation/2.0", "AbstractVegetationObjectType"), "cityobject");
			put(new QName("http://www.opengis.net/citygml/vegetation/1.0", "SolitaryVegetationObjectType"), "solitary_vegetat_object");
			put(new QName("http://www.opengis.net/citygml/vegetation/2.0", "SolitaryVegetationObjectType"), "solitary_vegetat_object");
			put(new QName("http://www.opengis.net/citygml/vegetation/1.0", "PlantCoverType"), "plant_cover");
			put(new QName("http://www.opengis.net/citygml/vegetation/2.0", "PlantCoverType"), "plant_cover");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "AbstractBridgeType"), "bridge");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "BridgePartType"), "bridge");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "BridgeType"), "bridge");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "AbstractBoundarySurfaceType"), "bridge_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "CeilingSurfaceType"), "bridge_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "InteriorWallSurfaceType"), "bridge_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "FloorSurfaceType"), "bridge_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "RoofSurfaceType"), "bridge_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "WallSurfaceType"), "bridge_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "GroundSurfaceType"), "bridge_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "ClosureSurfaceType"), "bridge_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "OuterCeilingSurfaceType"), "bridge_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "OuterFloorSurfaceType"), "bridge_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "AbstractOpeningType"), "bridge_opening");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "WindowType"), "bridge_opening");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "DoorType"), "bridge_opening");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "BridgeConstructionElementType"), "bridge_constr_element");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "BridgeInstallationType"), "bridge_installation");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "IntBridgeInstallationType"), "bridge_installation");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "BridgeRoomType"), "bridge_room");
			put(new QName("http://www.opengis.net/citygml/bridge/2.0", "BridgeFurnitureType"), "bridge_furniture");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "AbstractTunnelType"), "tunnel");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "TunnelPartType"), "tunnel");			
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "TunnelType"), "tunnel");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "AbstractBoundarySurfaceType"), "tunnel_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "CeilingSurfaceType"), "tunnel_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "InteriorWallSurfaceType"), "tunnel_thematic_surface");		
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "FloorSurfaceType"), "tunnel_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "RoofSurfaceType"), "tunnel_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "WallSurfaceType"), "tunnel_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "GroundSurfaceType"), "tunnel_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "ClosureSurfaceType"), "tunnel_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "OuterCeilingSurfaceType"), "tunnel_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "OuterFloorSurfaceType"), "tunnel_thematic_surface");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "AbstractOpeningType"), "tunnel_opening");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "WindowType"), "tunnel_opening");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "DoorType"), "tunnel_opening");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "TunnelInstallationType"), "tunnel_installation");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "IntTunnelInstallationType"), "tunnel_installation");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "HollowSpaceType"), "tunnel_hollow_space");
			put(new QName("http://www.opengis.net/citygml/tunnel/2.0", "TunnelFurnitureType"), "tunnel_furniture");
			put(new QName("http://www.opengis.net/citygml/appearance/1.0", "AppearanceType"), "appearance");
			put(new QName("http://www.opengis.net/citygml/appearance/2.0", "AppearanceType"), "appearance");
			put(new QName("http://www.opengis.net/citygml/appearance/1.0", "AbstractSurfaceDataType"), "surface_data");
			put(new QName("http://www.opengis.net/citygml/appearance/2.0", "AbstractSurfaceDataType"), "surface_data");
			put(new QName("http://www.opengis.net/citygml/appearance/1.0", "X3DMaterialType"), "surface_data");
			put(new QName("http://www.opengis.net/citygml/appearance/2.0", "X3DMaterialType"), "surface_data");
			put(new QName("http://www.opengis.net/citygml/appearance/1.0", "AbstractTextureType"), "surface_data");
			put(new QName("http://www.opengis.net/citygml/appearance/2.0", "AbstractTextureType"), "surface_data");
			put(new QName("http://www.opengis.net/citygml/appearance/1.0", "ParameterizedTextureType"), "surface_data");
			put(new QName("http://www.opengis.net/citygml/appearance/2.0", "ParameterizedTextureType"), "surface_data");
			put(new QName("http://www.opengis.net/citygml/appearance/1.0", "GeoreferencedTextureType"), "surface_data");
			put(new QName("http://www.opengis.net/citygml/appearance/2.0", "GeoreferencedTextureType"), "surface_data");
		}
	};
	
	@SuppressWarnings("serial")
	public static final LinkedHashSet<String> CityGML_Namespaces = new LinkedHashSet<String>() {{
		add("http://www.opengis.net/citygml/1.0");
		add("http://www.opengis.net/citygml/appearance/1.0");
		add("http://www.opengis.net/citygml/building/1.0");
		add("http://www.opengis.net/citygml/cityfurniture/1.0");
		add("http://www.opengis.net/citygml/cityobjectgroup/1.0");
		add("http://www.opengis.net/citygml/generics/1.0");
		add("http://www.opengis.net/citygml/landuse/1.0");
		add("http://www.opengis.net/citygml/relief/1.0");
		add("http://www.opengis.net/citygml/transportation/1.0");
		add("http://www.opengis.net/citygml/vegetation/1.0");
		add("http://www.opengis.net/citygml/waterbody/1.0");		
		add("http://www.opengis.net/citygml/texturedsurface/1.0");
		add("http://www.opengis.net/citygml/2.0");
		add("http://www.opengis.net/citygml/appearance/2.0");
		add("http://www.opengis.net/citygml/bridge/2.0");
		add("http://www.opengis.net/citygml/building/2.0");
		add("http://www.opengis.net/citygml/cityfurniture/2.0");
		add("http://www.opengis.net/citygml/cityobjectgroup/2.0");
		add("http://www.opengis.net/citygml/generics/2.0");
		add("http://www.opengis.net/citygml/landuse/2.0");
		add("http://www.opengis.net/citygml/relief/2.0");
		add("http://www.opengis.net/citygml/transportation/2.0");
		add("http://www.opengis.net/citygml/tunnel/2.0");
		add("http://www.opengis.net/citygml/vegetation/2.0");
		add("http://www.opengis.net/citygml/waterbody/2.0");
		add("http://www.opengis.net/citygml/texturedsurface/2.0");
	}};
	
	@SuppressWarnings("serial")
	public static final Map<String, GeometryType> BrepGeometryPropertyTypes = new HashMap<String, GeometryType>() {
		{
			put("SurfacePropertyType", GeometryType.ABSTRACT_SURFACE);
			put("MultiSurfacePropertyType", GeometryType.MULTI_SURFACE);
			put("SolidPropertyType", GeometryType.ABSTRACT_SOLID);
			put("MultiSolidPropertyType", GeometryType.MULTI_SOLID);
		}
	};
	
	@SuppressWarnings("serial")
	public static final Map<String, GeometryType> PointOrLineGeometryPropertyTypes = new HashMap<String, GeometryType>() {
		{
			put("PointPropertyType", GeometryType.POINT);
			put("MultiPointPropertyType", GeometryType.MULTI_POINT);
			put("CurvePropertyType", GeometryType.MULTI_CURVE);
			put("MultiCurvePropertyType", GeometryType.MULTI_CURVE);
		}
	};
	
	@SuppressWarnings("serial")
	public static final Map<String, GeometryType> HybridGeometryPropertyTypes = new HashMap<String, GeometryType>() {
		{
			put("GeometryPropertyType", GeometryType.ABSTRACT_GEOMETRY);
			put("MultiGeometryPropertyType", GeometryType.ABSTRACT_GEOMETRY);
			put("GeometricComplexPropertyType", GeometryType.GEOMETRIC_COMPLEX);
		}
	};
	
	public static class SimpleAttribute {
		private String propertyName;
		private String propertyTypeName;
		private String namespaceUri;
		private boolean isAttribute;
		private int minOccurs = 1;
		private int maxOccurs = 1;
		
		SimpleAttribute (String propertyName, String propertyTypeName, boolean isAttribute, String namespaceUri) {
			this.propertyName = propertyName;
			this.propertyTypeName = propertyTypeName;
			this.setNamespaceUri(namespaceUri);
			this.isAttribute = isAttribute;			
		}
		
		public String getPropertyName() {
			return this.propertyName;
		}
				
		public String getPropertyTypeName() {
			return this.propertyTypeName;
		}
		
		public boolean isXMLAttribute() {
			return this.isAttribute;
		}

		public int getMaxOccurs() {
			return maxOccurs;
		}

		public int getMinOccurs() {
			return minOccurs;
		}

		public String getNamespaceUri() {
			return namespaceUri;
		}

		public void setNamespaceUri(String namespaceUri) {
			this.namespaceUri = namespaceUri;
		}

	}
	
	public static class ComplexAttributeType {
		private String propertyTypeName;
		private List<SimpleAttribute> simpleBasicDataProperties;
		
		ComplexAttributeType (String propertyTypeName) {
			this.propertyTypeName = propertyTypeName;
			simpleBasicDataProperties = new ArrayList<SimpleAttribute>();
		}
		
		ComplexAttributeType (String propertyTypeName, List<SimpleAttribute> simpleBasicProperties) {
			this.propertyTypeName = propertyTypeName;
			this.simpleBasicDataProperties = simpleBasicProperties;
		}
		
		public String getXsTypeName() {
			return this.propertyTypeName;
		}
		
		public void addSimpleBasicProperties(SimpleAttribute simpleBasicPropertyMember) {
			this.simpleBasicDataProperties.add(simpleBasicPropertyMember);
		}
		
		public List<SimpleAttribute> getSimpleAttributes() {
			return this.simpleBasicDataProperties;
		}
	}
}
