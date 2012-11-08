import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WebWorker extends Thread {
	
	private String urlString;
	private int index;
	private WebFrame.LaunchThread launcher;
	
	public WebWorker(String url, int index, WebFrame.LaunchThread launcher) {
		this.urlString = url;
		this.index = index;
		this.launcher = launcher;
	}
	
	@Override
	public void run() {
		try {
			launcher.finish(connect(), index);
		} catch (InterruptedException e) {
			launcher.finish("interrupted", index);
		}
	}
	
	private String connect() throws InterruptedException {
 		InputStream input = null;
		StringBuilder contents = null;
		try {
			long downloadStart = System.currentTimeMillis();
			URL url = new URL(urlString);
			URLConnection connection = url.openConnection();
		
			// Set connect() to throw an IOException
			// if connection does not succeed in this many msecs.
			connection.setConnectTimeout(5000);
			
			connection.connect();
			input = connection.getInputStream();

			BufferedReader reader  = new BufferedReader(new InputStreamReader(input));
		
			char[] array = new char[1000];
			int len;
			contents = new StringBuilder(1000);
			while ((len = reader.read(array, 0, array.length)) > 0) {
				contents.append(array, 0, len);
				Thread.sleep(100);
			}
			
			SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
			String time = (System.currentTimeMillis() - downloadStart) + "ms";
			String size = contents.length() + " bytes";
			return (format.format(new Date()) + " " + time + " " + size);
			
		}
		// Otherwise control jumps to a catch...
		catch(MalformedURLException ignored) { /* empty */ }
		catch(IOException ignored) { /* empty */ }
		// "finally" clause, to close the input stream
		// in any case
		finally {
			try{
				if (input != null) input.close();
			}
			catch(IOException ignored) { /* empty */ }
		}
		return "err";
	}
	
}
