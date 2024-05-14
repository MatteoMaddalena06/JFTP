import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.SimpleDateFormat;
import java.util.*;

//General utility class.
public class Utils
{
    //regex to check IP address correctness.
    public static final String IpAddressRegex = "([0-9]+\\.){3}[0-9]+";
    //size of the buffer used for data transmission.
    public static final int BUFFER_SIZE = 8192;

    public static final String welcomeMessage =
        "-\r\n"+
        "\t        ___  ________ _________  ________    \r\n" +
        "\t       |\\  \\|\\  _____\\\\___   ___\\\\   __  \\  \r\n " +
        "\t       \\ \\  \\ \\  \\__/\\|___ \\  \\_\\ \\  \\|\\  \\  \r\n" +
        "\t     __ \\ \\  \\ \\   __\\    \\ \\  \\ \\ \\   ____\\ \r\n" +
        "\t    |\\  \\\\_\\  \\ \\  \\_|     \\ \\  \\ \\ \\  \\___| \r\n" +
        "\t    \\ \\________\\ \\__\\       \\ \\__\\ \\ \\__\\    \r\n" +
        "\t     \\|________|\\|__|        \\|__|  \\|__|    \r\n" +
        "\r\n" +
        "220-Welcome to ~JFTP~ server. This is an FTP server developed entirely in java as a school project.\r\n" +
        "220-The server complies with the minimum implementation defined by the RFC 959 specification.\r\n" +
        "220-The remaining commands, defined in other RFCs, have not been implemented.\r\n" +
        "220 To obtain the list of available commands use the HELP command";

    private Utils(){}

    /*
        Method for reading the server configuration.
        Input:
          1. the pathname of the configuration file.
        Return values:
          1. an instance of the ServerConfiguration class containing the configuration.
    */
    public static ServerConfiguration getServerConfiguration(String pathname)
      throws IOException, FileNotFoundException, ArrayIndexOutOfBoundsException, NumberFormatException
    {
        ServerConfiguration configuration;
        BufferedReader fileInputStream;
        String fileInput;
        String[] fileInputTokens;
        boolean[] flagArray = {false, false, false, false, false, false, false};
        int parametersCounter = 0;

        configuration = new ServerConfiguration();
        fileInputStream = new BufferedReader(new FileReader(pathname));

        while((fileInput = fileInputStream.readLine()) != null)
        {
            fileInput = fileInput.replaceAll(" |#.*", "");
            fileInputTokens = fileInput.split("=");

            if(fileInputTokens[0].equals("serverip"))
            {
                configuration.serverIp = fileInputTokens[1];
                parametersCounter += (flagArray[0]) ? 0 : 1;
                flagArray[0] = true;
            }
            else if(fileInputTokens[0].equals("rootdirectory"))
            {
                configuration.rootDirectory = fileInputTokens[1];
                parametersCounter += (flagArray[1]) ? 0 : 1;
                flagArray[1] = true;
            }
            else if(fileInputTokens[0].equals("userlistfile"))
            {
                configuration.userlistFile = fileInputTokens[1];
                parametersCounter += (flagArray[2]) ? 0 : 1;
                flagArray[2] = true;
            }
            else if(fileInputTokens[0].equals("listenport"))
            {
                configuration.listenPort = Integer.parseInt(fileInputTokens[1]);
                parametersCounter += (flagArray[3]) ? 0 : 1;
                flagArray[3] = true;
            }
            else if(fileInputTokens[0].equals("controltimeout"))
            {
                configuration.controlTimeout = Integer.parseInt(fileInputTokens[1]);
                parametersCounter += (flagArray[4]) ? 0 : 1;
                flagArray[4] = true;
            }
            else if(fileInputTokens[0].equals("dataaccepttimeout"))
            {
                configuration.dataAcceptTimeout = Integer.parseInt(fileInputTokens[1]);
                parametersCounter += (flagArray[5]) ? 0 : 1;
                flagArray[5] = true;
            }
            else if(fileInputTokens[0].equals("datarcvtimeout"))
            {
                configuration.dataRcvTimeout = Integer.parseInt(fileInputTokens[1]);
                parametersCounter += (flagArray[6]) ? 0 : 1;
                flagArray[6] = true;
            }
        }

        configuration.isAllParametersEntered = (parametersCounter == 7);
        return configuration;
    }

    /*
        Method for reading all information about a user in the users file.
        Input:
          1. the users file.
          2. the username.
        Return value:
          1. an instance of the UserInformation class containing information about the searched user.
    */
    public static UserInformation getUserInformation(String userListFile, String username)
      throws IOException, ArrayIndexOutOfBoundsException, FileNotFoundException
    {
        BufferedReader userListFileReader;
        UserInformation info;
        String input;
        String[] inputTokens, permissions;

        userListFileReader = new BufferedReader(new FileReader(userListFile));
        info = new UserInformation();

        info.userNotFound = true;

        while((input = userListFileReader.readLine()) != null)
        {
            input = input.replaceFirst("#.*", "");
            inputTokens = input.split(" ");

            if(!inputTokens[0].equals(username))
              continue;

            permissions = inputTokens[2].toLowerCase().split("-");

            info.userNotFound = false;
            info.password = inputTokens[1];

            for(String permission : permissions)
            {
                info.listPermission = permission.equals("l") || info.listPermission;
                info.readPermission = permission.equals("r") || info.readPermission;
                info.writePermission = permission.equals("w") || info.writePermission;
                info.deletePermission = permission.equals("d") || info.deletePermission;
                info.renamePermission = permission.equals("re") || info.renamePermission;
                info.seeHidenFilePermission = permission.equals("h") || info.seeHidenFilePermission;
            }

            info.rootDirectory = getCorrectPath("/", "/", inputTokens[3]);
            break;
        }

        return info;
    }

    /*
        Method for normalizing and correcting a pathname.
        Input:
          1. the root directory.
          2. the current working directory.
          3. the pathname to adjust.
        Return avlue:
          1. the adjusted directory.
    */
    public static String getCorrectPath(String rootDirectory, String workingDirectory, String pathname)
    {
        Path newPathObj;

        if(pathname.charAt(0) == '/')
          newPathObj = Paths.get(rootDirectory + pathname).normalize();

        else
          newPathObj = Paths.get(workingDirectory + '/' + pathname).normalize();

        if(newPathObj.startsWith(rootDirectory))
          return newPathObj.toString();

        return rootDirectory;
    }

    /*
        Method for obtaining the attributes of a file or directory.
        Input:
          1. a pathname.
          2. the command that uses the method.
        Return value:
          1. the file attributes as a String.
        If command = "LIST", the method returns all posix-style attributes.
        Otherwise, if command = "NLST", the method returns only the file names.
    */
    public static String getFileAttributes(String pathname, String command) throws IOException
    {
        PosixFileAttributes posixAttributes;
        Set<PosixFilePermission> permissions;
        String permissionsString, date;
        Path filePathObj;
        int numLinks;

        filePathObj = Paths.get(pathname);
        posixAttributes = Files.readAttributes(filePathObj, PosixFileAttributes.class);
        permissions = posixAttributes.permissions();
        permissionsString = PosixFilePermissions.toString(permissions);
        numLinks = (int) Files.getAttribute(filePathObj, "unix:nlink");
        date = new SimpleDateFormat("MMM dd HH:mm").format(new Date(posixAttributes.lastModifiedTime().toMillis()));

        if(command.equals("LIST"))
          return String.format
            (
                "%c%s  %3d  %-8s  %-8s  %8d  %s  %s\r\n", (Files.isDirectory(filePathObj)) ? 'd' : '-',
                permissionsString, numLinks, posixAttributes.owner(), posixAttributes.group(),
                posixAttributes.size(), date,  filePathObj.getFileName()
            );

        return filePathObj.getFileName() + "\r\n";
    }

    /*
        Method for transferring files.
        Input:
          1. the source stream.
          2. the destination stream.
          3. the type of data representation used for transmission("I", "A", "L 8", "A N").
    */
    public static void transferData(InputStream source, OutputStream destination, String type) throws IOException
    {
        byte[] buffer = new byte[BUFFER_SIZE], conversion_buffer = null;
        int size;
        boolean dataType;

        if((dataType = type.equals("A") || type.equals("A N")))
            conversion_buffer = new byte[CONV_BUFFER_SIZE];

        while((size = source.read(buffer, 0, BUFFER_SIZE)) > 0)
        {
            if(dataType)
                size = convert2ascii(buffer, conversion_buffer, size);

            destination.write((dataType) ? conversion_buffer : buffer, 0, size);
        }

        source.close(); destination.close();
    }

    private static int convert2ascii(byte[] src, byte[] dest, int size)
    {
        int j = 0;

        for(int i = 0; i < size; i++)
        {
            if(src[i] == '\n' && (i == 0 || src[i - 1] != '\r'))
            {
                dest[j++] = '\r';
                dest[j++] = '\n';
            }
            else
              dest[j++] = src[i];
        }

        return j;
    }
}

//class to return the server configuration.
class ServerConfiguration
{
    public String  serverIp;
    public String  rootDirectory;
    public String  userlistFile;
    public int     listenPort;
    public int     controlTimeout;
    public int     dataAcceptTimeout;
    public int     dataRcvTimeout;
    public boolean isAllParametersEntered;
}

//class to return information about a user.
class UserInformation
{
    public String  password;
    public boolean listPermission = false;
    public boolean readPermission = false;
    public boolean writePermission = false;
    public boolean deletePermission = false;
    public boolean renamePermission = false;
    public boolean seeHidenFilePermission = false;
    public String  rootDirectory;
    public boolean userNotFound;
}

//class to return the output of a command.
class CommandOutput
{
    private String responseMessage;
    private String logMessage;

    public CommandOutput(String responseMessage)
    {
        this.responseMessage = responseMessage;
        logMessage = "";
    }

    public CommandOutput(String responseMessage, String logMessage)
    {
        this(responseMessage);
        this.logMessage = logMessage;
    }

    public String getResponse()
    { return responseMessage; }

    public String getLog()
    { return logMessage; }
}

//Interface for implementing an array of function pointers.
interface FunctionPointer
{
    CommandOutput executeCommand(String parameters);
}
