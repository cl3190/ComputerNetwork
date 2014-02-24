


public class Test {
	
	public static void main(String args[]) 
	{ 
		String str = "login";
		
		switch(Command.getCommand(str)){
			case LOGIN:
				System.out.println("login");break;
			case LOGOUT:
				System.out.println("logout");break;
			default:
				System.out.println("unknown");break;
		}
	}
}
