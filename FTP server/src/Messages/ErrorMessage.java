//class to construct server error messages.
public class ErrorMessage
{
    public static final int missingParameter          = 0;
    public static final int configurationNotFound     = 1;
    public static final int unableToReadConfiguration = 2;
    public static final int notInitializedParameter   = 3;
    public static final int invalidParameterType      = 4;
    public static final int invalidIpAddress          = 5;
    public static final int invalidPathname           = 6;
    public static final int rootDirectoryIsFile       = 7;
    public static final int userlistIsNotFile         = 8;
    public static final int invalidPortNumber         = 9;
    public static final int incorrectUse              = 10;

    private ErrorMessage() {}

    private static final String[] errorMessages = {
        "Essential parameters are missing. Error in [*].\n",
        "Configuration file [*] not found.\n",
        "Unable to read configuration file [*].\n",
        "Parameter not initialized. Error in [*].\n",
        "Invalid parameter type. Error in [*].\n",
        "Invalid IP address. Error in [*].\n",
        "Invalid pathnames. Error in [*].\n",
        "The pathname of the \"rootdirectory\" parameter must reference a directory. Error in [*].\n",
        "The pathname of the \"userlistfile\" parameter must reference a file. Error in [*].\n",
        "Invalid \"controlconnectionport\" parameter number. Must be an integer between 0 and 65535. Error in [*].\n",
        "Usage: java JFtpServer <configuration file>.\n"
     };

    public static String create(int code, String... strings)
    { return new CreateMessage(errorMessages).createMessage(code, strings); }
}
