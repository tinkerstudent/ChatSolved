package com.tinkeracademy.projects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class ChatService implements Runnable {

	public ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	public ScheduledFuture<?> futureTask;
	
	public File chatUserFile;
	
	public File chatFile;
	
	public String chatUser;
	
	public ChatStatus initStatus = ChatStatus.NO;
	
	public List<String> pendingChatPosts = new ArrayList<String>();
	
	public enum ChatStatus {
		YES,
		NO,
		ERROR
	}
	
	public ChatService() {
		init();
	}
	
	public void stopService() {
		futureTask.cancel(false);
	}
	
	public void run() {
		// https://hc.apache.org/downloads.cgi
		// grab the latest file
//		updateChatFile();
	}
	
	public List<String> getChatHistory() {
		List<String> chatHistory = new ArrayList<String>();
		List<String> localContents = readChatFileContents();
		Map<Long, String> localChatContents = decodeChatContents(localContents);
		for (Map.Entry<Long, String> entry : localChatContents.entrySet()) {
			chatHistory.add(entry.getValue());
		}
		return chatHistory;
	}
	
	public ChatStatus addChatMessage(String msg) {
		CloseableHttpClient client = null;
		CloseableHttpResponse response = null;
		try {
			long ms = System.currentTimeMillis();
			String chatLine = ms + "=" + chatUser + ": " + msg;
			ChatStatus chatStatus = appendChatFileLine(chatLine);
			if (chatStatus == ChatStatus.YES) {
				addToPendingChats(chatLine);
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
				if (client != null) {
					client.close();
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		return ChatStatus.ERROR;
	}
	
	public ChatStatus setUser(String userName) throws IOException {
		ChatStatus chatStatus =  updateChatUserFileLine(userName);
		if (chatStatus == ChatStatus.YES) {
			chatUser = userName;
		}
		return chatStatus;
	}	
	
	//=========================================================================
	//
	// 						PRIVATE METHODS
	//
	//=========================================================================
	
	private String getChatUser() {
		BufferedReader br = null;
		String content = null;
		try {		
			br = new BufferedReader(new FileReader(chatUserFile));
			content = br.readLine();
		} catch(IOException e) {
			System.out.println("Oops:"+e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					System.out.println("Oops:"+e);
				}
			}
		}
		return content;
	}
	
	/**
	 * Appends a chat line to the chat file
	 * 
	 * @param line
	 * @return
	 */
	private synchronized ChatStatus appendChatFileLine(String line) throws IOException {
		return updateChatFile(line, new FileWriter(chatFile, true));
	}
	
	/**
	 * @param line
	 * @return
	 */
	private ChatStatus updateChatUserFileLine(String line) throws IOException {
		return updateChatFile(line, new FileWriter(chatUserFile, true));
	}
	
	private synchronized List<String> readChatFileContents() {
		BufferedReader br = null;
		List<String> chatLines = null;
		try {		
			br = new BufferedReader(new FileReader(chatFile));
			chatLines = IOUtils.readLines(br);
			
		} catch(IOException e) {
			System.out.println("Oops:"+e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					System.out.println("Oops:"+e);
				}
			}
		}
		return chatLines;
	}
	
	private synchronized ChatStatus writeChatFileLines(List<String> lines) throws IOException {
		FileWriter fw = new FileWriter(chatFile, false);
		for (String line : lines) {
			ChatStatus chatStatus = updateChatFile(line, fw);
			if (chatStatus != ChatStatus.YES) {
				return chatStatus;
			}
		}
		return ChatStatus.YES;
	}
	
	private synchronized String getPendingChats() {
		StringBuilder jsonBuilder = new StringBuilder();
		jsonBuilder.append("{");
		jsonBuilder.append(doubleQuoteText("chat"));
		jsonBuilder.append("[");
		for (String pendingChatPost : pendingChatPosts) {
			jsonBuilder.append(doubleQuoteText(pendingChatPost));
		}
		jsonBuilder.append("]");
		jsonBuilder.append("}");
		String pendingChatStr = jsonBuilder.toString();
		return pendingChatStr;
	}
	
	private synchronized void clearPendingChats() {
		pendingChatPosts.clear();
	}
	
	private void updateChatFile() {
		CloseableHttpClient client = null;
		CloseableHttpResponse response = null;
		try {
			HttpPost post = new HttpPost("http://tinkerstudent.appspot.com/<someurl>");
			client = HttpClients.createDefault();
			String pendingChats = getPendingChats();
			StringEntity stringEntity = new StringEntity(pendingChats);
			post.setEntity(stringEntity);
			response = client.execute(post);
			InputStream inputStream = response.getEntity().getContent();
			List<String> contents = IOUtils.readLines(inputStream);
			mergeChatFile(contents);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				clearPendingChats();
				if (response != null) {
					response.close();
				}
				if (client != null) {
					client.close();
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private Map<Long, String> decodeChatContents(List<String> contents) {
		Map<Long, String> decodedChat = new TreeMap<Long, String>();
		for (String content : contents) {
			String[] lineContents = content.split("=");
			long timeInMs = Long.parseLong(lineContents[0]);
			decodedChat.put(timeInMs, lineContents[1]);
		}
		return decodedChat;
	}
	
	private void mergeChatFile(List<String> remoteContents) throws IOException {
		Map<Long, String> remoteChatContents = decodeChatContents(remoteContents);
		List<String> localContents = readChatFileContents();
		Map<Long, String> localChatContents = decodeChatContents(localContents);
		localChatContents.putAll(remoteChatContents);
		List<String> mergedChatContents = new ArrayList<String>();
		for (Map.Entry<Long, String> entry : localChatContents.entrySet()) {
			mergedChatContents.add(entry.getKey() + "=" + entry.getValue());
		}
		writeChatFileLines(mergedChatContents);
	}
	
	private synchronized void addToPendingChats(String chatLine) {
		pendingChatPosts.add(chatLine);
	}
	
	private synchronized void init() {
		initializeChatFile();
		initializeChatUserFile();
		futureTask = scheduler.scheduleAtFixedRate(this, 1, 5, TimeUnit.SECONDS);
	}
	
	private ChatStatus initializeChatFile() {
		String userHomeDirectory = System.getProperty("user.home");
		try {		
			chatFile = new File(userHomeDirectory, "TinkerAcademyChat.tff");
			if (!chatFile.exists()) {
				chatFile.createNewFile();
			}
			return ChatStatus.YES;
		} catch(IOException e) {
			System.out.println("Oops:"+e);
			return ChatStatus.ERROR;
		}
	}
	
	private ChatStatus initializeChatUserFile() {
		String userHomeDirectory = System.getProperty("user.home");
		try {		
			chatUserFile = new File(userHomeDirectory, "TinkerAcademyChatUser.tff");
			if (!chatUserFile.exists()) {
				chatUserFile.createNewFile();
				return ChatStatus.NO;
			} else if (chatUserFile.length() == 0) {
				return ChatStatus.NO;
			} else {
				chatUser = getChatUser();
			}
			return ChatStatus.YES;
		} catch(IOException e) {
			System.out.println("Oops:"+e);
			return ChatStatus.ERROR;
		}
	}
	
	private synchronized ChatStatus updateChatFile(String line, FileWriter fw) throws IOException {
		PrintWriter pw = null;
		try {		
			pw = new PrintWriter(fw);
			pw.println(line);
			pw.flush();
			return ChatStatus.YES;
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}
	
	private String doubleQuoteText(String str) {
		return "\"" + str + "\"";
	}
	
}
