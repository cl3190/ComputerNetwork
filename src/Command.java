
public enum Command {
	LOGIN,LOGOUT,WHOELSE,WHOLASTHR,BROADCAST,
	MESSAGE,BLOCK,UNBLOCK,UNKNOWN;
	
	public static Command getCommand(String str){
		try{
			return valueOf(str.toUpperCase());
		}catch(Exception ex){
			return UNKNOWN;
		}
	}
}
