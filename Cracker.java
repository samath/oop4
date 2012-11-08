import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Cracker {
	// Array of chars used to produce strings
	public static final char[] CHARS = "abcdefghijklmnopqrstuvwxyz0123456789.,-!".toCharArray();	
	
	private final String target; 
	private final int length;
	private final int numThreads;
	private long timeElapsed = 0;
	public long getTimeElapsed() { return timeElapsed; }

	private final AtomicInteger counter = new AtomicInteger(0);
	public int countSolutions() { return counter.get();	}
	private final CountDownLatch latch;
	
	public Cracker(String target, int length, int numThreads) {
		this.target = target;
		this.length = length;
		this.numThreads = (numThreads < CHARS.length) ? numThreads : CHARS.length;
		latch = new CountDownLatch(numThreads);
	}
	
	public void crack() throws InterruptedException {
		long startTime = System.currentTimeMillis();
		for(int i = 0; i < numThreads; i++) {
			CrackerWorker cw = new CrackerWorker(calculateIndex(i, numThreads), calculateIndex(i + 1, numThreads));
			cw.start();
		}
		latch.await();
		timeElapsed = System.currentTimeMillis() - startTime;
	}
	public static int calculateIndex(int index, int threads) {
		return (index * CHARS.length) / threads;
	}

	
	public static void main(String args[]) {
		if(args.length == 1) {
			System.out.println(hexToString(hash(args[0])));
		} else if(args.length != 3) {
			System.err.println("Invalid number of arguments.");
		} else {
			try {
				Cracker c = new Cracker(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
				c.crack();
				System.out.println("Found " + c.countSolutions() + " solutions in " + c.getTimeElapsed() + " milliseconds.");
			} catch (NumberFormatException e) {
				System.err.println("USAGE: Cracker hash maxLength numThreads");
			} catch (InterruptedException e) {
				System.err.println("Interrupted: " + e.getMessage());
			}		
		}
	}
	
	public class CrackerWorker extends Thread {
		private final int start;
		private final int end;
		public CrackerWorker(int start, int end) {
			super();
			this.start = start;
			this.end = end;
		}
		
		@Override
		public void run() {
			for(int i = start; i < end; i++) {
				String s = "" + CHARS[i];
				crackRecursive(s);
			}
			latch.countDown();
		}
		
		public void crackRecursive(String s) {
			if(hexToString(hash(s)).equals(target)) {
				System.out.println(s);
				counter.incrementAndGet();
			}
			if(s.length() < length) {
				for(int i = 0; i < CHARS.length; i++) {
					crackRecursive(s + CHARS[i]);
				}
			}
		}
	}

	
	public static byte[] hash(String s) {
		try {
			byte[] bytes = s.getBytes();
			MessageDigest mg = MessageDigest.getInstance("SHA");
			mg.update(bytes);
			return mg.digest();
		} catch(NoSuchAlgorithmException e) {
			System.out.println(e.getMessage());
			return new byte[0];
		}
	}
	
	/*
	 Given a byte[] array, produces a hex String,
	 such as "234a6f". with 2 chars for each byte in the array.
	 (provided code)
	*/
	public static String hexToString(byte[] bytes) {
		StringBuffer buff = new StringBuffer();
		for (int i=0; i<bytes.length; i++) {
			int val = bytes[i];
			val = val & 0xff;  // remove higher bits, sign
			if (val<16) buff.append('0'); // leading 0
			buff.append(Integer.toString(val, 16));
		}
		return buff.toString();
	}
	
	/*
	 Given a string of hex byte values such as "24a26f", creates
	 a byte[] array of those values, one byte value -128..127
	 for each 2 chars.
	 (provided code)
	*/
	public static byte[] hexToArray(String hex) {
		byte[] result = new byte[hex.length()/2];
		for (int i=0; i<hex.length(); i+=2) {
			result[i/2] = (byte) Integer.parseInt(hex.substring(i, i+2), 16);
		}
		return result;
	}
	
	// possible test values:
	// a 86f7e437faa5a7fce15d1ddcb9eaeaea377667b8
	// fm adeb6f2a18fe33af368d91b09587b68e3abcb9a7
	// a! 34800e15707fae815d7c90d49de44aca97e2d759
	// xyz 66b27417d37e024c46526c2f6d358a754fc552f3

}
