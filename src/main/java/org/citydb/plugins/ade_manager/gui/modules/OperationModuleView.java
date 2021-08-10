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
package org.citydb.plugins.ade_manager.gui.modules;

import org.citydb.config.i18n.Language;
import org.citydb.config.project.database.DatabaseConnection;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.core.database.DatabaseController;
import org.citydb.core.database.connection.DatabaseConnectionPool;
import org.citydb.core.registry.ObjectRegistry;
import org.citydb.gui.plugin.view.ViewController;
import org.citydb.plugins.ade_manager.ADEManagerPlugin;
import org.citydb.plugins.ade_manager.gui.ADEManagerPanel;
import org.citydb.plugins.ade_manager.util.Translator;
import org.citydb.util.event.EventDispatcher;
import org.citydb.util.event.EventHandler;
import org.citydb.util.log.Logger;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.text.MessageFormat;

public abstract class OperationModuleView implements EventHandler {
	protected static final int BORDER_THICKNESS = 5;
	protected static final int MINIMUM_REQUIRED_ORACLE_VERSION = 11;

	protected final Logger log = Logger.getInstance();
	protected final EventDispatcher eventDispatcher = ObjectRegistry.getInstance().getEventDispatcher();
	protected final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();
	protected final DatabaseController databaseController = ObjectRegistry.getInstance().getDatabaseController();
	protected final ViewController viewController;

	protected JPanel parentPanel;
	protected final ADEManagerPlugin plugin;

	public OperationModuleView(ADEManagerPanel parentPanel, ADEManagerPlugin plugin) {
		this.parentPanel = parentPanel;
		this.plugin = plugin;
		this.viewController = parentPanel.getViewController();
	}

	public abstract Component getViewComponent();
	public abstract void doTranslation();
	public abstract void loadSettings();
	public abstract void setSettings();
	
	protected void printErrorMessage(Exception e) {
		printErrorMessage("An unexpected error occurred.", e);
	}

	protected void printErrorMessage(String info, Exception e) {
		viewController.errorMessage(
				Translator.I18N.getString("ade_manager.error.title"),
				Translator.I18N.getString("ade_manager.error.message"));
		log.error(info, e);
	}
	
	protected boolean checkAndConnectToDB() throws SQLException {
		if (!dbPool.isConnected()) {
			DatabaseConnection conn = ObjectRegistry.getInstance().getConfig().getDatabaseConfig().getActiveConnection();
			if (viewController.showOptionDialog(
					Translator.I18N.getString("ade_manager.dialog.database.connect.title"),
					MessageFormat.format(Translator.I18N.getString("ade_manager.dialog.database.connect"),
							conn.getDescription(), conn.toConnectString()),
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
				databaseController.connect();
			}
		}

		if (!dbPool.isConnected()) {
			return false;
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

		return true;
	}
	
}
