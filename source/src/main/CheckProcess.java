package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

public class CheckProcess implements Runnable {

	JLabelWithFont info;
	PingThread pingThread;
	String monIP;
	
	public CheckProcess(JLabelWithFont info){
		this.info = info;
		pingThread = new PingThread(info);
		new Thread(pingThread).start();
	}
	
	@Override
	public void run() {
		while (Win.moniProcess) {
			try {
				//防止在启动时窗口跑到左上角
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try { 
				Process p = Runtime.getRuntime().exec("tasklist /fo csv /fi \"imagename eq "+Win.processName+" \" /NH");
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				StringBuilder sb=new StringBuilder();
				while((line = reader.readLine()) != null){
					sb.append(line);
				} 
				p.getInputStream().close(); 
				String processString = sb.toString();
				int processNumber=processString.split(Win.processName).length - 1;
				switch (processNumber) {
				case 0:
					pingThread.runFlag = false;
					info.setText("未发现进程");
					break;
				case 1:
					Vector<String> conIP=getConIP(processString.split(",")[1].replaceAll("\"", ""));
					if (conIP.size() == 0) {
						pingThread.runFlag = false;
						info.setText("进程未建立TCP连接");
						break;
					}
					synchronized (Win.IP) {
						if (Win.moniProcess) {
							Win.IP = conIP;
							pingThread.runFlag = true;
						}
					}
					break;

				default:
					pingThread.runFlag = false;
					info.setText("发现多个进程："+Win.processName);
					break;
				}
			} catch (IOException e) { 
				e.printStackTrace(); 
			} 
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		pingThread.runFlag = true;
	}
	
	public void monIP(String IP){
		Win.moniProcess = true;
		monIP = IP;
	}
	
	
	public Vector<String> getConIP(String pid){
		Vector<String> ConIP=new Vector<>(4);
		try {
			Process p = Runtime.getRuntime().exec("netstat -a -n -o");
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			StringBuilder sb=new StringBuilder();
			while((line = reader.readLine()) != null){
				line = line.trim();
				if (line.endsWith(pid) && line.startsWith("TCP")) {
					sb.append(line+" ");
				}
			} 
			p.getInputStream().close(); 
			String[] mes=sb.toString().split(" +");
			for (int i = 0; i < mes.length; i++) {
				if (mes[i].trim().equals(pid)) {
					String str = mes[i-2].split(":")[0];
					if (!ConIP.contains(str) && !str.startsWith("127")) {
						ConIP.add(str);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ConIP;
	}

}
