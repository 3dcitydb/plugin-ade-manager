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
package org.citydb.plugins.ade_manager.gui.modules;

import org.citydb.config.i18n.Language;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.DatabaseController;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.event.EventDispatcher;
import org.citydb.event.EventHandler;
import org.citydb.gui.modules.database.operations.DatabaseOperationView;
import org.citydb.log.Logger;
import org.citydb.plugin.extension.view.ViewController;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.gui.ADEManagerPanel;
import org.citydb.plugins.ade_manager.util.Translator;
import org.citydb.registry.ObjectRegistry;

import javax.swing.*;
import java.sql.SQLException;
import java.text.MessageFormat;

public abstract class OperationModuleView extends DatabaseOperationView implements EventHandler{
	protected static final int BORDER_THICKNESS = 5;
	protected static final int MINIMUM_REQUIRED_ORACLE_VERSION = 11;

	protected final Logger log = Logger.getInstance();
	protected final EventDispatcher eventDispatcher = ObjectRegistry.getInstance().getEventDispatcher();
	protected final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();
	protected final DatabaseController databaseController = ObjectRegistry.getInstance().getDatabaseController();	
	protected final ViewController viewController;

	protected JPanel parentPanel;
	protected final ConfigImpl config;

	public OperationModuleView(ADEManagerPanel parentPanel, ConfigImpl config) {
		this.parentPanel = parentPanel;
		this.config = config;
		this.viewController = parentPanel.getViewController();
	}
	
	protected void printErrorMessage(Exception e) {
		printErrorMessage("An unexpected error occurred.", e);
	}

	protected void printErrorMessage(String info, Exception e) {
		viewController.errorMessage(
				Translator.I18N.getString("ade_manager.error.title"),
				Translator.I18N.getString("ade_manager.error.message"));
		log.error(info, e);
	}
	
	protected void checkAndConnectToDB() throws SQLException {
		String[] connectConfirm = { Language.I18N.getString("pref.kmlexport.connectDialog.line1"),
				Language.I18N.getString("pref.kmlexport.connectDialog.line3") };

		if (!dbPool.isConnected()) {
			if (JOptionPane.showConfirmDialog(parentPanel.getTopLevelAncestor(), connectConfirm,
					Language.I18N.getString("pref.kmlexport.connectDialog.title"),
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				databaseController.connect(true);
			}
			else
				throw new SQLException("Database is not connected.");
		}

		if (dbPool.getActiveDatabaseAdapter().getDatabaseType() == DatabaseType.ORACLE) {
			int currentOracleVersion = dbPool.getConnection().getMetaData().getDatabaseMajorVersion();
			if (currentOracleVersion < MINIMUM_REQUIRED_ORACLE_VERSION) {
				Object[] args = new Object[]{MINIMUM_REQUIRED_ORACLE_VERSION, currentOracleVersion};
				String warnMessage = MessageFormat.format(Translator.I18N.getString("ade_manager.db.warn.minimumDbVersionRequirement.msg"), args);
				viewController.warnMessage(Language.I18N.getString("common.dialog.warning.title"), warnMessage);
				throw new SQLException(warnMessage);
			}
		}
	}
	
}
