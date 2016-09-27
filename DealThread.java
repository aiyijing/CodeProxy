package CodeProxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.crypto.Data;

public class DealThread extends Thread {
	

	private Socket client;	//客户端套接字
	
	private Socket remote;	//服务端套接字
	
	private DataInputStream fromclient;//来自客户端的输入流
						
	private InputStream	fromserver;//服务器的输入流
	
	private OutputStream toclient,//客户端输出流
						 toserver;//服务器输出流
	
	private Map<String, String> request; //将request 请求头放入map中，形成键值对方便自定义

	
	private String url;//绝对资源定位符
	
	private String uri;//相对资源定位符
	
	private int port=0;//服务器端口
	
	private String host=null;
	
	private String method=null;//Post or GET
	
	private String httpversion;
	private	String end="\r\n";
	
	public DealThread(Socket socket) {
		this.client=socket;
		request=new HashMap<String, String>();
	}

	public void run() {
		this.getClientDate();
		this.getPortAndHost();
		if(request.containsKey("User-Agent")) request.remove("User-Agent");
		if(request.containsKey("Referer")) request.remove("Referer");
		if(request.containsKey("Proxy-Connection")) request.remove("Proxy-Connection");
		if(request.containsKey("Connection") && request.get("Connection").equalsIgnoreCase("Keep-Alive")) {
			request.remove("Connection");
		}
		
		this.setStreamAndSocket();
		this.Client2Server();
		System.out.println("");
		this.Server2Client();
		try{
			if(toserver != null) toserver.close();
			if(fromserver != null) fromserver.close();
			if(remote != null) remote.close();


			if(toclient != null) toclient.close();
			if(fromclient != null) fromclient.close();
			if(client != null) client.close();
		
		}catch (Exception e) {
			return;
		}
	}
	
	public void getClientDate(){
		
		
		try {
			this.fromclient=new DataInputStream(client.getInputStream());
			
			//第一行的处理方式；
			String line=fromclient.readLine();
			if(line!=null){
				StringTokenizer tokens=new StringTokenizer(line);
				method=tokens.nextToken();
				url=tokens.nextToken();
				httpversion=tokens.nextToken();
			}
			String key,value;
			while((line=fromclient.readLine())!=null){
				
				if(line.trim().length()==0){break;}//到协议尾部自动退出
				
				key=line.split(": ")[0];
				value=line.split(": ")[1];
				request.put(key, value);
				
			}
			
		} catch (IOException e) {
			return;
		}
		
	}
	public void getPortAndHost(){
		
		//设定端口和host
		if(url==null || request.isEmpty()){ return; }
		url = url.split("http://")[1];
		uri=url.substring(url.indexOf("/"));
		host=request.get("Host");
		if(host.contains(":")){
			String temp[]=host.split(":");
			host=temp[0];
			port=Integer.parseInt(temp[1]);
			return ;
		}
		port=80;
		
	}
	public void setStreamAndSocket(){
		
			try {
				remote=new Socket(host, port);
				fromserver=remote.getInputStream();
				toserver=remote.getOutputStream();
				toclient=client.getOutputStream();
				fromclient=new DataInputStream(client.getInputStream());
			} catch (Exception e) {
				return;
			}		
	}
	public void Client2Server(){

		if(!method.startsWith("GET")&&!method.startsWith("POST")){return;}

			try {
				//写入method
				toserver.write((method+" "+uri+" "+httpversion).getBytes());
				toserver.write(end.getBytes());
				
				//写入header
				System.out.println(method+" "+uri+" "+httpversion);
				for ( String key : request.keySet()) {
					String line=key+": "+request.get(key);
					toserver.write(line.getBytes());
					toserver.write(end.getBytes());
					System.out.println(line);
				}
				toserver.write(end.getBytes());
				toserver.flush();
				
				
				//将Post数据写入server
				if(method.startsWith("POST")){
					int lenth=Integer.parseInt(request.get("Content-Length"));
					for (int i = 0; i < lenth; i++) {
						toserver.write(fromclient.read());
					}
				}
				toserver.write(end.getBytes());
				toserver.flush();
			} catch (Exception e) {
				return;
			}
			
	}
	public void Server2Client(){

		//构建响应头；
		try {
			DataInputStream dataInputStream=new DataInputStream(remote.getInputStream());
			String line=dataInputStream.readLine();
			while(line!=null){
				if(line.trim().length()==0){break;}
				toclient.write(line.getBytes());
				toclient.write(end.getBytes());
				System.out.println(line);
				line=dataInputStream.readLine();
			}
			toclient.write(end.getBytes());
			toclient.flush();
		//写入body体；
			InputStream is=remote.getInputStream();
			byte[] buff=new  byte[1024];
			for(int i; (i = is.read(buff)) != -1;) 
			{
				toclient.write(buff, 0, i);
				toclient.flush();
			}
			
		} catch (Exception e) {
			return;
		}
	}

}
