import java.net.*;
import java.io.*;

//Class for the various threads created by the FTP server.
public class JFtpWorker extends Thread
{
    //Ftp server information
    protected String         serverIp;
    protected Socket         controlConnection;
    protected BufferedReader controlConnectionInput;
    protected PrintWriter    controlConnectionOutput;
    protected boolean        isServerModeSet;  //true = set, flase = unset
    protected boolean        serverMode;       //true = passive, false = active
    protected String         ipToConnectTo;    //for active mode
    protected int            portToConnectTo;  //for active mode
    protected ServerSocket   socketToListenTo; //for passive mode
    protected String         lastCommand;
    protected String         currentCommand;
    protected String         transferDataType;
    protected String         serverRootDirectory;
    protected String         fileToRename;
    protected int            controlTimeout;
    protected int            dataAcceptTimeout;
    protected int            dataRcvTimeout;

    //Connected user information
    protected String         userlistFile;
    protected boolean        isUserLoggedIn;         //true = logged, false = not logged
    protected String         username;
    protected String         userPassword;
    protected boolean        listPermission;         //true = has permission, false = doesn't have permission
    protected boolean        readPermission;         //true = has permission, false = doesn't have permission
    protected boolean        writePermission;        //true = has permission, false = doesn't have permission
    protected boolean        deletePermission;       //true = has permission, false = doesn't have permission
    protected boolean        renamePermission;       //true = has permission, false = doesn't have permission
    protected boolean        seeHidenFilePermission; //true = has permission, false = doesn't have permission
    protected String         userWorkingDirectory;
    protected String         userRootDirectory;
    protected String         userAddress;

    public JFtpWorker(Socket controlConnection, ServerConfiguration serverConfiguration) throws IOException
    {
        this.controlConnection = controlConnection;
        controlConnectionInput = new BufferedReader(new InputStreamReader(controlConnection.getInputStream()));
        controlConnectionOutput = new PrintWriter(controlConnection.getOutputStream(), true);
        serverIp = serverConfiguration.serverIp;
        serverRootDirectory = serverConfiguration.rootDirectory;
        userlistFile = serverConfiguration.userlistFile;
        controlTimeout = serverConfiguration.controlTimeout;
        dataAcceptTimeout = serverConfiguration.dataAcceptTimeout;
        dataRcvTimeout = serverConfiguration.dataRcvTimeout;
        transferDataType = "A";
        isUserLoggedIn = false;
        isServerModeSet = false;
    }

    @Override
    public void run()
    {
        CommandOutput commandOutput;
        CommandHandler commandHandler = new CommandHandler(this);
        String userIp = ((InetSocketAddress)controlConnection.getRemoteSocketAddress()).getAddress().getHostAddress();
        int userPort = ((InetSocketAddress)controlConnection.getRemoteSocketAddress()).getPort();
        String command = "";

        userAddress = userIp + ":" + userPort;

        System.out.print(LogMessage.create(LogMessage.clientConnected, userAddress));
        controlConnectionOutput.println(Response.create(Response.readyForUser, Utils.welcomeMessage));

        try
        {
            controlConnection.setSoTimeout(controlTimeout);

            do
            {
                lastCommand = command;

                if((currentCommand = controlConnectionInput.readLine()) == null)
                {
                    System.out.print(LogMessage.create(LogMessage.clientDisconnected, userAddress));
                    break;
                }

                commandOutput = commandHandler.executeCommand();
                controlConnectionOutput.println(commandOutput.getResponse());
                System.out.print(commandOutput.getLog());

                command = currentCommand.split(" ", 2)[0].toUpperCase();
            }
            while(!command.equals("QUIT"));
        }
        catch(SocketTimeoutException excp)
        {
            controlConnectionOutput.println(Response.create(Response.closeConnection, "Client forcibly disconnected"));
            System.out.print(LogMessage.create(LogMessage.timeoutExpired, userAddress));
        }
        catch(SocketException excp)
        { System.out.print(LogMessage.create(LogMessage.timeoutError, userAddress)); }
        catch(IOException excp)
        { System.out.print(LogMessage.create(LogMessage.controlConnectionError, userAddress)); }

        try
        {
            controlConnection.close();
        }
        catch(IOException excp)
        { System.out.print(LogMessage.create(LogMessage.controlConnectionCloseError)); }
    }
}
