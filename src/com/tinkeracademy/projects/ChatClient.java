package com.tinkeracademy.projects;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JTextArea;

import com.tinkeracademy.projects.ChatService.ChatStatus;

public class ChatClient implements ActionListener {

	public static ChatClient chat;
	
	public static ChatService chatService;
	
	public JButton sendButton;
	
	public JTextArea chatHistory;
	
	public JTextArea chatMessage;
	
	public ChatClient() {
		chatService = new ChatService();
	}
	
	/**
	 * Main method with boiler plate code to create and show gui
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		chat = new ChatClient();
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				chat.createAndShowGUI();
			}
		});
	}
	
	/**
	 * Creates and shows the GUI
	 * 
	 */
	public void createAndShowGUI() {
		Window.show();
		Window.addLabel("Chat Application Developed By: Tinker Academy v1.0");
		chatHistory = Window.addTextArea("", 16, 10, false);
		chatMessage = Window.addTextArea("", 4, 10, true);
		sendButton = Window.addButton("Send");
		sendButton.addActionListener(this);
		// Prompt for the user name the current user name is not registered on this computer,
		// Else update the chat history panel
		ChatStatus initStatus = chatService.initStatus;
		if (initStatus == ChatStatus.ERROR) {
			showError();
		} else if (initStatus == ChatStatus.NO) {
			promptChatUser();
		} else if (initStatus == ChatStatus.YES) {
			showChatHistory();
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e){
		ChatStatus initStatus = chatService.initStatus;
		try {
			// Set the user name if the current user name is not registered on this computer,
			// Else post the chat message to the network and update the chat history window
			if (initStatus == ChatStatus.NO) {
				setChatUser();
			} else if (initStatus == ChatStatus.YES) {
				addChatMessage();
				showChatHistory();
			} else if (initStatus == ChatStatus.ERROR) {
				showError();
			}
		} catch(IOException ex) {
			ex.printStackTrace();
			showError();
		}
	}
	
	/**
	 * Sets the chat user name to the name the user entered on the chat window
	 * 
	 * @throws IOException
	 */
	public void setChatUser() throws IOException {
		String userName = chatMessage.getText();
		if (chat != null) {
			userName = userName.trim();
		}
		if (userName.length() > 0) {
			ChatStatus setNameStatus = chatService.setUser(userName);
			if (setNameStatus == ChatStatus.ERROR) {
				showError();
			} else if (setNameStatus == ChatStatus.NO){
				promptChatUser();
			} else if (setNameStatus == ChatStatus.YES) {
				chatMessage.setText(getChatPrompt());
				chatService.initStatus = ChatStatus.YES;
			}
		} else {
			chatMessage.setText(getUserNamePrompt());
		}
	}
	
	/**
	 * Adds the chat message to the list of chats
	 * 
	 */
	public void addChatMessage() {
		String msg = chatMessage.getText();
		if (msg != null) {
			msg = msg.trim();
		}
		if (msg.length() > 0) {
			chatService.addChatMessage(msg);
		}
		clearChatMessage();
	}
	
	/**
	 * Shows the current list of chat messages in the chat history
	 * 
	 */
	public void showChatHistory() {
		List<String> chatLines = chatService.getChatHistory();
		if (chatLines != null) {
			StringBuffer buf = new StringBuffer();
			for (String str : chatLines) {
				buf.append(str);
				buf.append('\n');
			}
			chatHistory.setText(buf.toString());
		}
	}
	
	public void showError() {
		chatMessage.setText(getErrorText());
	}
	
	public String getErrorText() {
		String txt = "Oops! Error! Call Super.., no wait, Call Bat.., no wait, just Call me!";
		return txt;
	}
	
	public void promptChatUser() {
		chatMessage.setText(getUserNamePrompt());
	}
	
	public void clearChatMessage() {
		chatMessage.setText("");
	}
	
	public String getUserNamePrompt() {
		String txt = "<Enter Your Name>";
		return txt;
	}
	
	public String getChatPrompt() {
		String txt = "<Type in Chat Message>";
		return txt;
	}
	
}
