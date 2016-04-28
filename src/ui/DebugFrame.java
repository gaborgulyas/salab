/*
* Structural Anonymity Lab
* ========================
*
* Copyright (c) 2016 Gabor Gulyas
* Licenced under GNU GPLv3 (see licence.txt)
*
* URL:      https://github.com/gaborgulyas/salab
*
* */
package ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;


public class DebugFrame extends JFrame
{
	public JLabel[] info;
	public JProgressBar progressBar;

	// Constructor:
	public DebugFrame()
	{
		setTitle("DebugFrame");
		setSize(300, 200); // default size is 0,0
		setLocation(10, 200); // default is 0,0 (top left corner)

		JPanel mypanel = new JPanel();
		mypanel.setLayout(new BoxLayout(mypanel, BoxLayout.PAGE_AXIS));
		info = new JLabel[8];
		for(int i = 0; i<8; i++)
		{
			info[i] = new JLabel();
			if(i == 0)
				info[i].setFont(new Font("Courier New", Font.BOLD, 14));
			else
				info[i].setFont(new Font("Courier New", Font.PLAIN, 14));
			mypanel.add(info[i]);
		}
		
		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		mypanel.add(progressBar);

		mypanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		getContentPane().add(mypanel, BorderLayout.CENTER);
	
		// Window Listeners
		addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					System.exit(0);
				}
			}
		);
	}
}

