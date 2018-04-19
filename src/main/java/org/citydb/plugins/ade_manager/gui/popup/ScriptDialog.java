package org.citydb.plugins.ade_manager.gui.popup;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.citydb.gui.factory.PopupMenuDecorator;
import org.citydb.gui.util.GuiUtil;

@SuppressWarnings("serial")
public class ScriptDialog extends JDialog {
	private final String scriptString;
	private JButton installbutton;
	public ScriptDialog(JFrame frame, String title, String scriptString) {
		super(frame, title, true);
		this.scriptString = scriptString;
		initGUI();
	}

	private void initGUI() {
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		installbutton = new JButton("Install");		

		setLayout(new GridBagLayout()); {
			JPanel main = new JPanel();
			add(main, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,5,5,5,5));
			main.setLayout(new GridBagLayout());
			{
				JTextArea scriptArea = new JTextArea();
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
			add(installbutton, GuiUtil.setConstraints(0,3,0.0,0.0,GridBagConstraints.NONE,5,5,5,5));
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
