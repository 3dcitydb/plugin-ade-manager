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
package org.citydb.plugins.ade_manager.gui;

import org.citydb.core.plugin.extension.view.ViewController;
import org.citydb.gui.util.GuiUtil;
import org.citydb.plugins.ade_manager.ADEManagerPlugin;
import org.citydb.plugins.ade_manager.gui.modules.ADERegistryPanel;
import org.citydb.plugins.ade_manager.gui.modules.ADETransformationPanel;
import org.citydb.plugins.ade_manager.gui.modules.OperationModuleView;

import javax.swing.*;
import java.awt.*;

public class ADEManagerPanel extends JPanel {
	private OperationModuleView registryModule;
	private OperationModuleView transformationModule;
	private final ViewController viewController;
	private final ADEManagerPlugin plugin;
	
	public ADEManagerPanel(ViewController viewController, ADEManagerPlugin plugin) {	
		this.plugin = plugin;
		this.viewController = viewController;
		initGui();
	}

	public ViewController getViewController() {
		return viewController;
	}

	private void initGui() {
		transformationModule = new ADETransformationPanel(this, plugin);
		registryModule = new ADERegistryPanel(this, plugin);

		JPanel mainScrollView = new JPanel();
		mainScrollView.setLayout(new GridBagLayout());

		mainScrollView.add(registryModule.getViewComponent(), GuiUtil.setConstraints(0, 0, 1, 0, GridBagConstraints.BOTH, 0, 10, 0, 10));
		mainScrollView.add(transformationModule.getViewComponent(), GuiUtil.setConstraints(0, 1, 1, 0, GridBagConstraints.BOTH, 0, 10, 0, 10));
		mainScrollView.add(Box.createVerticalGlue(), GuiUtil.setConstraints(0, 2, 1, 1, GridBagConstraints.BOTH, 0, 10, 0, 10));

		JScrollPane mainScrollPanel = new JScrollPane(mainScrollView);
		mainScrollPanel.setBorder(BorderFactory.createEmptyBorder());
		mainScrollPanel.setViewportBorder(BorderFactory.createEmptyBorder());

		setLayout(new GridBagLayout());
		add(mainScrollPanel, GuiUtil.setConstraints(0, 0, 1, 1, GridBagConstraints.BOTH, 15, 0, 10, 0));
	}

	// localized Labels and Strings
	public void doTranslation() {
		registryModule.doTranslation();
		transformationModule.doTranslation();
	}

	public void loadSettings() {
		registryModule.loadSettings();
		transformationModule.loadSettings();
	}

	public void setSettings() {
		registryModule.setSettings();
		transformationModule.setSettings();
	}
}
