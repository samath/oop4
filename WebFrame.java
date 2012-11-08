import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;


public class WebFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	
	private static final String FILE_NAME = "links.txt";
	
	private DefaultTableModel model;
	private JButton singleFetch;
	private JButton multiFetch;
	private JTextField numThreadsBox;
	private JLabel runningLabel;
	private JLabel completedLabel;
	private JLabel elapsedLabel;
	private JProgressBar progress;
	private JButton stop;
	
	private boolean running = false;
	private final int numUrls;
	private AtomicInteger runningCount = new AtomicInteger(0);
	private AtomicInteger completedCount = new AtomicInteger(0);
	private long startTime = 0;
	
	private LaunchThread launch = null;
	
	public WebFrame(List<String> urls) {
		super("WebLoader");
		
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		this.add(getTable());
		this.add(getPanel());
		
		numUrls = urls.size();
		for(String s : urls) {
			model.addRow(new String[] { s, "" });
		}
		progress.setMaximum(numUrls);
		addListeners();
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible(true);
	}
	

	
	private JPanel getTable() {
		JPanel panel = new JPanel();
		
		model = new DefaultTableModel(new String[] { "url", "status"}, 0);
		JTable table = new JTable(model);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(new Dimension(600, 300));
		panel.add(scroll);

		return panel;
	}
	
	private JPanel getPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		singleFetch = new JButton("Single Thread Fetch");
		multiFetch = new JButton("Concurrent Fetch");
		numThreadsBox = new JTextField();
		numThreadsBox.setMaximumSize(new Dimension(100, 20));
		runningLabel = new JLabel();
		completedLabel = new JLabel();
		elapsedLabel = new JLabel();
		resetLabels();
		progress = new JProgressBar();
		stop = new JButton("Stop");
		
		panel.add(singleFetch);
		panel.add(multiFetch);
		panel.add(numThreadsBox);
		panel.add(runningLabel);
		panel.add(completedLabel);
		panel.add(elapsedLabel);
		panel.add(progress);
		panel.add(stop);
		return panel;
	}
	
	private void addListeners() {
		singleFetch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				launch = new LaunchThread(1);
				launch.start();
			}
		});
		multiFetch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					int count = Integer.parseInt(numThreadsBox.getText());
					launch = new LaunchThread(count);
					launch.start();
				} catch (NumberFormatException e) { /* empty */ }
			}
		});
		stop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(launch != null && launch.isAlive()) launch.interrupt();
			}
		});
	}
	
	private void setRunning() {
		if(!running) {
			runningCount = new AtomicInteger(0);
			completedCount = new AtomicInteger(0);
			running = true;
			startTime = System.currentTimeMillis();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
						singleFetch.setEnabled(false);
						multiFetch.setEnabled(false);
						stop.setEnabled(true);
						resetLabels();
						progress.setValue(0);
					
						for(int i = 0; i < model.getRowCount(); i++) {
							model.setValueAt("", i, 1);
						}
						update();
				}});	
		}
	}
	
	private void setNotRunning() {
		if(running) {
			running = false;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
						singleFetch.setEnabled(true);
						multiFetch.setEnabled(true);
						stop.setEnabled(false);	
						update();
				}});
		}
	}
	
	private void resetLabels() {
		runningLabel.setText("Running: 0");
		completedLabel.setText("Completed: 0");
		elapsedLabel.setText("Elapsed: 0");
	}
	
	private void update() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				runningLabel.setText("Running: " + runningCount.get());
				completedLabel.setText("Completed: " + completedCount.get());
				elapsedLabel.setText("Elapsed: " + (System.currentTimeMillis() - startTime));
				model.fireTableDataChanged();
		}});
	}
	
	public static void main(String[] args) {
		List<String> urls = new ArrayList<String>();
		BufferedReader reader = null;;
		try {
			reader = new BufferedReader(new FileReader(FILE_NAME));
			String s = "";
			while((s = reader.readLine()) != null) {
				urls.add(s);
			}
		} catch (FileNotFoundException e) {
			System.err.println(args[0] + " not found: " + e.getMessage());
			System.exit(1); 
		} catch (IOException e) {
			System.err.println("IOException in parsing urls: " + e.getMessage());
			System.exit(1);
		} finally {
			if(reader != null)
				try {
					reader.close();
				} catch (IOException e) { /* empty */ }
		}
		
		if(urls.size() > 0) {
			@SuppressWarnings("unused")
			WebFrame wf = new WebFrame(urls);
		}
	}
	
	class LaunchThread extends Thread {
		
		private final Semaphore lock;
		private final int numThreads; 
		private final WebWorker[] workers;
		
		public LaunchThread(int numThreads) {
			lock = new Semaphore(numThreads);
			this.numThreads = numThreads;
			workers = new WebWorker[numUrls];
		}
		
		@Override
		public void run() {
			try {
				setRunning();
				runningCount.incrementAndGet();
				for(int i = 0; i < model.getRowCount(); i++) {
					workers[i] = start(i);
					workers[i].start();
				}
				lock.acquire(numThreads);
			} catch (InterruptedException e) {
				for(WebWorker w : workers) {
					if(w != null && w.isAlive()) w.interrupt();
				}
			} finally {
				runningCount.decrementAndGet();
				setNotRunning();
			}
		}
		
		private WebWorker start(int i) throws InterruptedException {
			lock.acquire();
			runningCount.incrementAndGet();
			update();
			return new WebWorker((String)model.getValueAt(i, 0), i, this);
		}
		public void finish(String status, int index) {
			model.setValueAt(status, index, 1);
			runningCount.decrementAndGet();
			completedCount.incrementAndGet();
			update();
			lock.release();
		}

	}

}
