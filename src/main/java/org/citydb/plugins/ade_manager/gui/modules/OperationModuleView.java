package org.citydb.plugins.ade_manager.gui.modules;

import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.citydb.config.i18n.Language;
import org.citydb.config.project.database.DatabaseConfigurationException;
import org.citydb.database.DatabaseController;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.version.DatabaseVersionException;
import org.citydb.event.EventDispatcher;
import org.citydb.event.EventHandler;
import org.citydb.log.Logger;
import org.citydb.modules.database.gui.operations.DatabaseOperationView;
import org.citydb.plugin.extension.view.ViewController;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.gui.ADEManagerPanel;
import org.citydb.registry.ObjectRegistry;

public abstract class OperationModuleView extends DatabaseOperationView implements EventHandler{
	protected static final int BORDER_THICKNESS = 3;
	protected static final int MAX_TEXTFIELD_HEIGHT = 20;
	protected static final int MAX_LABEL_WIDTH = 60;
	protected static final int BUTTON_WIDTH = 155;
	
	protected final Logger LOG = Logger.getInstance();	
	protected final EventDispatcher eventDispatcher = ObjectRegistry.getInstance().getEventDispatcher();
	protected final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();
	protected final DatabaseController databaseController = ObjectRegistry.getInstance().getDatabaseController();	
	protected final ViewController viewContoller;
	protected final ReentrantLock mainLock = new ReentrantLock();
	
	protected JPanel parentPanel;
	protected final ConfigImpl config;
	protected int standardButtonHeight = (new JButton("D")).getPreferredSize().height;

	public OperationModuleView(ADEManagerPanel parentPanel, ConfigImpl config) {
		this.parentPanel = parentPanel;
		this.config = config;
		this.viewContoller = parentPanel.getViewController();
	}
	
	protected void printErrorMessage(Exception e) {
		printErrorMessage("Unexpected Error", e);
	}
	
	protected void printErrorMessage(String info, Exception e) {
		LOG.error(info + ". Cause: " + e.getMessage());			
		Throwable cause = e.getCause();
		while (cause != null) {
			LOG.error("Cause: " + cause.getMessage());
			cause = cause.getCause();
		}
	}
	
	protected void checkAndConnectToDB() throws SQLException {
		String[] connectConfirm = { Language.I18N.getString("pref.kmlexport.connectDialog.line1"),
				Language.I18N.getString("pref.kmlexport.connectDialog.line3") };

		if (!dbPool.isConnected() && JOptionPane.showConfirmDialog(parentPanel.getTopLevelAncestor(), connectConfirm,
				Language.I18N.getString("pref.kmlexport.connectDialog.title"),
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			try {
				databaseController.connect(true);
			} catch (DatabaseConfigurationException | DatabaseVersionException | SQLException e) {
				throw new SQLException("Failed to connect to the target database", e);
			}
		}
	}
	
}
