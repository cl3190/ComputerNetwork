import project1.Foo;


public class Test {
	
	public static void main(String args[]) 
	{ 
		Foo f1 = new Foo(1);
		Thread t1 = new Thread(f1);
		t1.start();
		f1.printVal(3);
	}
}
