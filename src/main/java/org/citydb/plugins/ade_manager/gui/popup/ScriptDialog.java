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
package org.citydb.plugins.ade_manager.gui.popup;

import org.citydb.config.i18n.Language;
import org.citydb.gui.factory.PopupMenuDecorator;
import org.citydb.gui.factory.RSyntaxTextAreaHelper;
import org.citydb.gui.util.GuiUtil;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;
import org.citydb.plugins.ade_manager.util.Translator;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ScriptDialog extends JDialog {
	private final Logger log = Logger.getInstance();
	private final DBSQLScript script;
	private final boolean autoInstall;
	private JButton installButton;

	public ScriptDialog(JFrame frame, DBSQLScript script, boolean autoInstall) {
		super(frame, true);
		this.script = script;
		this.autoInstall = autoInstall;
		initGUI();
	}

	private void initGUI() {
		if (autoInstall)
			this.setTitle(Translator.I18N.getString("ade_manager.scriptDialog.title.installScript"));
		else
			this.setTitle(Translator.I18N.getString("ade_manager.scriptDialog.title.generateScript"));

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		installButton = new JButton(Translator.I18N.getString("ade_manager.scriptDialog.button.install"));

		setLayout(new GridBagLayout());
		{
			JPanel main = new JPanel();
			RSyntaxTextArea scriptArea = new RSyntaxTextArea();

			main.setLayout(new GridBagLayout());
			{
				scriptArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
				scriptArea.setAutoIndentEnabled(true);
				scriptArea.setHighlightCurrentLine(true);
				scriptArea.setTabSize(2);
				scriptArea.setText(script.toString());

				RTextScrollPane scroll = new RTextScrollPane(scriptArea);
				main.add(scroll, GuiUtil.setConstraints(0, 0, 1, 1, GridBagConstraints.BOTH, 2, 0, 0, 0));
			}

			JLabel saveLabel = new JLabel(Translator.I18N.getString("ade_manager.scriptDialog.outputPanel.label"));
			JButton browserOutputButton = new JButton(Language.I18N.getString("common.button.browse"));
			JTextField browseOutputText = new JTextField();
			JPanel outputPanel = new JPanel();
			outputPanel.setLayout(new GridBagLayout());
			outputPanel.add(saveLabel, GuiUtil.setConstraints(0, 0, 0, 0, GridBagConstraints.NONE, 0, 0, 0, 5));
			outputPanel.add(browseOutputText, GuiUtil.setConstraints(1, 0, 1, 1, GridBagConstraints.BOTH, 0, 5, 0, 5));
			outputPanel.add(browserOutputButton, GuiUtil.setConstraints(2, 0, 0, 0, GridBagConstraints.NONE, 0, 5, 0, 0));

			JButton saveButton = new JButton(Translator.I18N.getString("ade_manager.scriptDialog.outputPanel.button"));
			JButton cancelButton = new JButton(Language.I18N.getString("common.button.ok"));
			Box box = Box.createHorizontalBox();
			box.add(saveButton);
			if (!autoInstall) {
				box.add(Box.createHorizontalStrut(10));
				box.add(installButton);
			}

			JPanel buttonsPanel = new JPanel();
			buttonsPanel.setLayout(new GridBagLayout());
			buttonsPanel.add(box, GuiUtil.setConstraints(0, 0, 2, 1, 1, 0, GridBagConstraints.NONE, 0, 0, 0, 5));
			buttonsPanel.add(cancelButton, GuiUtil.setConstraints(1, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, 0, 5, 0, 0));

			add(main, GuiUtil.setConstraints(0, 0, 1, 1, GridBagConstraints.BOTH, 10, 10, 10, 10));
			add(outputPanel, GuiUtil.setConstraints(0, 1, 1, 0, GridBagConstraints.BOTH, 5, 10, 5, 10));
			add(buttonsPanel, GuiUtil.setConstraints(0, 2, 1, 0, GridBagConstraints.HORIZONTAL, 10, 10, 10, 10));

			RSyntaxTextAreaHelper.installDefaultTheme(scriptArea);
			PopupMenuDecorator.getInstance().decorate(browseOutputText, scriptArea);

			cancelButton.addActionListener(e -> dispose());

			browserOutputButton.addActionListener(e -> {
				JFileChooser chooser = new JFileChooser();
				chooser.setDialogTitle(Translator.I18N.getString("ade_manager.scriptDialog.outputFileChooser.title"));
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
			});
			
			saveButton.addActionListener(e -> {
				BufferedWriter writer = null;
				String filename = browseOutputText.getText();
				try {
					writer = new BufferedWriter(new FileWriter(browseOutputText.getText()));
					writer.write(scriptArea.getText());
				} catch (IOException ioE) {
					log.error("Failed to save SQL-script file" + ioE.getMessage());
				} finally {
					if (writer != null) {
						try {
							writer.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					log.info("SQL-script is successfully saved to the file: " + filename);
				}
			});
		}

		setPreferredSize(new Dimension(800, 800));
		setResizable(true);
		pack();		
	}
	
	public JButton getButton() {
		return installButton;
	}
	
	public DBSQLScript getScript() {
		return script;
	}
	
}
