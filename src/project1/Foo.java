package project1;

public class Foo implements Runnable {

	private int val;
	
	public Foo(int val){this.val = val;}
	
	/*this synchronized is very very important*/
	public synchronized void printVal(int val){
		while(true)
			System.out.println(val);
	}
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(true)
			printVal(1);
	}
	

}
