
public class Test {
	public static void main(String ... args){
		String a = "1111\n\n22222222\n\n";
		String b[] = a.split("\n\n");
		System.out.println(b.length);
		for(String c: b){
			System.out.println(c);
		}
		
		String d="^^^^aaa";
		System.out.println(d.replaceAll("\\^\n", "\n"));
	}
}
