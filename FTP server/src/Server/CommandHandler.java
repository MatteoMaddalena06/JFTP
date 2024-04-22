import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

//Class containing methods for executing various FTP commands.
public class CommandHandler
{
    private JFtpWorker serverData;
    private HashMap<String, FunctionPointer> commandsArray;

    public CommandHandler(JFtpWorker serverData)
    {
        this.serverData = serverData;
        initFunctionArray();
    }

    /*
        Method for executing commands received from the client.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    public CommandOutput executeCommand()
    {
        FunctionPointer commandFunction;
        String[] commandTokens = serverData.currentCommand.split(" ", 2);

        commandTokens[0] = commandTokens[0].toUpperCase();

        if((commandFunction = commandsArray.get(commandTokens[0])) == null)
            return new CommandOutput(Response.create(Response.genericError, commandTokens[0], "Unrecognized command"));

        return commandFunction.executeCommand((commandTokens.length == 2) ? commandTokens[1] : null);
    }

    /*
        Method for superfluous commands.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput superfluousCommand(String parameters)
    { return new CommandOutput(Response.create(Response.superfluousCommand, serverData.currentCommand.split(" ", 2)[0].toUpperCase())); }

    /*
        Method for unimplemented commands
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput notImplementedCommand(String parameters)
    { return new CommandOutput(Response.create(Response.notImplementedCommand, serverData.currentCommand.split(" ", 2)[0].toUpperCase())); }

    /*
        Method for TYPE command: changes the type of representation used by the server for file transmission.
        Input:
          1. the data type sent by the client("I", "A", "L 8", "A N").
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput executeTYPE(String transferDataType)
    {
        if(transferDataType == null || !transferDataType.matches("(?i)(I|A|L 8|A N)"))
            return new CommandOutput(Response.create(Response.unrecognizedParameter, "TYPE"));

        if(!serverData.isUserLoggedIn)
            return new CommandOutput(Response.create(Response.notLoggedIn, "TYPE", "You need to login"));

        serverData.transferDataType = transferDataType;
        return new CommandOutput(Response.create(Response.genericSuccess, "TYPE", "Switched to type " + transferDataType.toUpperCase()));
    }

    /*
        Method for USER command: changes the user used by the client.
        Input:
          1. the username sent by the client.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
        Reads from the users file and sets all user information from it.
    */
    private CommandOutput executeUSER(String username)
    {
        UserInformation userInfo;
        File userRootDirectoryObj;
        String savedCurrentCommand;

        if(username == null)
            return new CommandOutput(Response.create(Response.unrecognizedParameter, "USER"));

        savedCurrentCommand = serverData.currentCommand;
        serverData.currentCommand = "";

        try
        {
            userInfo = Utils.getUserInformation(serverData.userlistFile, username);

            if(userInfo.userNotFound)
              return new CommandOutput(Response.create(Response.notLoggedIn, "USER", "User not found"));

            userRootDirectoryObj = new File(serverData.serverRootDirectory + userInfo.rootDirectory);

            if(!userRootDirectoryObj.exists() || userRootDirectoryObj.isFile())
              return new CommandOutput
                (
                    Response.create(Response.genericTmpError, "USER"),
                    LogMessage.create(LogMessage.userRootDirectoryError, serverData.userAddress, username)
                );

            serverData.isUserLoggedIn = false;
            serverData.username = username;
            serverData.userPassword = userInfo.password;
            serverData.listPermission = userInfo.listPermission;
            serverData.readPermission = userInfo.readPermission;
            serverData.writePermission = userInfo.writePermission;
            serverData.deletePermission = userInfo.deletePermission;
            serverData.renamePermission = userInfo.renamePermission;
            serverData.seeHidenFilePermission = userInfo.seeHidenFilePermission;
            serverData.userRootDirectory = userInfo.rootDirectory;
            serverData.userWorkingDirectory = userInfo.rootDirectory;

            serverData.currentCommand = savedCurrentCommand;
            return new CommandOutput(Response.create(Response.usernameFound), LogMessage.create(LogMessage.userFound, serverData.userAddress, username));

        }
        catch(FileNotFoundException excp)
        { return new CommandOutput(Response.create(Response.genericTmpError, "USER"), LogMessage.create(LogMessage.userlistFileNotFound, serverData.userAddress)); }
        catch(IOException excp)
        { return new CommandOutput(Response.create(Response.genericTmpError, "USER"), LogMessage.create(LogMessage.userlistReadError, serverData.userAddress)); }
        catch(ArrayIndexOutOfBoundsException excp)
        { return new CommandOutput(Response.create(Response.genericTmpError, "USER"), LogMessage.create(LogMessage.userlistFileIncomplete, serverData.userAddress, username)); }
    }

    /*
        Method for PASS command: check if the password entered by the client is correct.
        Input:
          1. the password sent by the client.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput executePASS(String userPassword)
    {
        if(userPassword == null)
          return new CommandOutput(Response.create(Response.unrecognizedParameter, "PASS"));

        if(!serverData.lastCommand.equals("USER"))
          return new CommandOutput(Response.create(Response.wrongOrder, "PASS", "USER"));

        if((serverData.isUserLoggedIn = userPassword.equals(serverData.userPassword)))
          return new CommandOutput(Response.create(Response.loggedIn), LogMessage.create(LogMessage.correctPassword, serverData.userAddress, serverData.username));

        return new CommandOutput(Response.create(Response.notLoggedIn, "PASS", "Incorrect password"));
    }

    /*
        Method for change working directory: change the client working directory.
        Input:
          1. the directory sent by the client.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
        Implements the CDUP and CWD commanda.
    */
    private CommandOutput changeDirectory(String pathname)
    {
        File pathnameObj;
        String command = serverData.currentCommand.split(" ", 2)[0].toUpperCase();

        if(!serverData.isUserLoggedIn)
          return new CommandOutput(Response.create(Response.notLoggedIn, command, "You need to login"));

        if(pathname == null)
        {
            if(command.equals("CWD"))
              return new CommandOutput(Response.create(Response.unrecognizedParameter, "CWD"));

            pathname = "..";
        }

        pathname = Utils.getCorrectPath(serverData.userRootDirectory, serverData.userWorkingDirectory, pathname);
        pathnameObj = new File(serverData.serverRootDirectory + pathname);

        if(!pathnameObj.exists() || pathnameObj.isFile())
          return new CommandOutput(Response.create(Response.actionNotTaken, command, "Unable to move to the \"" + pathname + "\" directory"));

        serverData.userWorkingDirectory = pathname;
        return new CommandOutput(Response.create(Response.actionTaken, command, "Moved to \"" + pathname + "\""));
    }

    /*
        Method for REIN command: reinitialize the FTP server.
        The parameter is superfluous and set to null. It's just there to fit into the FunctionPointer interface.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput executeREIN(String parameters)
    {
        serverData.transferDataType = "A";
        serverData.currentCommand = "";
        serverData.isUserLoggedIn = false;
        serverData.isServerModeSet = false;
        return new CommandOutput(Response.create(Response.readyForUser, " Reinitialized session"));
    }

    /*
        Method for QUIT command: disconnects the client from the FTP server.
        The parameter is superfluous and set to null. It's just there to fit into the FunctionPointer interface.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput executeQUIT(String parameters)
    { return new CommandOutput(Response.create(Response.closeConnection, "Goodbye!"), LogMessage.create(LogMessage.clientDisconnected, serverData.userAddress)); }

    /*
        Method for PORT command: puts the server into active mode.
        Input:
          1. the ip and the port sent by the client.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput executePORT(String userIPPort)
    {
        String[] portTokens;

        if(!serverData.isUserLoggedIn)
          return new CommandOutput(Response.create(Response.notLoggedIn, "PORT", "You need to login"));

        if(userIPPort == null)
          return new CommandOutput(Response.create(Response.unrecognizedParameter, "PORT"));

        portTokens = userIPPort.split(",");

        try
        {
            serverData.ipToConnectTo = portTokens[0] + "." + portTokens[1] + "." + portTokens[2] + "." + portTokens[3];
            serverData.portToConnectTo = Integer.parseInt(portTokens[4]) * 256 + Integer.parseInt(portTokens[5]);
            serverData.isServerModeSet = true;
            serverData.serverMode = false;
        }
        catch(ArrayIndexOutOfBoundsException excp)
        { return new CommandOutput(Response.create(Response.unrecognizedParameter, "PORT")); }

        return new CommandOutput(Response.create(Response.genericSuccess, "PORT", "Port " + serverData.portToConnectTo + " will be used"));
    }

    /*
        Method for PASV command: puts the server into passive mode.
        The parameter is superfluous and set to null. It's just there to fit into the FunctionPointer interface.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput executePASV(String parameters)
    {
        String serverIpToSend;
        int generatedPort;

        if(!serverData.isUserLoggedIn)
          return new CommandOutput(Response.create(Response.notLoggedIn, "PASV", "You need to login"));

        try
        {
            if(serverData.socketToListenTo != null && !serverData.socketToListenTo.isClosed())
              serverData.socketToListenTo.close();

            serverData.socketToListenTo = new ServerSocket(0);
            serverData.socketToListenTo.setSoTimeout(serverData.dataAcceptTimeout);
            generatedPort = serverData.socketToListenTo.getLocalPort();
            serverIpToSend = serverData.serverIp.replace('.', ',');
            serverData.isServerModeSet = true;
            serverData.serverMode = true;
            return new CommandOutput(Response.create(Response.enteringPassiveMode, serverIpToSend + "," + generatedPort / 256 + "," + generatedPort % 256));

        }
        catch(IOException excp)
        { return new CommandOutput(Response.create(Response.genericError, "PASV", "Unable to enter passive mode")); }
    }

    /*
        Method for STRU command: changes the file structure used by the FTP server for data transmission.
        This server supports file structure only. Then it simply returns the response code to the client.
        The parameter is superfluous and set to null. It's just there to fit into the FunctionPointer interface.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput executeSTRU(String parameters)
    {
        if(!serverData.isUserLoggedIn)
          return new CommandOutput(Response.create(Response.notLoggedIn, "STRU", "You need to login"));

        return new CommandOutput(Response.create(Response.genericSuccess, "STRU", "Structure changed to F"));
    }

    /*
        Method for MODE command: changes the file transfer mode.
        This server supports stream mode only. Then it simply returns the response code to the client.
        The parameter is superfluous and set to null. It's just there to fit into the FunctionPointer interface.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput executeMODE(String parameters)
    {
        if(!serverData.isUserLoggedIn)
          return new CommandOutput(Response.create(Response.notLoggedIn, "MODE", "You need to login"));

        return new CommandOutput(Response.create(Response.genericSuccess, "MODE", "Mode changed to S"));
    }

    /*
        Method for RETR command: allows the client to download files from the FTP server.
        Input:
          1. the file pathname sent by the client.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput executeRETR(String pathname)
    {
        File pathnameObj;
        Socket dataConnection;
        String[] logParameters = {serverData.userAddress, serverData.username, "RETR", ""};

        if(!serverData.isUserLoggedIn)
          return new CommandOutput(Response.create(Response.notLoggedIn, "RETR", "You need to log in"));

        if(pathname == null)
          return new CommandOutput(Response.create(Response.unrecognizedParameter, "RETR"));

        if(!serverData.readPermission)
          return new CommandOutput(Response.create(Response.actionNotTaken, "RETR",  "Permission denied"));

        if(!serverData.isServerModeSet)
          return new CommandOutput(Response.create(Response.unableToOpenDataConnection, "RETR", "Use PORT or PASV first"));

        logParameters[3] = pathname = Utils.getCorrectPath(serverData.userRootDirectory, serverData.userWorkingDirectory, pathname);
        pathnameObj = new File(serverData.serverRootDirectory + pathname);

        if(!pathnameObj.exists() || !pathnameObj.isFile())
          return new CommandOutput(Response.create(Response.actionNotTaken, "RETR", "Unable to read the file"));

        try
        {
            if(!serverData.serverMode)
              dataConnection = new Socket(serverData.ipToConnectTo, serverData.portToConnectTo);

            else
              dataConnection = serverData.socketToListenTo.accept();

            dataConnection.setSoTimeout(serverData.dataRcvTimeout);
        }
        catch(IOException excp)
        {
            return new CommandOutput
              (
                  Response.create(Response.unableToOpenDataConnection, "RETR", "Can't open data connection"),
                  LogMessage.create(LogMessage.unableToDownloadFile, logParameters)
              );
        }

        serverData.controlConnectionOutput.println(Response.create(Response.openDataConnection, "RETR"));

        try
        {
            Utils.transferData(new FileInputStream(pathnameObj), dataConnection.getOutputStream(), serverData.transferDataType);
            dataConnection.close();
        }
        catch(IOException excp)
        { return new CommandOutput(Response.create(Response.transferAborted, "RETR"), LogMessage.create(LogMessage.unableToDownloadFile, logParameters)); }

        return new CommandOutput(Response.create(Response.transferSuccess, "RETR"), LogMessage.create(LogMessage.fileDownloaded, logParameters));
    }

    /*
        Allows the client to upload a file to the FTP server.
        Input:
          the pathname with which to store the file sent by the client.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
        Implements STOR, APPE and STOU command.
    */
    private CommandOutput storeFile(String pathname)
    {
        File pathnameObj, parentPathnameObj;
        Socket dataConnection;
        String command = serverData.currentCommand.split(" ", 2)[0].toUpperCase();
        String[] logParameters = {serverData.userAddress, serverData.username, command, ""};
        boolean isAppendEnable = command.equals("APPE");
        int counter = 0;

        if(!serverData.isUserLoggedIn)
          return new CommandOutput(Response.create(Response.notLoggedIn, command, "You need to log in"));

        if(pathname == null)
          return new CommandOutput(Response.create(Response.unrecognizedParameter, command));

        if(!serverData.writePermission)
          return new CommandOutput(Response.create(Response.actionNotTaken, command, "Permission denied"));

        if(!serverData.isServerModeSet)
          return new CommandOutput(Response.create(Response.unableToOpenDataConnection, command, "Use PORT or PASV first"));

        logParameters[3] = pathname = Utils.getCorrectPath(serverData.userRootDirectory, serverData.userWorkingDirectory, pathname);
        pathnameObj = new File(serverData.serverRootDirectory + pathname);
        parentPathnameObj = new File(pathnameObj.getParent());

        parentPathnameObj.mkdirs();

        if(pathnameObj.exists() && !pathnameObj.isFile() && !command.equals("STOU"))
            return new CommandOutput(Response.create(Response.actionNotTaken, command, "Cannot overwrite a directory"));

        try
        {
            if(command.equals("STOU"))
            {
                while(pathnameObj.exists())
                  pathnameObj = new File(parentPathnameObj, pathnameObj.getName() + "(" + (counter++) + ")");
            }

            pathnameObj.createNewFile();
        }
        catch(IOException excp)
        { return new CommandOutput(Response.create(Response.tmpActionNotTaken, command, "Unable to create \"" + pathnameObj.getName() + "\" file")); }

        pathname = pathnameObj.toString();
        logParameters[3] = pathname;

        try
        {
            if(!serverData.serverMode)
              dataConnection = new Socket(serverData.ipToConnectTo, serverData.portToConnectTo);

            else
              dataConnection = serverData.socketToListenTo.accept();

            dataConnection.setSoTimeout(serverData.dataRcvTimeout);
        }
        catch(IOException excp)
        {
            return new CommandOutput
              (
                  Response.create(Response.unableToOpenDataConnection, command, "Can't open data connection"),
                  LogMessage.create(LogMessage.unableToUploadFile, logParameters)
              );
        }

        serverData.controlConnectionOutput.println(Response.create(Response.openDataConnection, command));

        try
        {
            Utils.transferData(dataConnection.getInputStream(), new FileOutputStream(pathnameObj, isAppendEnable), serverData.transferDataType);
            dataConnection.close();
        }
        catch(IOException excp)
        { return new CommandOutput(Response.create(Response.transferAborted, command), LogMessage.create(LogMessage.unableToUploadFile, logParameters)); }

        return new CommandOutput(Response.create(Response.transferSuccess, command), LogMessage.create(LogMessage.fileUploaded, logParameters));
    }

    /*
        Method from RNFR command: allows the client to select the file to rename.
        Input:
          1. the pathname of the file to rename sent by the client.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput executeRNFR(String pathname)
    {
        File pathnameObj;
        String savedCurrentCommand;

        savedCurrentCommand = serverData.currentCommand;
        serverData.currentCommand = "";

        if(!serverData.isUserLoggedIn)
          return new CommandOutput(Response.create(Response.notLoggedIn, "RNFR", "You need to log in"));

        if(pathname == null)
          return new CommandOutput(Response.create(Response.unrecognizedParameter, "RNFR"));

        if(!serverData.renamePermission)
          return new CommandOutput(Response.create(Response.actionNotTaken, "RNFR", "Permission denied"));

        pathname = Utils.getCorrectPath(serverData.userRootDirectory, serverData.userWorkingDirectory, pathname);
        pathnameObj = new File(serverData.serverRootDirectory + pathname);

        if(!pathnameObj.exists() || !pathnameObj.isFile())
          return new CommandOutput(Response.create(Response.actionNotTaken, "RNFR", "Unable to find the specified file"));

        serverData.currentCommand = savedCurrentCommand;
        serverData.fileToRename = pathname;
        return new CommandOutput(Response.create(Response.waitMoreCommand, "RNFR", "RNTO"));
    }

    /*
        Method for RNTO command: allows the client to actually rename a file.
        Input:
          1. new file name sent by the client.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput executeRNTO(String newFilename)
    {
        File fileToRenameObj;
        File newFilenameObj;
        String[] logParameters = {serverData.userAddress, serverData.username, "", ""};

        if(newFilename == null)
          return new CommandOutput(Response.create(Response.unrecognizedParameter, "RNTO"));

        if(!serverData.lastCommand.equals("RNFR"))
          return new CommandOutput(Response.create(Response.wrongOrder, "RNTO", "RNFR"));

        if(newFilename.contains("/"))
          return new CommandOutput(Response.create(Response.fileNameNotAllowed, "RNTO"));

        fileToRenameObj = new File(serverData.serverRootDirectory + serverData.fileToRename);
        newFilenameObj = new File(serverData.serverRootDirectory + Paths.get(serverData.fileToRename).getParent().toString() + '/' + newFilename);
        fileToRenameObj.renameTo(newFilenameObj);
        logParameters[2] = fileToRenameObj.toString(); logParameters[3] = newFilenameObj.toString();
        return new CommandOutput(Response.create(Response.actionTaken, "RNTO", "File renamed"), LogMessage.create(LogMessage.fileRenamed, logParameters));
    }

    /*
        Delete a file or a directory
        Input:
          1. the pathname of the file to delete sent by the client.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
        Implements the RMD and DELE command
    */
    private CommandOutput removeFile(String pathname)
    {
        File pathnameObj;
        String command = serverData.currentCommand.split(" ", 2)[0].toUpperCase();
        String type = ((command.equals("DELE")) ? "file" : "directory");
        String[] logParameters = {serverData.userAddress, serverData.username, command, "", type};

        if(!serverData.isUserLoggedIn)
          return new CommandOutput(Response.create(Response.notLoggedIn, command, "You need to log in"));

        if(pathname == null)
          return new CommandOutput(Response.create(Response.unrecognizedParameter, command));

        if(!serverData.deletePermission)
          return new CommandOutput(Response.create(Response.actionNotTaken, command, "Permission denied"));

        logParameters[3] = pathname = Utils.getCorrectPath(serverData.userRootDirectory, serverData.userWorkingDirectory, pathname);
        pathnameObj = new File(serverData.serverRootDirectory + pathname);

        if(!pathnameObj.exists() || (command.equals("DELE") && !pathnameObj.isFile()) || (command.equals("RMD") && pathnameObj.isFile()))
          return new CommandOutput(Response.create(Response.actionNotTaken, command, "Unable to find the specified " + type));

        if(!pathname.equals(serverData.userRootDirectory) && pathnameObj.delete())
          return new CommandOutput
            (
                Response.create(Response.actionTaken, command, pathname + " " + type + " deleted"),
                LogMessage.create(LogMessage.fileDeleted, logParameters)
            );

        return new CommandOutput(Response.create(Response.tmpActionNotTaken, command, "Unable to delete the specified " + type));

    }

    /*
        File list method: sends the list of files in a directory to the client.
        Input:
          1. the pathname of the directory sent by the client.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
        Implements the NLST and LIST commands.
    */
    private CommandOutput listFile(String pathname)
    {
        File pathnameObj;
        Socket dataConnection;
        String[] filenameArray;
        String fileInfo = "", command = serverData.currentCommand.split(" ", 2)[0].toUpperCase();

        if(!serverData.isUserLoggedIn)
          return new CommandOutput(Response.create(Response.notLoggedIn, command, "You need to log in"));

        if(pathname == null)
          pathname = ".";

        if(!serverData.listPermission)
          return new CommandOutput(Response.create(Response.actionNotTaken, command, "Permission denied"));

        if(!serverData.isServerModeSet)
          return new CommandOutput(Response.create(Response.unableToOpenDataConnection, command, "Use PORT or PASV first"));

        pathname = Utils.getCorrectPath(serverData.userRootDirectory, serverData.userWorkingDirectory, pathname);
        pathnameObj = new File(serverData.serverRootDirectory + pathname);

        if(!pathnameObj.exists())
          return new CommandOutput(Response.create(Response.actionNotTaken, command, "Directory not found"));

        try
        {
            if(!serverData.serverMode)
                dataConnection = new Socket(serverData.ipToConnectTo, serverData.portToConnectTo);

            else
                dataConnection = serverData.socketToListenTo.accept();

            dataConnection.setSoTimeout(serverData.dataRcvTimeout);
        }
        catch(IOException excp)
        { return new CommandOutput(Response.create(Response.unableToOpenDataConnection, command, "Can't open data connection")); }

        if(pathnameObj.isFile())
        {
            filenameArray = new String[1];
            filenameArray[0] = "";
        }
        else if((filenameArray = pathnameObj.list()) == null)
        { return new CommandOutput(Response.create(Response.genericTmpError, command)); }

        serverData.controlConnectionOutput.println(Response.create(Response.openDataConnection, command));

        try
        {
            for(String fileName : filenameArray)
            {
                if(!serverData.seeHidenFilePermission && fileName.charAt(0) == '.')
                  continue;

                fileInfo += Utils.getFileAttributes(serverData.serverRootDirectory + pathname + '/' + fileName, command);
            }

            Utils.transferData(new ByteArrayInputStream(fileInfo.getBytes()), dataConnection.getOutputStream(), "A");
            dataConnection.close();
        }
        catch(IOException excp)
        { return new CommandOutput(Response.create(Response.transferAborted, command)); }

        return new CommandOutput(Response.create(Response.transferSuccess, command));
    }

    /*
        Method for MKD command: create a directory.
        Input:
          1. the pathname of the new directory sent by the client.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput executeMKD(String pathname)
    {
        String[] logParameters = {serverData.userAddress, serverData.username, ""};

        if(!serverData.isUserLoggedIn)
          return new CommandOutput(Response.create(Response.notLoggedIn, "MKD", "You need to log in"));

        if(pathname == null)
          return new CommandOutput(Response.create(Response.unrecognizedParameter, "MKD"));

        if(!serverData.writePermission)
          return new CommandOutput(Response.create(Response.actionNotTaken, "MKD", "Permission denied"));

        logParameters[2] = pathname = Utils.getCorrectPath(serverData.userRootDirectory, serverData.userWorkingDirectory, pathname);

        if(!new File(serverData.serverRootDirectory + pathname).mkdirs())
          return new CommandOutput(Response.create(Response.actionNotTaken, "MKD", "Unable to create directory"));

        return new CommandOutput
          (
              Response.create(Response.directorySuccess, "MKD", "Directory created"),
              LogMessage.create(LogMessage.directoryCreated, logParameters)
          );
    }

    /*
        Method for PWD command: sends the client current working directory to the client.
        The parameter is superfluous and set to null. It's just there to fit into the FunctionPointer interface.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput executePWD(String parameters)
    {
        Path realWorkingDirectory;

        if(!serverData.isUserLoggedIn)
          return new CommandOutput(Response.create(Response.notLoggedIn, "PWD", "You need to log in"));

        realWorkingDirectory = Paths.get("/" + ((serverData.userWorkingDirectory + "/").replaceFirst(serverData.userRootDirectory, ""))).normalize();
        return new CommandOutput
          (Response.create(Response.directorySuccess, "\"" +  realWorkingDirectory.toString() + "\"; PWD", "Current directory printed"));
    }

    /*
        Method for SYST command: sends the client information about the system on which the FTP server is hosted.
        The parameter is superfluous and set to null. It's just there to fit into the FunctionPointer interface.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput executeSYST(String parameters)
    { return new CommandOutput(Response.create(Response.systemInfo)); }

    /*
        Method for HELP command: sends the list of commands supported by the FTP server to the client.
        The parameter is superfluous and set to null. It's just there to fit into the FunctionPointer interface.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput executeHELP(String parameters)
    { return new CommandOutput(Response.create(Response.helpOK)); }

    /*
        Function fot NOOP command: no operation.
        The parameter is superfluous and set to null. It's just there to fit into the FunctionPointer interface.
        Return values:
          1. an instance of the CommandOutput class will contain the response for the client and the log message
    */
    private CommandOutput executeNOOP(String parameters)
    { return new CommandOutput(Response.create(Response.genericSuccess, "NOOP", "No operation done")); }

    //Method for initializing hashmap of function pointers.
    private void initFunctionArray()
    {
        commandsArray = new HashMap<String, FunctionPointer>();

        commandsArray.put("TYPE", this::executeTYPE);     commandsArray.put("USER", this::executeUSER);
        commandsArray.put("PASS", this::executePASS);     commandsArray.put("CWD",  this::changeDirectory);
        commandsArray.put("CDUP", this::changeDirectory); commandsArray.put("REIN", this::executeREIN);
        commandsArray.put("QUIT", this::executeQUIT);     commandsArray.put("PORT", this::executePORT);
        commandsArray.put("PASV", this::executePASV);     commandsArray.put("STRU", this::executeSTRU);
        commandsArray.put("MODE", this::executeMODE);     commandsArray.put("RETR", this::executeRETR);
        commandsArray.put("STOR", this::storeFile);       commandsArray.put("STOU", this::storeFile);
        commandsArray.put("APPE", this::storeFile);       commandsArray.put("RNFR", this::executeRNFR);
        commandsArray.put("RNTO", this::executeRNTO);     commandsArray.put("DELE", this::removeFile);
        commandsArray.put("LIST", this::listFile);        commandsArray.put("NLST", this::listFile);
        commandsArray.put("RMD",  this::removeFile);      commandsArray.put("MKD",  this::executeMKD);
        commandsArray.put("PWD",  this::executePWD);      commandsArray.put("SYST", this::executeSYST);
        commandsArray.put("HELP", this::executeHELP);     commandsArray.put("NOOP", this::executeNOOP);

        commandsArray.put("ALLO", this::superfluousCommand);
        commandsArray.put("SITE", this::superfluousCommand);
        commandsArray.put("ACCT", this::superfluousCommand);

        commandsArray.put("SMNT", this::notImplementedCommand); commandsArray.put("REST", this::notImplementedCommand);
        commandsArray.put("ABOR", this::notImplementedCommand); commandsArray.put("STAT", this::notImplementedCommand);
        commandsArray.put("ADAT", this::notImplementedCommand); commandsArray.put("AUTH", this::notImplementedCommand);
        commandsArray.put("AVBL", this::notImplementedCommand); commandsArray.put("CCC",  this::notImplementedCommand);
        commandsArray.put("CONF", this::notImplementedCommand); commandsArray.put("CSID", this::notImplementedCommand);
        commandsArray.put("DSIZ", this::notImplementedCommand); commandsArray.put("ENC",  this::notImplementedCommand);
        commandsArray.put("EPRT", this::notImplementedCommand); commandsArray.put("EPSV", this::notImplementedCommand);
        commandsArray.put("FEAT", this::notImplementedCommand); commandsArray.put("HOST", this::notImplementedCommand);
        commandsArray.put("LANG", this::notImplementedCommand); commandsArray.put("LPRT", this::notImplementedCommand);
        commandsArray.put("LPSV", this::notImplementedCommand); commandsArray.put("MDTM", this::notImplementedCommand);
        commandsArray.put("MFCT", this::notImplementedCommand); commandsArray.put("MFMT", this::notImplementedCommand);
        commandsArray.put("MFF",  this::notImplementedCommand); commandsArray.put("MIC",  this::notImplementedCommand);
        commandsArray.put("MLSD", this::notImplementedCommand); commandsArray.put("MLST", this::notImplementedCommand);
        commandsArray.put("OPTS", this::notImplementedCommand); commandsArray.put("PBSZ", this::notImplementedCommand);
        commandsArray.put("PROT", this::notImplementedCommand); commandsArray.put("RMDA", this::notImplementedCommand);
        commandsArray.put("SIZE", this::notImplementedCommand); commandsArray.put("SPSV", this::notImplementedCommand);
        commandsArray.put("THMB", this::notImplementedCommand); commandsArray.put("XCUP", this::notImplementedCommand);
        commandsArray.put("XMKD", this::notImplementedCommand); commandsArray.put("XPWD", this::notImplementedCommand);
        commandsArray.put("XRMD", this::notImplementedCommand); commandsArray.put("XRCP", this::notImplementedCommand);
        commandsArray.put("XRSQ", this::notImplementedCommand); commandsArray.put("XSEM", this::notImplementedCommand);
        commandsArray.put("XSEN", this::notImplementedCommand);
    }
}
