import java.util.*;

//class to construct responses for the client.
public class Response
{
    public static final int genericError               = 500;
    public static final int superfluousCommand         = 202;
    public static final int notImplementedCommand      = 502;
    public static final int unrecognizedParameter      = 501;
    public static final int notLoggedIn                = 530;
    public static final int genericSuccess             = 200;
    public static final int genericTmpError            = 400;
    public static final int usernameFound              = 331;
    public static final int wrongOrder                 = 503;
    public static final int loggedIn                   = 230;
    public static final int actionNotTaken             = 550;
    public static final int actionTaken                = 250;
    public static final int readyForUser               = 220;
    public static final int closeConnection            = 221;
    public static final int enteringPassiveMode        = 227;
    public static final int unableToOpenDataConnection = 425;
    public static final int openDataConnection         = 150;
    public static final int transferAborted            = 426;
    public static final int transferSuccess            = 226;
    public static final int waitMoreCommand            = 350;
    public static final int tmpActionNotTaken          = 450;
    public static final int directorySuccess           = 257;
    public static final int systemInfo                 = 215;
    public static final int helpOK                     = 214;
    public static final int fileNameNotAllowed         = 553;

    private Response() {}

    private static HashMap<Integer, String> responseMessages = new HashMap<Integer, String>()
      {{
          put(500, "500 * failed. *.");
          put(202, "202 * is superfluous.");
          put(502, "502 * not implemented.");
          put(501, "501 * failed. Unrecognized parameter.");
          put(530, "530 * failed. *.");
          put(200, "200 * OK. *.");
          put(400, "400 * failed. Error during processing.");
          put(331, "331 USER OK. Username ok.");
          put(503, "503 * failed. Enter * first.");
          put(230, "230 PASS OK. Logged in.");
          put(550, "550 * failed. *.");
          put(250, "250 * OK. *");
          put(220, "220*.");
          put(221, "221 *.");
          put(227, "227 PASV OK. Entering passive mode (*).");
          put(425, "425 * failed. *.");
          put(150, "150 * OK. About to open data connection.");
          put(426, "426 * failed. Connection closed; transfer aborted.");
          put(226, "226 * OK. Closing data connection.");
          put(350, "350 * OK. Need * now");
          put(450, "450 * failed. *.");
          put(257, "257 * OK. *.");
          put(215, "215 UNIX Type: L8.");
          put(214, "214-List of supported commands:\r\n" +
                   "     TYPE  USER  PASS  CWD   CDUP  REIN\r\n" +
                   "     QUIT  PORT  PASV  STRU  MODE  RETR\r\n" +
                   "     STOR  STOU  APPE  RNFR  RNTO  DELE\r\n" +
                   "     LIST  MKD   PWD   SYST  HELP  NOOP\r\n" +
                   "     NLST  RMD\r\n" +
                   "214 HELP OK.");
          put(553, "553 * failed. File name not allowed.");
      }};

    public static String create(int code, String... strings)
    {
        String[] array = {responseMessages.get(code)};
        return new CreateMessage(array).createMessage(0, strings);
    }

}
