/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 * 
 * (C) 2013 - 2016,
 * Chair of Geoinformatics,
 * Technische Universitaet Muenchen, Germany
 * http://www.gis.bgu.tum.de/
 * 
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 * 
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Muenchen <http://www.moss.de/>
 * 
 * The 3D City Database Importer/Exporter program is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 */
package org.citydb.plugins.ade_manager.config;

import java.io.File;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.citydb.config.project.plugin.PluginConfig;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.util.CoreConstants;

@XmlType(name="ADEManagerType", propOrder={			
		"xmlSchemaInputPath",		
		"transformationOutputPath",	
		"adeName",		
		"adeDescription",	
		"adeVersion",	
		"adeDbPrefix",	
		"initialObjectclassId",
		"adeRegistryInputPath"
})

public class ConfigImpl extends PluginConfig {
	private String xmlSchemaInputPath;
	private String transformationOutputPath;
	private String adeName;
	private String adeDescription;
	private String adeVersion;
	private String adeDbPrefix;
	private int initialObjectclassId;
	private String adeRegistryInputPath;	
	@XmlTransient
	private String inputGraphPath;
	@XmlTransient
	private String tmpGraphDirPath;
	
	public ConfigImpl() {
		inputGraphPath = "/org/citydb/plugins/ade_manager/graph/Working_Graph.ggx";
		
		File tmp = new File(CoreConstants.IMPEXP_DATA_DIR.resolve("graph").toString());
		if (!tmp.exists()) {
			tmp.mkdirs();
		}
		tmpGraphDirPath = tmp.getAbsolutePath();		
		
		initialObjectclassId = ADEMetadataManager.MIN_ADE_OBJECTCLASSID;
	}

	public String getTransformationOutputPath() {
		return transformationOutputPath;
	}

	public void setTransformationOutputPath(String transformationOutputPath) {
		this.transformationOutputPath = transformationOutputPath;					
	}

	public String getXMLschemaInputPath() {
		return xmlSchemaInputPath;
	}

	public void setXMLschemaInputPath(String xmlSchemaInputPath) {
		this.xmlSchemaInputPath = xmlSchemaInputPath;		
	}

	public String getInputGraphPath() {
		return inputGraphPath;
	}

	public String getAdeName() {
		return adeName;
	}

	public void setAdeName(String adeName) {
		this.adeName = adeName;
	}

	public String getAdeDescription() {
		return adeDescription;
	}

	public void setAdeDescription(String adeDescription) {
		this.adeDescription = adeDescription;
	}

	public String getAdeVersion() {
		return adeVersion;
	}

	public void setAdeVersion(String adeVersion) {
		this.adeVersion = adeVersion;
	}

	public String getAdeDbPrefix() {
		return adeDbPrefix;
	}

	public void setAdeDbPrefix(String adeDbPrefix) {
		this.adeDbPrefix = adeDbPrefix;
	}

	public int getInitialObjectclassId() {
		return initialObjectclassId;
	}

	public void setInitialObjectclassId(int initialObjectclassId) {
		this.initialObjectclassId = initialObjectclassId;
	}
	
	public String getTmpGraphDirPath() {
		return tmpGraphDirPath;
	}

	public String getAdeRegistryInputPath() {
		return adeRegistryInputPath;
	}

	public void setAdeRegistryInputPath(String adeRegistryInputPath) {
		this.adeRegistryInputPath = adeRegistryInputPath;
	}
	
}
