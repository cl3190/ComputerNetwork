public class Code {
	/* log related command */
	public static final int LOG_SUCCESS = 102;
	/*
	 * LOG_FAIL is when pass is wrong, 
	 * LOG_DENY is when the pass is wrong for 3
	 * times, server is blocking the access
	 */
	public static final int LOG_FAIL = 103;
	public static final int LOG_DENY = 104;
	public static final int LOG_ALREADY_LOGGED = 105;
	public static final int LOG_UNAME_NOT_EXIST=106;
	public static final int LOG_OUT_REPLY = 107;
	
	/*unknown command*/
	public static final int UNKNOWN_COMMAND = 201;
	
	
	/* display user name related*/
	public static final int WHO_ELSE_RESPOND = 301;
	public static final int WHO_LAST_HOUR = 302;
	
	/*blocking related*/
	public static final int BLOCK_NO_SUCH_USER=401;
	public static final int BLOCK_YOUSELF = 402;
	public static final int BLOCK_SUCCSS = 403;
	
	/*unblocking related*/
	public static final int UNBLOCK_NO_SUCH_USER=501;
	public static final int UNBLOCK_YOUSELF = 502;
	public static final int UNBLOCK_SUCCSS = 503;
	
}
