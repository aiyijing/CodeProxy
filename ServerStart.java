package CodeProxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerStart {
	public static int port=8989;
	protected ExecutorService executor;
	protected ServerSocket serverSocket;
	public ServerStart(int port){
		executor = Executors.newCachedThreadPool();
		try {
			serverSocket =new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void accept(){
		while(true){
			try {
				Socket socket=this.serverSocket.accept();
				new DealThread(socket).start();;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public static void main(String[] args) throws IOException {
		new ServerStart(port).accept();
		
	}
	
}
