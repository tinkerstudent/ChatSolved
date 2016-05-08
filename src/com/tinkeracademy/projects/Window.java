package com.tinkeracademy.projects;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;

public class Window {

	public static JFrame frame;
	
	public static void show() {
		frame = new JFrame("Tinker Academy");
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		frame.setPreferredSize(new Dimension(800, 600));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
        frame.pack();
        frame.setVisible(true);
	}
	
	public static JLabel addLabel(String labelText) {
		JLabel label = new JLabel(labelText);
		label.setOpaque(true);
		label.setBackground(Color.ORANGE);
        frame.getContentPane().add(label);
        return label;
	}
	
	public static JTextArea addTextArea(String text, int rows, int columns, boolean editable) {
		JTextArea textArea = new JTextArea(text, rows, columns);
		Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 20);
		textArea.setFont(font);
		textArea.setEditable(editable);
		textArea.setLineWrap(true);
		JScrollPane scrollPane = new JScrollPane( textArea );
		Border border = BorderFactory.createLineBorder(Color.ORANGE, 5);
		textArea.setBorder(border);
		frame.getContentPane().add(scrollPane);
		return textArea;
	}
	
	public static JButton addButton(String buttonName) {
		return addButton(buttonName, frame.getContentPane());
	}
	
	public static JButton addButton(String buttonName, Container panel) {
		JButton button = new JButton(buttonName);
		panel.add(button);
		return button;
	}
	
	public static JButton[][] addGridButtons(String[][] buttonNames) {
		JButton[][] buttons = new JButton[buttonNames.length][buttonNames[0].length];
		int rows = buttonNames.length;
		int cols = buttonNames[0].length;
		JPanel gridPanel = new JPanel();
		GridLayout layout = new GridLayout(rows, cols);
		gridPanel.setLayout(layout);
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				JButton button = addButton(buttonNames[i][j], gridPanel);
				buttons[i][j] = button;
			}
		}
		frame.getContentPane().add(gridPanel);
		return buttons;
	}
	
}