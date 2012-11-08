
public class Account {

	private final int id;
	private int balance;
	private int transactions = 0;
	
	public Account(int id, int balance) {
		this.id = id;
		this.balance = balance;
	}
	
	public synchronized int getBalance() {
		return balance;
	}
	
	public synchronized int getTransactions() {
		return transactions;
	}
	
	public synchronized void transaction(int amount) {
		balance += amount;
		transactions++;
	}
	
	@Override
	public String toString() {
		synchronized(this) {
			return "acct:" + id + " bal:"+  balance + " trans:" + transactions;
		}
	}
	
}
