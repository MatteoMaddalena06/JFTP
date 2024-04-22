//class to construct server log messages.
public class LogMessage
{
    public static final int clientConnected             = 0;
    public static final int unableToStartServer         = 1;
    public static final int unableToAcceptRequest       = 2;
    public static final int clientDisconnected          = 3;
    public static final int timeoutExpired              = 4;
    public static final int timeoutError                = 5;
    public static final int controlConnectionError      = 6;
    public static final int controlConnectionCloseError = 7;
    public static final int userNotFound                = 8;
    public static final int userRootDirectoryError      = 9;
    public static final int userFound                   = 10;
    public static final int userlistFileNotFound        = 11;
    public static final int userlistReadError           = 12;
    public static final int userlistFileIncomplete      = 13;
    public static final int correctPassword             = 14;
    public static final int unableToOpenFile            = 15;
    public static final int unableToDownloadFile        = 16;
    public static final int fileDownloaded              = 17;
    public static final int unableToUploadFile          = 18;
    public static final int fileUploaded                = 19;
    public static final int fileRenamed                 = 20;
    public static final int fileDeleted                 = 21;
    public static final int unableToDeleteFile          = 22;
    public static final int unbaleToCreateDirectory     = 23;
    public static final int directoryCreated            = 24;
    public static final int configurationInfo           = 25;

    private LogMessage() {}

    private static final String[] logMessages = {
        "   ├Client[*] connected.\n",
        "Error with control connection #unable to start the server#.\n",
        "   ├Error with control connection #unable to accept client request#.\n",
        "   ├Client[*] disconnected #client closed the connection#.\n",
        "   ├Client[*] disconnected #timeout expired#.\n",
        "   ├Client[*] disconnected #error in setting timeout#.\n",
        "   ├Client[*] disconnected #control connection error#.\n",
        "   ├Error while closing the control connection.\n",
        "   ├Client[*] USER info #username \"*\" not found#.\n",
        "   ├Client[*] USER error #user \"*\" has an incorrect root directory#.\n",
        "   ├Client[*] USER info #username \"*\" found(0)#.\n",
        "   ├Client[*] USER error #unable to find the list of users#.\n",
        "   ├Client[*] USER error #unable to read the list of users#.\n",
        "   ├Client[*] USER error #missing \"*\" user information(1)#.\n",
        "   ├Client[*] PASS info #user logged in as \"*\"#.\n",
        "   ├Client[*](*) * error #unable to read \"*\" file#.\n",
        "   ├Client[*]](*) * error #unable to download \"*\" file#.\n",
        "   ├Client[*](*) * info #\"*\" file downloaded#.\n",
        "   ├Client[*]](*) * error #unable to upload \"*\" file#.\n",
        "   ├Client[*](*) * info #\"*\" file uploaded#.\n",
        "   ├Client[*](*) RNTO info #file renamed from: \"*\" to \"*\"#.\n",
        "   ├Client[*](*) * info #\"*\" * deleted#.\n",
        "   ├Client[*](*) * error #unable to delete \"*\" *#.\n",
        "   ├Client[*](*) MKD error #unable to create \"*\" directory#.\n",
        "   ├Client[*](*) MKD info #\"*\" directory created#.\n",
        "~\n [jftp server]\n   ├server ip             →  *\n   ├server port           →  *\n" +
        "   ├control timeout       →  *\n   ├data accept timeout   →  *\n" +
        "   ├data receive timeout  →  *\n   ├user list file        →  *\n   └server root directory →  *\n\n [Server log]\n"
      };

    public static String create(int code, String... strings)
    { return new CreateMessage(logMessages).createMessage(code, strings); }
}
