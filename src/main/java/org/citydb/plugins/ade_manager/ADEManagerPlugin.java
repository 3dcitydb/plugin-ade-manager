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
package org.citydb.plugins.ade_manager;

import java.util.Locale;
import java.util.ResourceBundle;

import org.citydb.ImpExpLauncher;
import org.citydb.plugin.Plugin;
import org.citydb.plugin.extension.config.ConfigExtension;
import org.citydb.plugin.extension.config.PluginConfigEvent;
import org.citydb.plugin.extension.view.View;
import org.citydb.plugin.extension.view.ViewController;
import org.citydb.plugin.extension.view.ViewExtension;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.gui.ADEManagerView;
import org.citydb.plugins.ade_manager.util.Translator;

public class ADEManagerPlugin implements Plugin, ViewExtension, ConfigExtension<ConfigImpl> {
	private ADEManagerView view;
	private ConfigImpl config;
	private Locale currentLocale;
	
	public static void main(String[] args) {
		ImpExpLauncher launcher = new ImpExpLauncher()
				.withArgs(args)
				.withPlugin(new ADEManagerPlugin());

		launcher.start();
	}

	public void shutdown() {
		saveSettings();
	}

	public void switchLocale(Locale locale) {
		if (locale.equals(currentLocale))
			return;
		Translator.I18N = ResourceBundle.getBundle("org.citydb.plugins.ade_manager.i18n.language", locale);
		currentLocale = locale;
		view.doTranslation();
	}

	public View getView() {
		return view;
	}
	
	public void loadSettings() {
		view.loadSettings();
	}

	public void saveSettings() {
		view.saveSettings();
	}

	@Override
	public void configLoaded(ConfigImpl config2) {
		boolean reload = this.config != null;		
		setConfig(config2);
		
		if (reload)
			loadSettings();
	}

	@Override
	public ConfigImpl getConfig() {
		return config;
	}

	public void setConfig(ConfigImpl config) {
		this.config = config;
	}
	
	@Override
	public void handleEvent(PluginConfigEvent event) {
		switch (event) {
		case RESET_DEFAULT_CONFIG:
			this.config = new ConfigImpl();
			loadSettings();
			break;
		case PRE_SAVE_CONFIG:
			saveSettings();
			break;
		}
	}

	@Override
	public void initViewExtension(ViewController viewController, Locale locale) {
		Translator.I18N = ResourceBundle.getBundle("org.citydb.plugins.ade_manager.i18n.language", locale);
		view = new ADEManagerView(viewController, this);
		loadSettings();
	}
	
}
