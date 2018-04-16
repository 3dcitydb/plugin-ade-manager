
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
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.citydb.config.i18n.Language;
import org.citydb.config.project.database.DatabaseConfigurationException;
import org.citydb.database.DatabaseController;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.version.DatabaseVersionException;
import org.citydb.event.Event;
import org.citydb.event.EventHandler;
import org.citydb.gui.util.GuiUtil;
import org.citydb.log.Logger;
import org.citydb.plugin.extension.view.ViewController;
import org.citydb.plugins.ade_manager.ADEManagerPlugin;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.gui.table.TableModelImpl;
import org.citydb.plugins.ade_manager.gui.table.adeTable.ADERow;
import org.citydb.plugins.ade_manager.gui.table.schemaTable.SchemaRow;
import org.citydb.plugins.ade_manager.registry.ADERegistrationController;
import org.citydb.plugins.ade_manager.registry.ADERegistrationException;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataEntity;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.transformation.TransformationException;
import org.citydb.plugins.ade_manager.transformation.TransformationController;
import org.citydb.registry.ObjectRegistry;

@SuppressWarnings("serial")
public class ADEManagerPanel extends JPanel implements EventHandler {
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();
	private final DatabaseController databaseController = ObjectRegistry.getInstance().getDatabaseController();
	private final Logger LOG = Logger.getInstance();	
	private final ViewController viewController;
	
	// predefined value
	protected static final int BORDER_THICKNESS = 4;
	protected static final int MAX_TEXTFIELD_HEIGHT = 20;
	protected static final int MAX_LABEL_WIDTH = 60;

	private ConfigImpl config;
	
	// Gui variables
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
	
	private JPanel browseSchemaMappingPanel;
	private JTextField browseSchemaMappingText = new JTextField();
	private JButton browseSchemaMappingButton = new JButton();
	
	private JPanel browseCreateDBScriptPanel;
	private JTextField browseCreateDBScriptText = new JTextField();
	private JButton browseCreateDBScriptButton = new JButton();
	
	private JPanel browseDropDBScriptPanel;
	private JTextField browseDropDBScriptText = new JTextField();
	private JButton browseDropDBScriptButton = new JButton();
	
	private JPanel adeButtonsPanel;
	private JButton registerADEButton = new JButton();	
	private JButton fetchADEsButton = new JButton();	
	private JButton removeADEButton = new JButton();
	private JButton generateDeleteScriptsButton = new JButton();
	private JScrollPane adeTableScrollPanel;
	private JTable adeTable;
	private TableModelImpl<ADERow> adeTableModel = new TableModelImpl<ADERow>(ADERow.getColumnNames());

	private TableModelImpl<SchemaRow> schemaTableModel = new TableModelImpl<SchemaRow>(SchemaRow.getColumnNames());	
	
	private final TransformationController adeTransformer;
	private final ADERegistrationController adeRegistor;

	public ADEManagerPanel(ViewController viewController, ADEManagerPlugin plugin) {	
		this.config = plugin.getConfig();		
		this.viewController = viewController;
		this.adeRegistor = new ADERegistrationController(config);
		this.adeTransformer = new TransformationController(config);
		
		initGui();
		addListeners();	
		setEnabledMetadataSettings(false);
	}

	private void initGui() {	
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

		browseSchemaMappingPanel = new JPanel();
		browseSchemaMappingPanel.setLayout(new GridBagLayout());
		browseSchemaMappingPanel.setBorder(BorderFactory.createTitledBorder(""));
		browseSchemaMappingPanel.add(browseSchemaMappingText, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		browseSchemaMappingPanel.add(browseSchemaMappingButton, GuiUtil.setConstraints(1,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		
		browseCreateDBScriptPanel = new JPanel();
		browseCreateDBScriptPanel.setLayout(new GridBagLayout());
		browseCreateDBScriptPanel.setBorder(BorderFactory.createTitledBorder(""));
		browseCreateDBScriptPanel.add(browseCreateDBScriptText, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		browseCreateDBScriptPanel.add(browseCreateDBScriptButton, GuiUtil.setConstraints(1,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		
		browseDropDBScriptPanel = new JPanel();
		browseDropDBScriptPanel.setLayout(new GridBagLayout());
		browseDropDBScriptPanel.setBorder(BorderFactory.createTitledBorder(""));
		browseDropDBScriptPanel.add(browseDropDBScriptText, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		browseDropDBScriptPanel.add(browseDropDBScriptButton, GuiUtil.setConstraints(1,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		
		adeButtonsPanel = new JPanel();
		adeButtonsPanel.setLayout(new GridBagLayout());
		adeButtonsPanel.add(fetchADEsButton, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		adeButtonsPanel.add(registerADEButton, GuiUtil.setConstraints(1,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
		adeButtonsPanel.add(removeADEButton, GuiUtil.setConstraints(2,0,0.0,0.0,GridBagConstraints.NONE,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));
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
		mainScrollView.add(browseSchemaMappingPanel, GuiUtil.setConstraints(0,index++,0.0,0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));		
		mainScrollView.add(browseCreateDBScriptPanel, GuiUtil.setConstraints(0,index++,0.0,0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));	
		mainScrollView.add(browseDropDBScriptPanel, GuiUtil.setConstraints(0,index++,0.0,0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS,BORDER_THICKNESS));	
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

		((TitledBorder) browseSchemaMappingPanel.getBorder()).setTitle("Schema Mapping File (.xml)");
		browseSchemaMappingButton.setText(Language.I18N.getString("common.button.browse"));
		
		((TitledBorder) browseCreateDBScriptPanel.getBorder()).setTitle("CREATE_DB Script (.sql)");
		browseCreateDBScriptButton.setText(Language.I18N.getString("common.button.browse"));
		
		((TitledBorder) browseDropDBScriptPanel.getBorder()).setTitle("DROP_DB Script (.sql)");
		browseDropDBScriptButton.setText(Language.I18N.getString("common.button.browse"));
		
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
		browseSchemaMappingText.setText(config.getSchemaMappingPath());
		browseCreateDBScriptText.setText(config.getCreateDbScriptPath());
		browseDropDBScriptText.setText(config.getDropDbScriptPath());
	}

	public void setSettings() {
		config.setXMLschemaInputPath(browseXMLSchemaText.getText());
		config.setTransformationOutputPath(browseOutputText.getText());
		config.setAdeName(nameInputField.getText());
		config.setAdeDescription(descriptionInputField.getText());
		config.setAdeVersion(versionInputField.getText());
		config.setAdeDbPrefix(dbPrefixInputField.getText());
		config.setInitialObjectclassId(Integer.valueOf(initObjectClassIdInputField.getText()));
		config.setSchemaMappingPath(browseSchemaMappingText.getText());
		config.setCreateDbScriptPath(browseCreateDBScriptText.getText());
		config.setDropDbScriptPath(browseDropDBScriptText.getText());
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
		
		browseSchemaMappingButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				browserSchemaMappingFile();
			}
		});
		
		browseCreateDBScriptButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				browserCreateDBScriptFile();
			}
		});
		
		browseDropDBScriptButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				browserDropDBScriptFile();
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
	
	private void browserSchemaMappingFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	
		FileNameExtensionFilter filter = new FileNameExtensionFilter("XML Schema Mapping file (*.xml)", "xml");
		chooser.addChoosableFileFilter(filter);
		chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
		chooser.setFileFilter(filter);
	
		if (!browseSchemaMappingText.getText().trim().isEmpty())
			chooser.setCurrentDirectory(new File(browseSchemaMappingText.getText()));
	
		int result = chooser.showOpenDialog(getTopLevelAncestor());
		if (result == JFileChooser.CANCEL_OPTION)
			return;
	
		browseSchemaMappingText.setText(chooser.getSelectedFile().toString());
	}
	
	private void browserCreateDBScriptFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	
		FileNameExtensionFilter filter = new FileNameExtensionFilter("CREATE_DB script (*.sql)", "sql");
		chooser.addChoosableFileFilter(filter);
		chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
		chooser.setFileFilter(filter);
	
		if (!browseCreateDBScriptText.getText().trim().isEmpty())
			chooser.setCurrentDirectory(new File(browseCreateDBScriptText.getText()));
	
		int result = chooser.showOpenDialog(getTopLevelAncestor());
		if (result == JFileChooser.CANCEL_OPTION)
			return;
	
		browseCreateDBScriptText.setText(chooser.getSelectedFile().toString());
	}
		
	private void browserDropDBScriptFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	
		FileNameExtensionFilter filter = new FileNameExtensionFilter("DROP_DB script (*.sql)", "sql");
		chooser.addChoosableFileFilter(filter);
		chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
		chooser.setFileFilter(filter);
	
		if (!browseDropDBScriptText.getText().trim().isEmpty())
			chooser.setCurrentDirectory(new File(browseDropDBScriptText.getText()));
	
		int result = chooser.showOpenDialog(getTopLevelAncestor());
		if (result == JFileChooser.CANCEL_OPTION)
			return;
	
		browseDropDBScriptText.setText(chooser.getSelectedFile().toString());
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
				SchemaRow schemaColumn = new SchemaRow(schemaNamespace);
				schemaTableModel.addNewRow(schemaColumn);
			}			
			LOG.info("Parsing ADE XML schema completed.");
			
			setEnabledMetadataSettings(true);
		} catch (TransformationException e) {
			printErrorMessage("Failed to read and parse ADE schema", e);
		}
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
				
		try {
			checkAndConnectToDB();
		} catch (SQLException e) {
			printErrorMessage(e);
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
			isComplete = adeRegistor.registerADE();
		} catch (ADERegistrationException e) {
			adeRegistor.rollbackTransactions();
			printErrorMessage("ADE registration aborted", e);
		} finally {
			adeRegistor.closeDBConnection();
		}
		
		// update the ADE list table by querying the ADE again
		if (isComplete)
			showRegisteredADEs();
		
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
			List<ADEMetadataEntity> adeList = adeRegistor.queryRegisteredADEs();
			if (adeList.size() == 0) 
				viewController.warnMessage("Info", "No ADEs are registered in the connected database");
			
			adeTableModel.reset();			
			for (ADEMetadataEntity adeEntity: adeList) {
				if (adeEntity == null) 
					continue; 				
				adeTableModel.addNewRow(new ADERow(adeEntity));
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
			isComplete = adeRegistor.deregisterADE(adeId);
		} catch (ADERegistrationException e) {
			adeRegistor.rollbackTransactions();
			printErrorMessage("ADE Deregistration aborted", e);
		} finally {
			adeRegistor.closeDBConnection();
		}	
		
		// update the ADE list table by querying the ADE again
		if (isComplete)
			showRegisteredADEs();
	}
	
	private void generateDeleteScripts() {
		// TODO
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

	public void handleEvent(Event event) throws Exception {}

}
