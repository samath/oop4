import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;


public class Bank {
	
	private static String USAGE = "Usage: Bank inputFile.txt numThreads";
	private static void printUsage() { System.err.println(USAGE); }
	
	private static int NUM_ACCOUNTS = 20;
	private static int CAPACITY = 20;
	private static int BALANCE = 1000;
	
	public static void main(String args[]) {
		if(args.length != 2) { printUsage(); return; }
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(new File(args[0])));
		} catch(FileNotFoundException e) {
			printUsage(); return;
		}
		
		Bank bank = new Bank(Integer.parseInt(args[1]));
		bank.startWorkers();
		try {
			parseFile(reader, bank);
			bank.await();
			for(int i = 0; i < NUM_ACCOUNTS; i++) {
				System.out.println(bank.getAccount(i).toString());
			}		
		} catch(InterruptedException e) {
			System.err.println("Main thread interrupted: " + e.getMessage());
		} catch(IOException e) {
			System.err.println("IOException in reading " + args[0] + ": " + e.getMessage());
		}
	}
	
	public static void parseFile(BufferedReader f, Bank bank) throws InterruptedException, IOException {
		String nextLine;
		while((nextLine = f.readLine()) != null) {
			String[] items = nextLine.split(" ", 3);
			if(items.length == 3) {
				try {
					bank.put(new Transaction(Integer.parseInt(items[0]), 
										  	  Integer.parseInt(items[1]),
										  	  Integer.parseInt(items[2])));
				} catch(NumberFormatException e) {
					System.err.println("Invalid line: " + nextLine);
				}
			} else System.err.println("Invalid line: " + nextLine);
		}
		for(int i = 0; i < bank.numThreads; i++) {
			bank.put(Transaction.getNullTransaction());
		}
	}
	
	public final int numThreads;
	private CountDownLatch latch;
	private BlockingQueue<Transaction> queue;
	private Account[] accountArray;
	
	public Bank(int numThreads) {
		this.numThreads = numThreads;
		latch = new CountDownLatch(numThreads);
		queue = new ArrayBlockingQueue<Transaction>(CAPACITY);
		
		accountArray = new Account[NUM_ACCOUNTS];
		for(int i = 0; i < accountArray.length; i++) {
			accountArray[i] = new Account(i, BALANCE);
		}
	}
	
	public void startWorkers() {
		for(int i = 0; i < numThreads; i++) {
			Worker w = new Worker();
			w.start();
		}	
	}
	
	public void put(Transaction t) throws InterruptedException {
		queue.put(t);
	}
	
	public void await() throws InterruptedException {
		latch.await();
	}
	
	public Account getAccount(int i) {
		return accountArray[i];
	}
	

	
	private class Worker extends Thread {		
		@Override
		public void run() {
			while(true) {
				try {
					Transaction t = queue.take();
					if(t.getToID() < 0) {
						latch.countDown();
						return;
					} else {
						accountArray[t.getToID()].transaction(-t.getAmount());
						accountArray[t.getFromID()].transaction(t.getAmount());
					}
				} catch(InterruptedException e) {
					System.err.println(e.getMessage());
				}
			}
		}
	}


}