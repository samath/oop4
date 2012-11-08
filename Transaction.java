
public final class Transaction {
	
	private final int amount;
	private final int toID;
	private final int fromID;
	
	public Transaction(int toID, int fromID, int amount) {
		this.amount = amount;
		this.toID = toID;
		this.fromID = fromID;
	}
	
	public int getAmount() { return amount; }
	public int getToID() { return toID; }
	public int getFromID() { return fromID; }
	
	public static Transaction getNullTransaction() {
		return new Transaction(-1, 0, 0);
	}
	
	@Override
	public String toString() {
		return "" + toID + " " + fromID + " " + amount;
	}

}
