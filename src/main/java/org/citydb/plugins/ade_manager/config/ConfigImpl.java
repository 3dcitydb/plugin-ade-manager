/*
 * 3D City Database - The Open Source CityGML Database
 * https://www.3dcitydb.org/
 *
 * Copyright 2013 - 2026
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
package org.citydb.plugins.ade_manager.config;

import org.citydb.config.project.plugin.PluginConfig;
import org.citydb.core.util.CoreConstants;
import org.citydb.plugins.ade_manager.util.GlobalConstants;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.io.File;

@XmlType(name="ADEManagerType", propOrder={			
		"xmlSchemaInputPath",		
		"transformationOutputPath",	
		"adeName",		
		"adeDescription",	
		"adeVersion",	
		"adeDbPrefix",	
		"initialObjectclassId",
		"adeRegistryInputPath",
		"guiConfig"
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
	private GuiConfig guiConfig;
	@XmlTransient
	private String tmpGraphDirPath;
	
	public ConfigImpl() {		
		File tmp = new File(CoreConstants.IMPEXP_DATA_DIR.resolve(GlobalConstants.TMP_GRAPH_FOLDER_NAME).toString());
		if (!tmp.exists()) {
			tmp.mkdirs();
		}
		tmpGraphDirPath = tmp.getAbsolutePath();		
		
		initialObjectclassId = GlobalConstants.MIN_ADE_OBJECTCLASSID;
		guiConfig = new GuiConfig();
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

	public GuiConfig getGuiConfig() {
		return guiConfig;
	}

	public void setGuiConfig(GuiConfig guiConfig) {
		if (guiConfig != null) {
			this.guiConfig = guiConfig;
		}
	}
}
