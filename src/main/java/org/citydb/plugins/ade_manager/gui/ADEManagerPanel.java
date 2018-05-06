
package org.citydb.plugins.ade_manager.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.citydb.event.Event;
import org.citydb.event.EventHandler;
import org.citydb.event.global.DatabaseConnectionStateEvent;
import org.citydb.gui.util.GuiUtil;
import org.citydb.plugin.extension.view.ViewController;
import org.citydb.plugins.ade_manager.ADEManagerPlugin;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.gui.modules.ADEDeletePanel;
import org.citydb.plugins.ade_manager.gui.modules.ADERegistryPanel;
import org.citydb.plugins.ade_manager.gui.modules.ADETransformationPanel;
import org.citydb.plugins.ade_manager.gui.modules.OperationModuleView;

@SuppressWarnings("serial")
public class ADEManagerPanel extends JPanel implements EventHandler {	
	private final int BORDER_THICKNESS = 5;

	private JTabbedPane subModuleTab;
	private OperationModuleView[] subModulePanels;
	private ViewController viewController;	

	private ConfigImpl config;
	
	public ADEManagerPanel(ViewController viewController, ADEManagerPlugin plugin) {	
		this.config = plugin.getConfig();		
		this.viewController = viewController;
		initGui();
	}

	public ViewController getViewController() {
		return viewController;
	}

	private void initGui() {	
		OperationModuleView transformationPanel = new ADETransformationPanel(this, config);
		OperationModuleView deletePanel = new ADEDeletePanel(this, config);
		OperationModuleView registryPanel = new ADERegistryPanel(this, config);
		
		subModulePanels = new OperationModuleView[3];
		subModulePanels[0] = transformationPanel;	
		subModulePanels[1] = deletePanel;		
		subModulePanels[2] = registryPanel;

		subModuleTab = new JTabbedPane();
		for (int i = 0; i < subModulePanels.length - 1; ++i)
			subModuleTab.insertTab(null, subModulePanels[i].getIcon(), null, subModulePanels[i].getToolTip(), i);

		subModuleTab.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {			
				int index = subModuleTab.getSelectedIndex();
				for (int i = 0; i < subModuleTab.getTabCount(); ++i)
					subModuleTab.setComponentAt(i, index == i ? subModulePanels[index].getViewComponent() : null);
			}
		});

		JPanel mainScrollView = new JPanel();
		mainScrollView.setLayout(new GridBagLayout());
		
		int index = 0;		
		mainScrollView.add(registryPanel.getViewComponent(), GuiUtil.setConstraints(0,index++,1.0,0.0,GridBagConstraints.BOTH,0,0,0,0));		
		mainScrollView.add(subModuleTab, GuiUtil.setConstraints(0,index++,1.0,0.0,GridBagConstraints.BOTH,BORDER_THICKNESS,0,0,0));		
		mainScrollView.add(Box.createVerticalGlue(), GuiUtil.setConstraints(0,index++,1.0,1.0,GridBagConstraints.BOTH,0,0,0,0));

		JScrollPane mainScrollPanel = new JScrollPane(mainScrollView);
		mainScrollPanel.setBorder(BorderFactory.createEmptyBorder());
		mainScrollPanel.setViewportBorder(BorderFactory.createEmptyBorder());
		this.setLayout(new GridBagLayout());	
		this.add(mainScrollPanel, GuiUtil.setConstraints(0,1,1.0,1.0,GridBagConstraints.BOTH,5,0,0,0));
	}

	// localized Labels and Strings
	public void doTranslation() {	
		for (int i = 0; i < subModulePanels.length; ++i) {
			subModulePanels[i].doTranslation();;			
		}
		for (int i = 0; i < subModuleTab.getTabCount(); ++i) {
			subModuleTab.setTitleAt(i, subModulePanels[i].getLocalizedTitle());			
		}
		
	}

	public void loadSettings() {
		int index = 0;
		for (int i = 0; i < subModulePanels.length; ++i) {
			subModulePanels[i].loadSettings();			
		}

		subModuleTab.setSelectedIndex(-1);
		subModuleTab.setSelectedIndex(index);
	}

	public void setSettings() {
		for (int i = 0; i < subModulePanels.length; ++i)
			subModulePanels[i].setSettings();
	}

	public void handleEvent(Event event) throws Exception {
		DatabaseConnectionStateEvent state = (DatabaseConnectionStateEvent)event;
		for (int i = 0; i < subModulePanels.length; ++i)
			subModulePanels[i].handleDatabaseConnectionStateEvent(state);
	}

}
