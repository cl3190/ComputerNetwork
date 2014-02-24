package project1;

public class Foo implements Runnable {

	private int val;

	public Foo(int val) {
		this.val = val;
	}

	/* this synchronized is very very important */
	public synchronized void printVal(int val) {
		while (true)
			System.out.println(3);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		synchronized (this) {
			while (true) {
				System.out.println(1);
			}
		}
	}

}
