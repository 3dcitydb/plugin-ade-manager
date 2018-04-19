package org.citydb.plugins.ade_manager.gui.popup;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.citydb.config.i18n.Language;
import org.citydb.gui.factory.PopupMenuDecorator;
import org.citydb.gui.util.GuiUtil;
import org.citydb.log.Logger;

@SuppressWarnings("serial")
public class ScriptDialog extends JDialog {
	private final String scriptString;
	private JButton installbutton;
	private boolean autoInstall;
	private final Logger LOG = Logger.getInstance();
	
	public ScriptDialog(JFrame frame, String title, String scriptString, boolean autoInstall) {
		super(frame, title, true);
		this.scriptString = scriptString;
		this.autoInstall = autoInstall;
		initGUI();
	}

	private void initGUI() {
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		installbutton = new JButton("Install");		
		
		setLayout(new GridBagLayout()); {
			JPanel main = new JPanel();		
			add(main, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,5,5,5,5));
			JTextArea scriptArea = new JTextArea();
			main.setLayout(new GridBagLayout());
			{
				scriptArea.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));				
				scriptArea.setEditable(false);
				scriptArea.setBackground(Color.WHITE);
				scriptArea.setFont(new Font(Font.MONOSPACED, 0, 11));
				scriptArea.setText(scriptString);	
				scriptArea.setCaretPosition(0);					
				PopupMenuDecorator.getInstance().decorate(scriptArea);
				
				JScrollPane scroll = new JScrollPane(scriptArea);
				scroll.setAutoscrolls(true);
				scroll.setBorder(BorderFactory.createEtchedBorder());
				main.add(scroll, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,2,0,0,0));
			}
			installbutton.setMargin(new Insets(installbutton.getMargin().top, 25, installbutton.getMargin().bottom, 25));	
			if (!autoInstall)
				add(installbutton, GuiUtil.setConstraints(0,3,0.0,0.0,GridBagConstraints.NONE,5,5,5,5));
						
			JButton browserOutputButton = new JButton(Language.I18N.getString("common.button.browse"));
			JTextField browseOutputText = new JTextField();
			JPanel OutputPanel = new JPanel();
			OutputPanel.setLayout(new GridBagLayout());
			OutputPanel.setBorder(BorderFactory.createTitledBorder("Save As"));
			OutputPanel.add(browseOutputText, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,5,5,5,5));
			OutputPanel.add(browserOutputButton, GuiUtil.setConstraints(1,0,0.0,0.0,GridBagConstraints.NONE,5,5,5,5));			
			add(OutputPanel, GuiUtil.setConstraints(0,4,1.0,0.0,GridBagConstraints.BOTH,5,5,5,5));	
			
			JButton saveButton = new JButton("Save Script");
			add(saveButton, GuiUtil.setConstraints(0,5,0.0,0.0,GridBagConstraints.NONE,5,5,5,5));
			
			browserOutputButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JFileChooser chooser = new JFileChooser();
					chooser.setDialogTitle("Ouput Folder");
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);		
					
					FileNameExtensionFilter filter = new FileNameExtensionFilter("SQL File (*.sql)", "sql");
					chooser.addChoosableFileFilter(filter);
					chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
					chooser.setFileFilter(filter);		
					
					chooser.setCurrentDirectory(new File(browseOutputText.getText()));
				
					int result = chooser.showSaveDialog(main.getTopLevelAncestor());
					if (result == JFileChooser.CANCEL_OPTION)
						return;
				
					String browseString = chooser.getSelectedFile().toString();
					
					if (browseString.lastIndexOf('.') == -1) {
						browseOutputText.setText(browseString + ".sql");
					}
					else {
						browseOutputText.setText(browseString);
					}
				}
			});
			
			saveButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					BufferedWriter writer = null;
					String filename = browseOutputText.getText();
					try {
						writer = new BufferedWriter(new FileWriter(browseOutputText.getText()));
						writer.write(scriptArea.getText());
					} catch (IOException ioE) {
						LOG.error("Failed to save SQL-script file" + ioE.getMessage());
					} finally {
						if (writer != null) {
							try {
								writer.close();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}	
						dispose();
						LOG.info("SQL-script is successfully saved to the file: " + filename);
					}
				}
			});
		}

		setPreferredSize(new Dimension(900, 900));
		setResizable(true);
		pack();		
	}
	
	public JButton getButton() {
		return this.installbutton;
	}
	
	public String getScript() {
		return this.scriptString;
	}
	
}
