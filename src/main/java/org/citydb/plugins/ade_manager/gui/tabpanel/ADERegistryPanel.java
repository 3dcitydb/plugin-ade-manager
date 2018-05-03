package org.citydb.plugins.ade_manager.gui.tabpanel;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.citydb.config.i18n.Language;
import org.citydb.config.project.database.DBOperationType;
import org.citydb.config.project.database.DatabaseConfigurationException;
import org.citydb.database.DatabaseController;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.version.DatabaseVersionException;
import org.citydb.event.Event;
import org.citydb.event.EventDispatcher;
import org.citydb.event.EventHandler;
import org.citydb.gui.util.GuiUtil;
import org.citydb.log.Logger;
import org.citydb.modules.database.gui.operations.DatabaseOperationView;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.event.ScriptCreationEvent;
import org.citydb.plugins.ade_manager.gui.ADEManagerPanel;
import org.citydb.plugins.ade_manager.gui.popup.ScriptDialog;
import org.citydb.plugins.ade_manager.gui.table.ADEMetadataRow;
import org.citydb.plugins.ade_manager.gui.table.TableModel;
import org.citydb.plugins.ade_manager.registry.ADERegistrationController;
import org.citydb.plugins.ade_manager.registry.ADERegistrationException;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataInfo;
import org.citydb.registry.ObjectRegistry;

public class ADERegistryPanel extends DatabaseOperationView implements EventHandler {
	private final ADEManagerPanel parentPanel;
	private final ConfigImpl config;
	
	private JPanel component;
	private JPanel browseRegistryPanel;
	private JTextField browseRegistryText = new JTextField();
	private JButton browseRegistryButton = new JButton();		
	private JPanel adeButtonsPanel;
	private JButton registerADEButton = new JButton();	
	private JButton fetchADEsButton = new JButton();	
	private JButton removeADEButton = new JButton();
	private JButton generateDeleteScriptsButton = new JButton();
	private JScrollPane adeTableScrollPanel;
	private JTable adeTable;
	private JPanel adeTablebuttonPanel;	
	private TableModel<ADEMetadataRow> adeTableModel = new TableModel<ADEMetadataRow>(ADEMetadataRow.getColumnNames());
	
	private final EventDispatcher eventDispatcher = ObjectRegistry.getInstance().getEventDispatcher();
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();
	private final DatabaseController databaseController = ObjectRegistry.getInstance().getDatabaseController();
	private final Logger LOG = Logger.getInstance();
	
	private ADERegistrationController adeRegistor;	
	
	public ADERegistryPanel(ADEManagerPanel parentPanel, ConfigImpl config) {
		this.parentPanel = parentPanel;
		this.config = config;
		this.adeRegistor = new ADERegistrationController(config);
		eventDispatcher.addEventHandler(org.citydb.plugins.ade_manager.event.EventType.SCRIPT_CREATION_EVENT, this);
		
		initGui();
	}
	
	private void initGui() {		
		component = new JPanel();
		component.setLayout(new GridBagLayout());
		
		int BORDER_THICKNESS = ADEManagerPanel.BORDER_THICKNESS;
		int BUTTON_WIDTH = ADEManagerPanel.BUTTON_WIDTH;
		
		int standardButtonHeight = parentPanel.getStandardButtonHeight();
		fetchADEsButton.setPreferredSize(new Dimension(BUTTON_WIDTH, standardButtonHeight));
		removeADEButton.setPreferredSize(new Dimension(BUTTON_WIDTH, standardButtonHeight));
		registerADEButton.setPreferredSize(new Dimension(BUTTON_WIDTH, standardButtonHeight));
		generateDeleteScriptsButton.setPreferredSize(new Dimension(BUTTON_WIDTH, standardButtonHeight));
		
		// ADE table panel
		adeTable = new JTable(adeTableModel);
		adeTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		adeTable.setCellSelectionEnabled(false);
		adeTable.setColumnSelectionAllowed(false);
		adeTable.setRowSelectionAllowed(true);
		adeTable.setRowHeight(20);		
		adeTableScrollPanel = new JScrollPane(adeTable);
		adeTableScrollPanel.setPreferredSize(new Dimension(adeTable.getPreferredSize().width, 150));
		adeTableScrollPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(0, 0, 4, 4)));
		
		adeTablebuttonPanel = new JPanel();
		adeTablebuttonPanel.setLayout(new GridBagLayout());
		adeTablebuttonPanel.add(fetchADEsButton, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		adeTablebuttonPanel.add(removeADEButton, GuiUtil.setConstraints(1,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
				
		browseRegistryPanel = new JPanel();
		browseRegistryPanel.setLayout(new GridBagLayout());
		browseRegistryPanel.setBorder(BorderFactory.createTitledBorder(""));
		browseRegistryPanel.add(browseRegistryText, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		browseRegistryPanel.add(browseRegistryButton, GuiUtil.setConstraints(1,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));	
		
		adeButtonsPanel = new JPanel();
		adeButtonsPanel.setLayout(new GridBagLayout());
		adeButtonsPanel.add(registerADEButton, GuiUtil.setConstraints(1,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		adeButtonsPanel.add(generateDeleteScriptsButton, GuiUtil.setConstraints(3,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));		
	
		int index = 0;
		component.add(adeTableScrollPanel, GuiUtil.setConstraints(0,index++,1.0,0.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));		
		component.add(adeTablebuttonPanel, GuiUtil.setConstraints(0,index++,1.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		component.add(browseRegistryPanel, GuiUtil.setConstraints(0,index++,1.0,0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));		
		component.add(adeButtonsPanel, GuiUtil.setConstraints(0,index++,1.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
			
		fetchADEsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						showRegisteredADEs();						
					}
				};
				thread.setDaemon(true);
				thread.start();
			}
		});
		
		registerADEButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						registerADE();
					}
				};
				thread.setDaemon(true);
				thread.start();
			}
		});
		
		removeADEButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						deregisterADE();
					}
				};
				thread.setDaemon(true);
				thread.start();
			}
		});
		
		generateDeleteScriptsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						generateDeleteScripts();
					}
				};
				thread.setDaemon(true);
				thread.start();
			}
		});
		
		browseRegistryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				browserRegistryInputDirectory();
			}
		});
	}
	
	@Override
	public String getLocalizedTitle() {
		return "Registry";
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
		((TitledBorder) browseRegistryPanel.getBorder()).setTitle("Input for ADE Registry");
		browseRegistryButton.setText(Language.I18N.getString("common.button.browse"));
		
		registerADEButton.setText("Register ADE into DB");
		fetchADEsButton.setText("Fetch ADEs from DB");
		removeADEButton.setText("Remove seleted ADE from DB");
		generateDeleteScriptsButton.setText("Generate Delete Scripts");
	}

	@Override
	public void setEnabled(boolean enable) {
		// 
		
	}

	@Override
	public void loadSettings() {
		browseRegistryText.setText(config.getAdeRegistryInputPath());		
	}

	@Override
	public void setSettings() {
		config.setAdeRegistryInputPath(browseRegistryText.getText()); 
		
	}
	
	private void browserRegistryInputDirectory() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Input Folder");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setCurrentDirectory(new File(browseRegistryText.getText()).getParentFile());
	
		int result = chooser.showOpenDialog(parentPanel.getTopLevelAncestor());
		if (result == JFileChooser.CANCEL_OPTION)
			return;
	
		String browseString = chooser.getSelectedFile().toString();
		if (!browseString.isEmpty())
			browseRegistryText.setText(browseString);
	}
	
	private void registerADE() {
		setSettings();
		
		// database connection is required
		try {
			checkAndConnectToDB();
		} catch (SQLException e) {
			parentPanel.printErrorMessage("ADE registration aborted", e);
			return;
		}
		
		boolean isComplete = false;
		try {	
			adeRegistor.initDBConneciton();
			isComplete = adeRegistor.registerADE();
			adeRegistor.commitTransactions();
		} catch (ADERegistrationException e) {
			adeRegistor.rollbackTransactions();
			parentPanel.printErrorMessage("ADE registration aborted", e);
		} finally {
			adeRegistor.closeDBConnection();
		}
		
		if (isComplete) {
			// database re-connection is required for completing the ADE registration process
			LOG.info("ADE registration is completed and will take effect after reconnecting to the database.");	
			if (dbPool.isConnected()) {
				dbPool.disconnect();
				try {
					databaseController.connect(true);
				} catch (DatabaseConfigurationException | DatabaseVersionException | SQLException e) {
					parentPanel.printErrorMessage("Failed to reconnect to the database", e);
				}
			}	
			// update the ADE list table by querying the ADE again
			showRegisteredADEs();
		}
	
	}
	
	private void showRegisteredADEs() {
		setSettings();	
		
		// database connection is required
		try {
			checkAndConnectToDB();
		} catch (SQLException e) {
			parentPanel.printErrorMessage("Querying ADEs aborted", e);
			return;
		}
		
		try {	
			adeRegistor.initDBConneciton();
			List<ADEMetadataInfo> adeList = adeRegistor.queryRegisteredADEs();
			if (adeList.size() == 0) 
				LOG.info("Status: No ADEs are registered in the connected database");
			
			adeTableModel.reset();			
			for (ADEMetadataInfo adeEntity: adeList) {
				if (adeEntity == null) 
					continue; 				
				adeTableModel.addNewRow(new ADEMetadataRow(adeEntity));
			}
		} catch (ADERegistrationException e) {
			parentPanel.printErrorMessage(e);
		} finally {
			adeRegistor.closeDBConnection();
		}
	}
	
	private void deregisterADE(){
		setSettings();	
		
		// database connection is required
		try {
			checkAndConnectToDB();
		} catch (SQLException e) {
			parentPanel.printErrorMessage("ADE Deregistration aborted", e);
			return;
		}
		
		int selectedRowNum = adeTable.getSelectedRow();
		if (selectedRowNum == -1) {
			parentPanel.getViewController().errorMessage("ADE Deregistration aborted", "Please select one of the listed ADEs");
			return;
		}
		
		boolean isComplete = false;
		String adeId = adeTableModel.getColumn(selectedRowNum).getValue(0);	
		try {
			adeRegistor.initDBConneciton();
			isComplete = adeRegistor.deregisterADE(adeId);
			adeRegistor.commitTransactions();
		} catch (ADERegistrationException e) {
			adeRegistor.rollbackTransactions();
			parentPanel.printErrorMessage("ADE Deregistration aborted", e);
		} finally {
			adeRegistor.closeDBConnection();
		}	
		
		if (isComplete) {
			// database re-connection is required for completing the ADE de-registration process
			LOG.info("ADE Deregistration is completed and will take effect after reconnecting to the database.");
			if (dbPool.isConnected()) {
				dbPool.disconnect();
				try {
					databaseController.connect(true);
				} catch (DatabaseConfigurationException | DatabaseVersionException | SQLException e) {
					parentPanel.printErrorMessage("Failed to reconnect to the database", e);
				}
			}
			// update the ADE list table by querying the ADE again
			showRegisteredADEs();
		}		
	}
	
	private void generateDeleteScripts() {
		// database connection is required
		try {
			checkAndConnectToDB();
		} catch (SQLException e) {
			parentPanel.printErrorMessage("Delete-script creation aborted", e);
			return;
		}
		
		try {			
			adeRegistor.initDBConneciton();
			boolean autoInstall = false;
			adeRegistor.createDeleteScripts(autoInstall);
		} catch (ADERegistrationException e) {
			parentPanel.printErrorMessage("Delete-script creation aborted", e);
		} finally {
			adeRegistor.closeDBConnection();			
		}	
	}
	
	private void checkAndConnectToDB() throws SQLException {
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

	@Override
	public void handleEvent(Event event) throws Exception {
		if (event.getEventType() == org.citydb.plugins.ade_manager.event.EventType.SCRIPT_CREATION_EVENT) {
            ScriptCreationEvent scriptCreationEvent = (ScriptCreationEvent) event;
            String script = scriptCreationEvent.getScript();
            boolean autoInstall = scriptCreationEvent.isAutoInstall();
            
            final ScriptDialog scriptDialog = new ScriptDialog(parentPanel.getViewController().getTopFrame(), script, autoInstall);			
    		scriptDialog.getButton().addActionListener(new ActionListener() {
    			public void actionPerformed(ActionEvent e) {
    				SwingUtilities.invokeLater(new Runnable() {
    					public void run() {
    						try {
    							adeRegistor.initDBConneciton();
    							adeRegistor.installDeleteScript(scriptDialog.getScript());
    							adeRegistor.commitTransactions();
    						} catch (ADERegistrationException e) {
    							adeRegistor.rollbackTransactions();
    							parentPanel.printErrorMessage(e);
    						} finally {
    							adeRegistor.closeDBConnection();
    							scriptDialog.dispose();
    						}  						
    					}
    				});
    			}
    		});

    		SwingUtilities.invokeLater(new Runnable() {
    			public void run() {
    				scriptDialog.setLocationRelativeTo(parentPanel.getTopLevelAncestor());
    				scriptDialog.setVisible(true);
    			}
    		});
        }		
	
	}

}
