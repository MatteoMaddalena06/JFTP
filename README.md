# JFTP
JFTP, acronym for **J**ava **F**ile **T**ransfer **P**rotocol (I'm very imaginative), is an ftp server completely developed in java. Meets the minimum implementation defined in the [RFC 959](https://datatracker.ietf.org/doc/html/rfc959) specification. However, the server was designed as a "modern" ftp server. In fact, it does not support obsolete data representation types (such as EBCDIC), and data transfer techniques that make no sense for modern personal computers. 
It is therefore a modern ftp server designed to be used for transmitting files only in ascii or binary mode, using stream mode, and transferring the files as they are.
The server is also multithreaded, thus supporting use by multiple users at the same time.
## Source analysis
The program consists of eight modules each designed for a specific purpose:
- **Utils.java**: it is the module containing the general utility methods, such as the one for reading the server configuration from the appropriate file, or the one for transferring data.
- **JFtpServer.java**: it is the module that reads the server configuration from the appropriate file (via the method defined in the Utils.java module) and checks its correctness. If the check is successful it will establish the control connection with the clients and create a thread for each of these.
- **JFtpWorker.java**: it is the module that contains the method executed by the threads created by the JFtpServer.java module. It contains all the information about the connection between client and server, and executes the commands it receives from the client via the executeCommand() method defined in the CommandHandler module. This module is the only one to send and receive data via control connection (except for the response code 150 which is sent by the CommandHandler module).
- **CommandHandler.java**: it is the module that executes the commands received from the client. It defines a method for each command, or group of commands, and executes them using the methods. The function to be executed is established through the use of a hash table (in this case HashMap) containing the <Name of the instruction, pointer to the appropriate function> pairs.

