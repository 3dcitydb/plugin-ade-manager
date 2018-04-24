
package org.citydb.plugins.ade_manager.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.citydb.config.i18n.Language;
import org.citydb.config.project.database.DatabaseConfigurationException;
import org.citydb.database.DatabaseController;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.version.DatabaseVersionException;
import org.citydb.event.Event;
import org.citydb.event.EventDispatcher;
import org.citydb.event.EventHandler;
import org.citydb.gui.util.GuiUtil;
import org.citydb.log.Logger;
import org.citydb.plugin.extension.view.ViewController;
import org.citydb.plugins.ade_manager.ADEManagerPlugin;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.event.ScriptCreationEvent;
import org.citydb.plugins.ade_manager.gui.popup.ScriptDialog;
import org.citydb.plugins.ade_manager.gui.table.ADEMetadataRow;
import org.citydb.plugins.ade_manager.gui.table.ADESchemaNamespaceRow;
import org.citydb.plugins.ade_manager.gui.table.TableModel;
import org.citydb.plugins.ade_manager.registry.ADERegistrationController;
import org.citydb.plugins.ade_manager.registry.ADERegistrationException;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataInfo;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.transformation.TransformationException;
import org.citydb.plugins.ade_manager.transformation.TransformationController;
import org.citydb.registry.ObjectRegistry;

@SuppressWarnings("serial")
public class ADEManagerPanel extends JPanel implements EventHandler {	
	protected static final int BORDER_THICKNESS = 4;
	protected static final int MAX_TEXTFIELD_HEIGHT = 20;
	protected static final int MAX_LABEL_WIDTH = 60;
	protected static final int BUTTON_WIDTH = 180;
	
	private JPanel browseXMLSchemaPanel;
	private JTextField browseXMLSchemaText = new JTextField();
	private JButton browseXMLSchemaButton = new JButton();
	private JButton readXMLSchemaButton = new JButton();	
	private JTable schemaTable;
	private JScrollPane schemaPanel;	
	private JPanel namePanel;
	private JTextField nameInputField = new JTextField();	
	private JPanel descriptionPanel;
	private JTextField descriptionInputField = new JTextField();
	private JPanel versionPanel;
	private JTextField versionInputField = new JTextField();
	private JPanel dbPrefixPanel;
	private JTextField dbPrefixInputField = new JTextField();
	private JPanel initObjectClassIdPanel;
	private JTextField initObjectClassIdInputField = new JTextField();	
	private JPanel transformationOutputPanel;
	private JTextField browseOutputText = new JTextField();
	private JButton browserOutputButton = new JButton();
	private JButton transformAndExportButton = new JButton();
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
	private TableModel<ADESchemaNamespaceRow> schemaTableModel = new TableModel<ADESchemaNamespaceRow>(ADESchemaNamespaceRow.getColumnNames());	
	
	private final EventDispatcher eventDispatcher = ObjectRegistry.getInstance().getEventDispatcher();
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();
	private final DatabaseController databaseController = ObjectRegistry.getInstance().getDatabaseController();
	private final Logger LOG = Logger.getInstance();
	
	private ViewController viewController;	
	private TransformationController adeTransformer;
	private ADERegistrationController adeRegistor;	
	private ConfigImpl config;
	
	public ADEManagerPanel(ViewController viewController, ADEManagerPlugin plugin) {	
		this.config = plugin.getConfig();		
		this.viewController = viewController;
		this.adeRegistor = new ADERegistrationController(config);
		this.adeTransformer = new TransformationController(config);
		eventDispatcher.addEventHandler(org.citydb.plugins.ade_manager.event.EventType.SCRIPT_CREATION_EVENT, this);
		
		initGui();
		addListeners();	
		setEnabledMetadataSettings(false);
	}

	private void initGui() {	
		// adjust buttons size
		int standardButtonHeight = (new JButton("D")).getPreferredSize().height;
		readXMLSchemaButton.setPreferredSize(new Dimension(BUTTON_WIDTH, standardButtonHeight));
		transformAndExportButton.setPreferredSize(new Dimension(BUTTON_WIDTH, standardButtonHeight));
		fetchADEsButton.setPreferredSize(new Dimension(BUTTON_WIDTH, standardButtonHeight));
		removeADEButton.setPreferredSize(new Dimension(BUTTON_WIDTH, standardButtonHeight));
		registerADEButton.setPreferredSize(new Dimension(BUTTON_WIDTH, standardButtonHeight));
		generateDeleteScriptsButton.setPreferredSize(new Dimension(BUTTON_WIDTH, standardButtonHeight));
		
		// Input panel
		browseXMLSchemaPanel = new JPanel();
		browseXMLSchemaPanel.setLayout(new GridBagLayout());
		browseXMLSchemaPanel.add(browseXMLSchemaText, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		browseXMLSchemaPanel.add(browseXMLSchemaButton, GuiUtil.setConstraints(1,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));	
				
		// Schema table panel
		schemaTable = new JTable(schemaTableModel);
		schemaTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		schemaTable.setCellSelectionEnabled(false);
		schemaTable.setColumnSelectionAllowed(false);
		schemaTable.setRowSelectionAllowed(true);
		schemaTable.setRowHeight(25);		
		schemaPanel = new JScrollPane(schemaTable);
		schemaPanel.setPreferredSize(new Dimension(browseXMLSchemaText.getPreferredSize().width, 200));
		schemaPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(0, 0, 0, 0)));
		
		// metadata parameters
		namePanel = new JPanel();
		namePanel.setLayout(new GridBagLayout());
		namePanel.add(nameInputField, GuiUtil.setConstraints(0, 0, 1.0, 1.0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS));
		nameInputField.setColumns(10);

		descriptionPanel = new JPanel();
		descriptionPanel.setLayout(new GridBagLayout());
		descriptionPanel.add(descriptionInputField, GuiUtil.setConstraints(0, 0, 1.0, 1.0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS));
		descriptionInputField.setColumns(10);
		
		versionPanel = new JPanel();
		versionPanel.setLayout(new GridBagLayout());
		versionPanel.add(versionInputField, GuiUtil.setConstraints(0, 0, 1.0, 1.0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS));
		versionInputField.setColumns(10);
		
		dbPrefixPanel = new JPanel();
		dbPrefixPanel.setLayout(new GridBagLayout());
		dbPrefixPanel.add(dbPrefixInputField, GuiUtil.setConstraints(0, 0, 1.0, 1.0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS));
		dbPrefixInputField.setColumns(10);
		
		initObjectClassIdPanel = new JPanel();
		initObjectClassIdPanel.setLayout(new GridBagLayout());
		initObjectClassIdPanel.add(initObjectClassIdInputField, GuiUtil.setConstraints(0, 0, 1.0, 1.0, GridBagConstraints.HORIZONTAL, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS));
		initObjectClassIdInputField.setColumns(10);
		
		Box box = Box.createVerticalBox();	
		box.add(namePanel);
		box.add(descriptionPanel);
		box.add(versionPanel);
		box.add(dbPrefixPanel);
		box.add(initObjectClassIdPanel);
		
		JPanel metadataInputPanel = new JPanel(new BorderLayout());
		metadataInputPanel.add(box, BorderLayout.NORTH);
		
		JPanel schemaAndMetadataPanel = new JPanel();
		schemaAndMetadataPanel.setLayout(new GridBagLayout());
		schemaAndMetadataPanel.add(schemaPanel, GuiUtil.setConstraints(0,0,0.7,0,GridBagConstraints.BOTH,0,0,0,0));
		schemaAndMetadataPanel.add(Box.createRigidArea(new Dimension(BORDER_THICKNESS, 0)), GuiUtil.setConstraints(1,0,0,0,GridBagConstraints.NONE,0,0,0,0));
		schemaAndMetadataPanel.add(metadataInputPanel, GuiUtil.setConstraints(2,0,0.3,0,GridBagConstraints.BOTH,0,0,0,0));
	
		// Export panel
		transformationOutputPanel = new JPanel();
		transformationOutputPanel.setLayout(new GridBagLayout());
		transformationOutputPanel.setBorder(BorderFactory.createTitledBorder(""));
		transformationOutputPanel.add(browseOutputText, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		transformationOutputPanel.add(browserOutputButton, GuiUtil.setConstraints(1,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
				
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
		
		// Assemble all panels
		JPanel mainScrollView = new JPanel();
		mainScrollView.setLayout(new GridBagLayout());
		
		int index = 0;		
		mainScrollView.add(browseXMLSchemaPanel, GuiUtil.setConstraints(0,index++,0.0,0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		mainScrollView.add(readXMLSchemaButton, GuiUtil.setConstraints(0,index++,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS*2,BORDER_THICKNESS));
		mainScrollView.add(schemaAndMetadataPanel, GuiUtil.setConstraints(0,index++,0,0.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));		
		mainScrollView.add(transformationOutputPanel, GuiUtil.setConstraints(0,index++,1.0,0.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		mainScrollView.add(transformAndExportButton, GuiUtil.setConstraints(0,index++,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS*2,BORDER_THICKNESS));
		mainScrollView.add(adeTableScrollPanel, GuiUtil.setConstraints(0,index++,0,0.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));		
		mainScrollView.add(adeTablebuttonPanel, GuiUtil.setConstraints(0,index++,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		mainScrollView.add(browseRegistryPanel, GuiUtil.setConstraints(0,index++,0.0,0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));		
		mainScrollView.add(adeButtonsPanel, GuiUtil.setConstraints(0,index++,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		mainScrollView.add(Box.createVerticalGlue(), GuiUtil.setConstraints(0,index++,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));

		JScrollPane mainScrollPanel = new JScrollPane(mainScrollView);
		mainScrollPanel.setBorder(BorderFactory.createEmptyBorder());
		mainScrollPanel.setViewportBorder(BorderFactory.createEmptyBorder());
		this.setLayout(new GridBagLayout());	
		this.add(mainScrollPanel, GuiUtil.setConstraints(0,1,1.0,1.0,GridBagConstraints.BOTH,5,0,0,0));
	}

	// localized Labels and Strings
	public void doTranslation() {	
		browseXMLSchemaPanel.setBorder(BorderFactory.createTitledBorder("XML Schema (XSD)"));

		namePanel.setBorder(BorderFactory.createTitledBorder("Name"));
		descriptionPanel.setBorder(BorderFactory.createTitledBorder("Description"));
		versionPanel.setBorder(BorderFactory.createTitledBorder("Version"));
		dbPrefixPanel.setBorder(BorderFactory.createTitledBorder("DB_Prefix"));
		initObjectClassIdPanel.setBorder(BorderFactory.createTitledBorder("InitialObjectclassId"));

		browseXMLSchemaButton.setText(Language.I18N.getString("common.button.browse"));
		readXMLSchemaButton.setText("Read XML Schema");
		((TitledBorder) transformationOutputPanel.getBorder()).setTitle("Output");
		browserOutputButton.setText(Language.I18N.getString("common.button.browse"));
		transformAndExportButton.setText("Transform");

		((TitledBorder) browseRegistryPanel.getBorder()).setTitle("Input for ADE Registry");
		browseRegistryButton.setText(Language.I18N.getString("common.button.browse"));
		
		registerADEButton.setText("Register ADE into DB");
		fetchADEsButton.setText("Fetch ADEs from DB");
		removeADEButton.setText("Remove seleted ADE from DB");
		generateDeleteScriptsButton.setText("Generate Delete Scripts");
	}

	public void loadSettings() {
		browseXMLSchemaText.setText(config.getXMLschemaInputPath());
		browseOutputText.setText(config.getTransformationOutputPath());
		nameInputField.setText(config.getAdeName());
		descriptionInputField.setText(config.getAdeDescription());
		versionInputField.setText(config.getAdeVersion());
		dbPrefixInputField.setText(config.getAdeDbPrefix());
		initObjectClassIdInputField.setText(String.valueOf(config.getInitialObjectclassId()));
		browseRegistryText.setText(config.getAdeRegistryInputPath());
	}

	public void setSettings() {
		config.setXMLschemaInputPath(browseXMLSchemaText.getText());
		config.setTransformationOutputPath(browseOutputText.getText());
		config.setAdeName(nameInputField.getText());
		config.setAdeDescription(descriptionInputField.getText());
		config.setAdeVersion(versionInputField.getText());
		config.setAdeDbPrefix(dbPrefixInputField.getText());
		config.setInitialObjectclassId(Integer.valueOf(initObjectClassIdInputField.getText()));
		config.setAdeRegistryInputPath(browseRegistryText.getText());
	}

	private void addListeners() {
		browseXMLSchemaButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				browserXMLschemaFile();
			}
		});
		
		readXMLSchemaButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						parseADESchema();						
					}
				};
				thread.setDaemon(true);
				thread.start();
			}
		});
	
		schemaTable.addMouseListener(new MouseAdapter()
		{
		    public void mouseClicked(MouseEvent e)
		    {
		        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {	
		        	Thread thread = new Thread() {
						public void run() { 							
				        //	dummyFunc();
						}
					};
					thread.setDaemon(true);
					thread.start();
		        }
		    }
		});
		
		browserOutputButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				browseTransformationOutputDirectory();
			}
		});
				
		transformAndExportButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						transformADESchema();						
					}
				};
				thread.setDaemon(true);
				thread.start();
			}
		});
		
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
	
	private void browserXMLschemaFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	
		FileNameExtensionFilter filter = new FileNameExtensionFilter("XML Schema file (*.xsd)", "xsd");
		chooser.addChoosableFileFilter(filter);
		chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
		chooser.setFileFilter(filter);
	
		if (!browseXMLSchemaText.getText().trim().isEmpty())
			chooser.setCurrentDirectory(new File(browseXMLSchemaText.getText()));
	
		int result = chooser.showOpenDialog(getTopLevelAncestor());
		if (result == JFileChooser.CANCEL_OPTION)
			return;
	
		browseXMLSchemaText.setText(chooser.getSelectedFile().toString());
	}
	
	private void setEnabledMetadataSettings(boolean enable) {
		nameInputField.setEnabled(enable);
		descriptionInputField.setEnabled(enable);
		versionInputField.setEnabled(enable);
		dbPrefixInputField.setEnabled(enable);
		initObjectClassIdInputField.setEnabled(enable);
		browseOutputText.setEnabled(enable);
		browserOutputButton.setEnabled(enable);
		transformAndExportButton.setEnabled(enable);
	}

	private void browseTransformationOutputDirectory() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Ouput Folder");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setCurrentDirectory(new File(browseOutputText.getText()));
	
		int result = chooser.showSaveDialog(getTopLevelAncestor());
		if (result == JFileChooser.CANCEL_OPTION)
			return;
	
		String browseString = chooser.getSelectedFile().toString();
		if (!browseString.isEmpty())
			browseOutputText.setText(browseString);
	}

	private void parseADESchema() {
		viewController.clearConsole();
		schemaTableModel.reset();
		
		LOG.info("Start parsing ADE XML schema...");
		String xmlSchemaPath = browseXMLSchemaText.getText();
		try {			
			List<String> adeNamespaces = adeTransformer.getADENamespacesFromXMLSchema(xmlSchemaPath);		
			for (String schemaNamespace : adeNamespaces) {
				ADESchemaNamespaceRow schemaColumn = new ADESchemaNamespaceRow(schemaNamespace);
				schemaTableModel.addNewRow(schemaColumn);
			}			
			LOG.info("Parsing ADE XML schema completed.");
			
			setEnabledMetadataSettings(true);
		} catch (TransformationException e) {
			printErrorMessage("Failed to read and parse ADE schema", e);
		}
	}

	private void browserRegistryInputDirectory() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Input Folder");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setCurrentDirectory(new File(browseRegistryText.getText()).getParentFile());
	
		int result = chooser.showOpenDialog(getTopLevelAncestor());
		if (result == JFileChooser.CANCEL_OPTION)
			return;
	
		String browseString = chooser.getSelectedFile().toString();
		if (!browseString.isEmpty())
			browseRegistryText.setText(browseString);
	}

	private void transformADESchema() {	
		setSettings();
	
		String adeName = config.getAdeName();
		if (adeName.trim().equals("")) {
			viewController.errorMessage("Incomplete Information", "Please enter a name for the ADE");
			return;
		}
		
		String dbPrefix = config.getAdeDbPrefix();
		if (dbPrefix.trim().equals("")) {
			viewController.errorMessage("Incomplete Information", "Please enter a name for the ADE");
			return;
		}
		
		int initialObjectclassId = config.getInitialObjectclassId();
		if (initialObjectclassId < ADEMetadataManager.MIN_ADE_OBJECTCLASSID) {
			viewController.errorMessage("Incorrect Information", "Then initial objectclass ID must be greater than or equal to 10000");
			return;
		}	
		
		int selectedRowNum = schemaTable.getSelectedRow();
		if (selectedRowNum == -1) {
			viewController.errorMessage("Incomplete Information", "Please select a schema namespace");
			return;
		}
		String selectedSchemaNamespace = schemaTableModel.getColumn(selectedRowNum).getValue(0);
	
		try {
			adeTransformer.doProcess(selectedSchemaNamespace);
		} catch (TransformationException e) {
			printErrorMessage(e);
			return;
		}
	
		LOG.info("Transformation finished");
	}

	private void registerADE() {
		setSettings();
		
		// database connection is required
		try {
			checkAndConnectToDB();
		} catch (SQLException e) {
			printErrorMessage("ADE registration aborted", e);
			return;
		}
		
		boolean isComplete = false;
		try {	
			adeRegistor.initDBConneciton();
			isComplete = adeRegistor.registerADE();
			adeRegistor.commitTransactions();
		} catch (ADERegistrationException e) {
			adeRegistor.rollbackTransactions();
			printErrorMessage("ADE registration aborted", e);
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
					printErrorMessage("Failed to reconnect to the database", e);
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
			printErrorMessage("Querying ADEs aborted", e);
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
			printErrorMessage(e);
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
			printErrorMessage("ADE Deregistration aborted", e);
			return;
		}
		
		int selectedRowNum = adeTable.getSelectedRow();
		if (selectedRowNum == -1) {
			viewController.errorMessage("ADE Deregistration aborted", "Please select one of the listed ADEs");
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
			printErrorMessage("ADE Deregistration aborted", e);
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
					printErrorMessage("Failed to reconnect to the database", e);
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
			printErrorMessage("Delete-script creation aborted", e);
			return;
		}
		
		try {			
			adeRegistor.initDBConneciton();
			boolean autoInstall = false;
			adeRegistor.createDeleteScripts(autoInstall);
		} catch (ADERegistrationException e) {
			printErrorMessage("Delete-script creation aborted", e);
		} finally {
			adeRegistor.closeDBConnection();			
		}	
	}
	
	private void printErrorMessage(Exception e) {
		printErrorMessage("Unexpected Error", e);
	}
	
	private void printErrorMessage(String info, Exception e) {
		LOG.error(info + ". Cause: " + e.getMessage());			
		Throwable cause = e.getCause();
		while (cause != null) {
			LOG.error("Cause: " + cause.getMessage());
			cause = cause.getCause();
		}
	}
	
	private void checkAndConnectToDB() throws SQLException {
		String[] connectConfirm = { Language.I18N.getString("pref.kmlexport.connectDialog.line1"),
				Language.I18N.getString("pref.kmlexport.connectDialog.line3") };

		if (!dbPool.isConnected() && JOptionPane.showConfirmDialog(getTopLevelAncestor(), connectConfirm,
				Language.I18N.getString("pref.kmlexport.connectDialog.title"),
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			try {
				databaseController.connect(true);
			} catch (DatabaseConfigurationException | DatabaseVersionException | SQLException e) {
				throw new SQLException("Failed to connect to the target database", e);
			}
		}
	}

	public void handleEvent(Event event) throws Exception {
		if (event.getEventType() == org.citydb.plugins.ade_manager.event.EventType.SCRIPT_CREATION_EVENT) {
            ScriptCreationEvent scriptCreationEvent = (ScriptCreationEvent) event;
            String script = scriptCreationEvent.getScript();
            boolean autoInstall = scriptCreationEvent.isAutoInstall();
            
            final ScriptDialog scriptDialog = new ScriptDialog(viewController.getTopFrame(), script, autoInstall);			
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
    							printErrorMessage(e);
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
    				scriptDialog.setLocationRelativeTo(getTopLevelAncestor());
    				scriptDialog.setVisible(true);
    			}
    		});
        }		
	}

}
