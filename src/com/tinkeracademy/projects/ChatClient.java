package com.tinkeracademy.projects;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
	
	public ChatStatus initStatus = ChatStatus.NO;
	
	public ChatClient() {
		chatService = new ChatService();
	}
	
	public static void main(String[] args) {
		chat = new ChatClient();
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				chat.createAndShowGUI();
			}
		});
	}
	
	public void createAndShowGUI() {
		Window.show();
		Window.addLabel("Chat Application Developed By: Tinker Academy v1.0");
		chatHistory = Window.addTextArea("", 16, 10, false);
		chatMessage = Window.addTextArea("", 4, 10, true);
		sendButton = Window.addButton("Send");
		sendButton.addActionListener(this);
		initStatus = chatService.initializeChatUserFile();
		if (initStatus == ChatStatus.ERROR) {
			showError();
		} else if (initStatus == ChatStatus.NO) {
			promptChatUser();
		} else if (initStatus == ChatStatus.YES) {
			initChatHistory();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (initStatus == ChatStatus.NO) {
			initializeChatUser();
		} else if (initStatus == ChatStatus.YES) {
			postChatMessage();
		} else if (initStatus == ChatStatus.ERROR) {
			showError();
		}
	}
	
	public void showError() {
		chatMessage.setText(getErrorText());
	}
	
	public void initializeChatUser() {
		String chatName = chatMessage.getText();
		if (chat != null) {
			chatName = chatName.trim();
		}
		if (chatName.length() > 0) {
			ChatStatus setNameStatus = chatService.initializeChatUser(chatName);
			if (setNameStatus == ChatStatus.ERROR) {
				showError();
			} else if (setNameStatus == ChatStatus.NO){
				promptChatUser();
			} else if (setNameStatus == ChatStatus.YES) {
				chatMessage.setText(getChatPrompt());
				initStatus = ChatStatus.YES;
			}
		} else {
			chatMessage.setText(getUserNamePrompt());
		}
	}
	
	public void postChatMessage() {
		String msg = chatMessage.getText();
		if (msg != null) {
			msg = msg.trim();
		}
		if (msg.length() > 0) {
			chatService.postChatMessage(msg);
			updateChatHistory();
			clearChatMessage();
		}
		clearChatMessage();
	}
	
	public void initChatHistory() {
		updateChatHistory();
		chatService.startService();
	}
	
	public void updateChatHistory() {
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
