package org.citydb.plugins.ade_manager.gui.modules;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.citydb.config.i18n.Language;
import org.citydb.config.project.database.DBOperationType;
import org.citydb.config.project.database.DatabaseConfigurationException;
import org.citydb.config.project.exporter.SimpleQuery;
import org.citydb.config.project.global.LogLevel;
import org.citydb.database.DatabaseController;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.database.version.DatabaseVersionException;
import org.citydb.event.Event;
import org.citydb.event.EventDispatcher;
import org.citydb.event.global.InterruptEvent;
import org.citydb.gui.util.GuiUtil;

import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.deletion.DBDeleteController;
import org.citydb.plugins.ade_manager.deletion.DBDeleteException;
import org.citydb.plugins.ade_manager.gui.ADEManagerPanel;
import org.citydb.plugins.ade_manager.gui.popup.StatusDialog;
import org.citydb.plugins.ade_manager.gui.util.FilterPanel;
import org.citydb.query.Query;
import org.citydb.query.builder.QueryBuildException;
import org.citydb.query.builder.config.ConfigQueryBuilder;
import org.citydb.registry.ObjectRegistry;

public class ADEDeletePanel extends OperationModuleView {
	private FilterPanel filterPanel;
	private JButton deleteButton = new JButton();
	private JPanel component;	

	public ADEDeletePanel(ADEManagerPanel parentPanel, ConfigImpl config) {
		super(parentPanel, config);		
		initGui();
	}
	
	protected void initGui() {	
		component = new JPanel();
		component.setLayout(new GridBagLayout());

		filterPanel = new FilterPanel(viewContoller, config);
		
		JPanel deletePanel = new JPanel();
		deletePanel.setLayout(new GridBagLayout());
		deletePanel.add(deleteButton, GuiUtil.setConstraints(0,0,2,1,1.0,0.0,GridBagConstraints.NONE,0,0,0,0));	
		
		int index = 0;
		component.add(filterPanel, GuiUtil.setConstraints(0,index++,1.0,1.0,GridBagConstraints.BOTH,0,0,0,0));
		component.add(deletePanel, GuiUtil.setConstraints(0,index++,1.0,1.0,GridBagConstraints.BOTH,0,0,0,0));
	
		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						doDelete();						
					}
				};
				thread.setDaemon(true);
				thread.start();
			}
		});
	}
	
	@Override
	public String getLocalizedTitle() {
		return "Deletion";
	}

	@Override
	public Component getViewComponent() {
		return component;
	}

	@Override
	public String getToolTip() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public DBOperationType getType() {
		return null;
	}

	@Override
	public void doTranslation() {
		filterPanel.doTranslation();
		deleteButton.setText("Delete");
	}

	@Override
	public void setEnabled(boolean enable) {
		// 
	}

	@Override
	public void loadSettings() {
		filterPanel.loadSettings();
	}

	@Override
	public void setSettings() {
		filterPanel.setSettings();
	}
	
	private void doDelete() {
		final ReentrantLock lock = this.mainLock;
		lock.lock();
		
		try {
			viewContoller.clearConsole();
			setSettings();
			dbPool.purge();
			
			final DatabaseController databaseController = ObjectRegistry.getInstance().getDatabaseController();
			if (!databaseController.isConnected()) {
				try {
					databaseController.connect(true);
					if (!databaseController.isConnected())
						return;
				} catch (DatabaseConfigurationException | DatabaseVersionException | SQLException e) {
					//
				}
			}

			SimpleQuery simpleQueryConfig = config.getDeleteQuery();
			if (simpleQueryConfig.getFeatureTypeFilter().getTypeNames().isEmpty()) {
				viewContoller.errorMessage(Language.I18N.getString("export.dialog.error.incorrectData"),
						Language.I18N.getString("common.dialog.error.incorrectData.featureClass"));
				return;
			}
			
			final AbstractDatabaseAdapter databaseAdapter = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter();
			final SchemaMapping schemaMapping = ObjectRegistry.getInstance().getSchemaMapping();

			// build query from filter settings
			Query query = null;
			try {
				ConfigQueryBuilder queryBuilder = new ConfigQueryBuilder(schemaMapping, databaseAdapter);
				query = queryBuilder.buildQuery(config.getDeleteQuery(), config.getNamespaceFilter());			
			} catch (QueryBuildException e) {
				LOG.error("Failed to build the delete query expression.");
				return;
			}
			
			final EventDispatcher eventDispatcher = ObjectRegistry.getInstance().getEventDispatcher();
			final StatusDialog deleteDialog = new StatusDialog(viewContoller.getTopFrame(), 
					"CityGML Delete",
					null,
					"Deleting city objects",
					true);

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					deleteDialog.setLocationRelativeTo(viewContoller.getTopFrame());
					deleteDialog.setVisible(true);
				}
			});
			
			deleteDialog.getCancelButton().addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							eventDispatcher.triggerEvent(new InterruptEvent(
									"User abort of database delete.", 
									LogLevel.INFO, 
									Event.GLOBAL_CHANNEL,
									this));
						}
					});
				}
			});

			viewContoller.setStatusText("Delete");
			LOG.info("Initializing database delete...");

			DBDeleteController deleter = new DBDeleteController(query);
			boolean success = false;
			try {
				success = deleter.doProcess();
				if (JOptionPane.showConfirmDialog(parentPanel.getTopLevelAncestor(), 
						"Do you want to clean up the global appearances, which are not referenced by any other features any more?",
						"Cleaning up global appearances?",
						JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					success = deleter.cleanupGlobalAppearances();
				}				
			} catch (DBDeleteException e) {
				LOG.error(e.getMessage());
				Throwable cause = e.getCause();
				while (cause != null) {
					LOG.error("Cause: " + cause.getMessage());
					cause = cause.getCause();
				}
			}

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					deleteDialog.dispose();
				}
			});
			
			// cleanup
			try {
				eventDispatcher.flushEvents();
			} catch (InterruptedException e1) {
				//
			}
			
			deleter.cleanup();
			dbPool.purge();

			if (success) {
				LOG.info("Database delete successfully finished.");
			} else {
				LOG.warn("Database delete aborted.");
			}

			viewContoller.setStatusText(Language.I18N.getString("main.status.ready.label"));
		}
		finally {
			lock.unlock();
		}
	}
	
	@Override
	public void handleEvent(Event event) throws Exception {}
	
}
