import java.net.*;
import java.io.*;

//FTP server main class.
public class JFtpServer
{
    public static void main(String[] args)
    {
        ServerConfiguration configuration;
        File serverRootDirectoryObj, userlistFileObj;
        ServerSocket listenSocket;

        if(args.length != 1)
        {
            System.out.print(ErrorMessage.create(ErrorMessage.incorrectUse));
            return;
        }

        try
        {
            configuration = Utils.getServerConfiguration(args[0]);
        }
        catch(FileNotFoundException excp)
        { System.out.print(ErrorMessage.create(ErrorMessage.configurationNotFound, args[0])); return; }
        catch(IOException excp)
        { System.out.print(ErrorMessage.create(ErrorMessage.unableToReadConfiguration, args[0])); return; }
        catch(ArrayIndexOutOfBoundsException excp)
        { System.out.print(ErrorMessage.create(ErrorMessage.notInitializedParameter, args[0])); return; }
        catch(NumberFormatException excp)
        { System.out.print(ErrorMessage.create(ErrorMessage.invalidParameterType, args[0])); return; }

        if(!configuration.isAllParametersEntered)
        {
            System.out.print(ErrorMessage.create(ErrorMessage.missingParameter, args[0]));
            return;
        }
        if(!configuration.serverIp.matches(Utils.IpAddressRegex))
        {
            System.out.print(ErrorMessage.create(ErrorMessage.invalidIpAddress, args[0]));
            return;
        }

        serverRootDirectoryObj = new File(configuration.rootDirectory);
        userlistFileObj = new File(configuration.userlistFile);

        if(!serverRootDirectoryObj.exists() || !userlistFileObj.exists())
        {
            System.out.print(ErrorMessage.create(ErrorMessage.invalidPathname, args[0]));
            return;
        }
        if(serverRootDirectoryObj.isFile())
        {
            System.out.print(ErrorMessage.create(ErrorMessage.rootDirectoryIsFile, args[0]));
            return;
        }
        if(!userlistFileObj.isFile())
        {
            System.out.print(ErrorMessage.create(ErrorMessage.userlistIsNotFile, args[0]));
            return;
        }

        if(configuration.controlTimeout < 0)
          configuration.controlTimeout = 0;

        if(configuration.dataAcceptTimeout < 0)
          configuration.dataAcceptTimeout = 0;

        if(configuration.dataRcvTimeout < 0)
          configuration.dataRcvTimeout = 0;

        try
        {
            listenSocket = new ServerSocket(configuration.listenPort);
        }
        catch(IllegalArgumentException excp)
        {
            System.out.print(ErrorMessage.create(ErrorMessage.invalidPortNumber, args[0]));
            return;
        }
        catch(IOException excp)
        { System.out.print(LogMessage.create(LogMessage.unableToStartServer)); return; }

        System.out.print
          (
              LogMessage.create(LogMessage.configurationInfo, configuration.serverIp, Integer.toString(configuration.listenPort),
              (configuration.controlTimeout == 0) ? "no timeout" : configuration.controlTimeout + "ms = " + "(" + configuration.controlTimeout/60000 + "m:" + (configuration.controlTimeout/1000)%60 + "s)",
              (configuration.dataAcceptTimeout == 0) ? "no timeout" : configuration.dataAcceptTimeout + "ms = " + "(" + configuration.dataAcceptTimeout/60000 + "m:" + (configuration.dataAcceptTimeout/1000)%60 + "s)",
              (configuration.dataRcvTimeout == 0) ? "no timeout" : configuration.dataRcvTimeout + "ms = " + "(" + configuration.dataRcvTimeout/60000 + "m:" + (configuration.dataRcvTimeout/1000)%60 + "s)",
              configuration.userlistFile, configuration.rootDirectory)
          );

        while(true)
        {
            try
            {
                new JFtpWorker(listenSocket.accept(), configuration).start();
            }
            catch(IOException excp)
            { System.out.print(LogMessage.create(LogMessage.unableToAcceptRequest)); }
        }

        //listenSocket.close();
    }
}
