import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


public class JCount extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private static int BLANK_SPACE = 40;
	public static int DEFAULT_MAX = 100000000;
	public static int UPDATE_FREQUENCY = 10000;
	public static int SLEEP_TIME = 100;
	
	private boolean active = false;
	private Thread worker = null;
	
	private final JTextField maxField;
	private final JLabel current;
	private final JButton start;
	private final JButton stop;
	public JCount() {
		super();
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		maxField = new JTextField();
		maxField.setText("" + DEFAULT_MAX);
		current = new JLabel("0");
		start = new JButton("Start");
		stop = new JButton("Stop");
		
		this.add(maxField);
		this.add(current);
		this.add(start);
		this.add(stop);
		this.add(Box.createRigidArea(new Dimension(0, BLANK_SPACE)));
		addListeners();
	}
	
	private void reset() {
		if(active) {
			worker.interrupt();
			active = false;
		}
	}
	
	private void addListeners() {
		start.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				reset();
				int max = Integer.parseInt(maxField.getText());
				worker = new CountWorker(max);
				worker.start();
			}
		});
		stop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg1) { reset(); }
		});
	}
	
	private class CountWorker extends Thread {
		private final int max;
		private int count = 0;
		public CountWorker(int max) {
			super();
			this.max = max;
		}
		@Override
		public void run() {
			active = true;
			try {
				while(count < max) {
					count++;
					if(count % UPDATE_FREQUENCY == 0) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								current.setText("" + count);
							}
						});
						Thread.sleep(SLEEP_TIME);
					}
				}
			} catch (InterruptedException e) { /* empty */ }
			active = false;
		}	
	}
	
	private static void createAndShowGUI() {
		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.add(new JCount());
		frame.add(new JCount());
		frame.add(new JCount());
		frame.add(new JCount());

		frame.pack();
		frame.setVisible(true);
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				createAndShowGUI();
			}
		});
	}

}
