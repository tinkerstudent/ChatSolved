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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class ChatService implements Runnable {

	public final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	public ScheduledFuture<?> futureTask;
	
	public File chatUserFile;
	
	public File chatFile;
	
	public String chatUser;
	
	public enum ChatStatus {
		YES,
		NO,
		ERROR
	}
	
	public void startService() {
		initializeChatFile();
		futureTask = scheduler.scheduleAtFixedRate(this, 5, 5, TimeUnit.SECONDS);
	}
	
	public void stopService() {
		futureTask.cancel(false);
	}
	
	public void run() {
		// https://hc.apache.org/downloads.cgi
		// grab the latest file
		updateChatFile();
	}
	
	public void updateChatFile() {
		CloseableHttpClient client = null;
		CloseableHttpResponse response = null;
		try {
			HttpGet get = new HttpGet("http://tinkerstudent.appspot.com/<someurl>");
			client = HttpClients.createDefault();
			response = client.execute(get);
			InputStream inputStream = response.getEntity().getContent();
			List<String> contents = IOUtils.readLines(inputStream);
			mergeChatFile(contents);
		} catch (Exception e) {
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
	}
	
	public Map<Long, String> decodeChatContents(List<String> contents) {
		Map<Long, String> decodedChat = new TreeMap<Long, String>();
		for (String content : contents) {
			String[] lineContents = content.split("=");
			long timeInMs = Long.parseLong(lineContents[0]);
			decodedChat.put(timeInMs, lineContents[1]);
		}
		return decodedChat;
	}
	
	public void mergeChatFile(List<String> remoteContents) {
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
	
	public String getChatUser() {
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
	
	public List<String> getChatHistory() {
		List<String> chatHistory = new ArrayList<String>();
		List<String> localContents = readChatFileContents();
		Map<Long, String> localChatContents = decodeChatContents(localContents);
		for (Map.Entry<Long, String> entry : localChatContents.entrySet()) {
			chatHistory.add(entry.getValue());
		}
		return chatHistory;
	}
	
	public ChatStatus initializeChatUserFile() {
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
	
	public ChatStatus initializeChatFile() {
		String userHomeDirectory = System.getProperty("user.home");
		try {		
			chatFile = new File(userHomeDirectory, "TinkerAcademyChat.tff");
			if (!chatFile.exists()) {
				chatUserFile.createNewFile();
			}
			return ChatStatus.YES;
		} catch(IOException e) {
			System.out.println("Oops:"+e);
			return ChatStatus.ERROR;
		}
	}
	
	public ChatStatus initializeChatUser(String userName) {
		ChatStatus chatStatus =  updateChatUserFileLine(userName);
		if (chatStatus == ChatStatus.YES) {
			chatUser = userName;
		}
		return chatStatus;
	}
	
	public ChatStatus postChatMessage(String msg) {
		CloseableHttpClient client = null;
		CloseableHttpResponse response = null;
		try {
			long ms = System.currentTimeMillis();
			String chatLine = ms + "=" + chatUser + ": " + msg;
			ChatStatus chatStatus = appendChatFileLine(chatLine);
			if (chatStatus == ChatStatus.YES) {
				HttpPost post = new HttpPost("http://tinkerstudent.appspot.com/<someurl>");
				StringEntity stringEntity = new StringEntity(chatLine);
				post.setEntity(stringEntity);
				client = HttpClients.createDefault();
				response = client.execute(post);
				return ChatStatus.YES;
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
	
	public ChatStatus updateChatUserFileLine(String line) {
		PrintWriter pw = null;
		try {		
			pw = new PrintWriter(new FileWriter(chatUserFile, true));
			pw.println(line);
			pw.flush();
			return ChatStatus.YES;
		} catch(IOException e) {
			System.out.println("Oops:"+e);
			return ChatStatus.ERROR;
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}
	
	public synchronized List<String> readChatFileContents() {
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
	
	public synchronized ChatStatus writeChatFileLines(List<String> lines) {
		PrintWriter pw = null;
		try {		
			pw = new PrintWriter(new FileWriter(chatFile, false));
			for (String line : lines) {
				pw.println(line);
			}
			pw.flush();
			return ChatStatus.YES;
		} catch(IOException e) {
			System.out.println("Oops:"+e);
			return ChatStatus.ERROR;
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}
	
	public synchronized ChatStatus appendChatFileLine(String line) {
		PrintWriter pw = null;
		try {		
			pw = new PrintWriter(new FileWriter(chatFile, true));
			pw.println(line);
			pw.flush();
			return ChatStatus.YES;
		} catch(IOException e) {
			System.out.println("Oops:"+e);
			return ChatStatus.ERROR;
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}
}
