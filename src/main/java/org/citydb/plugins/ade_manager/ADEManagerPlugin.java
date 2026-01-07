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
package org.citydb.plugins.ade_manager;

import org.citydb.core.plugin.Plugin;
import org.citydb.core.plugin.extension.config.ConfigExtension;
import org.citydb.core.plugin.extension.config.PluginConfigEvent;
import org.citydb.gui.ImpExpLauncher;
import org.citydb.gui.plugin.view.View;
import org.citydb.gui.plugin.view.ViewController;
import org.citydb.gui.plugin.view.ViewExtension;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.config.GuiConfig;
import org.citydb.plugins.ade_manager.gui.ADEManagerView;
import org.citydb.plugins.ade_manager.util.Translator;

import java.util.Locale;
import java.util.ResourceBundle;

public class ADEManagerPlugin extends Plugin implements ViewExtension, ConfigExtension<ConfigImpl> {
	private ADEManagerView view;
	private ConfigImpl config;

	public static void main(String[] args) {
		ImpExpLauncher launcher = new ImpExpLauncher()
				.withArgs(args)
				.withPlugin(new ADEManagerPlugin());

		launcher.start();
	}

	@Override
	public void initGuiExtension(ViewController viewController, Locale locale) {
		Translator.I18N = ResourceBundle.getBundle("org.citydb.plugins.ade_manager.i18n.language", locale);
		view = new ADEManagerView(viewController, this);
		loadSettings();
	}

	public void shutdownGui() {
		setSettings();
	}

	public void switchLocale(Locale locale) {
		Translator.I18N = ResourceBundle.getBundle("org.citydb.plugins.ade_manager.i18n.language", locale);
		view.switchLocale(locale);
	}

	public View getView() {
		return view;
	}
	
	public void loadSettings() {
		view.loadSettings();
	}

	public void setSettings() {
		view.setSettings();
	}

	@Override
	public void configLoaded(ConfigImpl config) {
		boolean reload = this.config != null;		
		setConfig(config);
		
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
				setSettings();
				break;
			case RESET_GUI_VIEW:
				config.setGuiConfig(new GuiConfig());
				break;
		}
	}
}
