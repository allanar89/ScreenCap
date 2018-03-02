package cu.slam.capturafacil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Scanner;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Activity;
import android.content.res.AssetManager;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {

	String ruta = Environment.getExternalStorageDirectory() + "/www/";
	String nFichero = "cap.jpg";
	ServerSocket ss;
	Socket conexion;
	Thread tarea;
	Handler man = new Handler();
	TextView info;
	int rotacionActual=0;
	String HTML;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		info = (TextView)findViewById(R.id.textView1);
		ServidorIdle();
		tarea = new Thread(new Runnable() {

			@Override
			public void run() {				
				tsck();
				man.postDelayed(this, 1500);// repetir cada 1.5 segundo
			}
		});
		tarea.start();
	}

	void ServidorIdle(){		
		try {
			SocketServerThread sst = new SocketServerThread(1234);	
			info.setText(getIpAddress()+" en puerto "+1234+" esperando conexiones.");
			sst.start();
		} catch (Exception e) {
			//e.printStackTrace();
			Log.e("error: ", e.getMessage());
		}		
	}
	
	
	void tsck() {
		try {
			Process sh = Runtime.getRuntime().exec("su");
			OutputStream os = sh.getOutputStream();
			os.write(("/system/bin/screencap -p " + ruta + nFichero)
					.getBytes("ASCII"));
			File file = new File(ruta, nFichero);
			if (file.exists()) {
				FileInputStream fis = new FileInputStream(file);
				byte[] lect = new byte[fis.available()];
				fis.read(lect);
				fis.close();
				rotacionActual = getWindowManager().getDefaultDisplay()
						.getRotation();
				switch (rotacionActual) {// Surface.ROTATION_angulo
				case 1:
					rotacionActual *= -90;
					break;
				case 2:
					rotacionActual = 180;
					break;
				case 3:
					rotacionActual *= -90;//-270 garantiza siempre estar en posición izq - der
					break;
				default:
					rotacionActual = 0;
					break;
				}
				//Log.e("rot: ", ""+rotacionActual);
				HTML = PaginaHTML(rotacionActual);				
				// Escribe en el index.php el parámetro de rotación para la
				// imágen
				File indexhtml = new File(ruta, "index.php");
				FileOutputStream fos = new FileOutputStream(indexhtml);				
				fos.write(HTML.getBytes());
				fos.close();
			}
			os.flush();
			os.close();
			sh.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
			//Log.e("Err", "" + e.getMessage());
		}

	}

	String PaginaHTML(int grado) {
		String index = "";
		String val = "";
		try {
			AssetManager am = getAssets();
			Scanner in = new Scanner(am.open("index.html"));
			while (in.hasNext()) {
				val = in.nextLine();				
				if (val.contains("deg")) {
					val = "<script>document.getElementById('screen').style.transform = "
							+ "'rotate(" + grado + "deg)'" + ";</script>";					
				}
				index += val + "\n";				
			}

		} catch (Exception e) {
			Log.e("err", "" + e.getMessage() + "...");
		} finally {
			return index;
		}
	}
	
	private String getIpAddress() {
		String localObject = "";
		try {
			Enumeration<NetworkInterface> localEnumeration1 = NetworkInterface.getNetworkInterfaces();
			while (true) {
				if (!localEnumeration1.hasMoreElements())
					return localObject;
				Enumeration<InetAddress> localEnumeration2 = ((NetworkInterface) localEnumeration1
						.nextElement()).getInetAddresses();
				while (localEnumeration2.hasMoreElements()) {
					InetAddress localInetAddress = (InetAddress) localEnumeration2
							.nextElement();
					if (localInetAddress.isSiteLocalAddress()) {
						localObject = "IP local: "
								+ localInetAddress.getHostAddress() + "\n";
					}
				}
			}
		} catch (SocketException localSocketException) {
			localSocketException.printStackTrace();
			return localObject + "Error, algo va mal! "
					+ localSocketException.toString() + "\n";
		}
	}
	
	private class SocketServerReplyThread extends Thread {
		private Socket hostThreadSocket;
		String tmsg;

		SocketServerReplyThread(Socket paramString, String arg3) {
			this.hostThreadSocket = paramString;
			Log.e("socket: ", hostThreadSocket.toString());
			this.tmsg = arg3;
		}

		public void run() {
			String str = this.tmsg;
			try {
				PrintStream localPrintStream = new PrintStream(hostThreadSocket.getOutputStream());
				localPrintStream.print(str);
				DatagramPacket dp=new DatagramPacket(str.getBytes(), 0, str.length());
				dp.setSocketAddress(hostThreadSocket.getLocalSocketAddress());
				DatagramSocket ds = new DatagramSocket(hostThreadSocket.getPort());
				ds.send(dp);
				ds.close();
				localPrintStream.close();
				return;
			} catch (IOException localIOException) {
				localIOException.printStackTrace();
			}
		}
	}
	private class SocketServerThread extends Thread {
		private int SocketServerPORT;		

		private SocketServerThread(int port) {
			this.SocketServerPORT = port;			
		}

		public void run() {
			try {
				ss = new ServerSocket(SocketServerPORT);				
				while (true) {
					ss.setReuseAddress(true);
					Socket localSocket = ss.accept();
					Log.e("Info","Alguien se conecto");
					SocketServerReplyThread ssrt = new SocketServerReplyThread(localSocket, HTML);
					ssrt.run();
				}
			} catch (IOException localIOException) {
				localIOException.printStackTrace();
			}
		}
	}

}
