# JFTP
JFTP, acronym for **J**ava **F**ile **T**ransfer **P**rotocol (I'm very imaginative), is an ftp server completely developed in java. Meets the minimum implementation defined in the [RFC 959](https://datatracker.ietf.org/doc/html/rfc959) specification. However, the server was designed as a "modern" ftp server. In fact, it does not support obsolete data representation types (such as EBCDIC), and data transfer techniques that make no sense for modern personal computers. 
It is therefore a modern ftp server designed to be used for transmitting files only in ascii or binary mode, using stream mode, and transferring the files as they are.
The server is also multithreaded, thus supporting use by multiple users at the same time.
## Source analysis
The program consists of eight modules each designed for a specific purpose:
- **Utils.java**: it is the module containing the general utility methods, such as the one for reading the server configuration from the appropriate file, or the one for transferring data.
- **JFtpServer.java**: it is the module that reads the server configuration from the appropriate file (via the method defined in the Utils.java module) and checks its correctness. If the check is successful it will establish the control connection with the clients and create a thread for each of these.
- **JFtpWorker.java**: it is the module that contains the method executed by the threads created by the JFtpServer.java module. It contains all the information about the connection between client and server, and executes the commands it receives from the client via the executeCommand() method defined in the CommandHandler module. This module is the only one to send and receive data via control connection (except for the response code 150 which is sent by the CommandHandler module).
- **CommandHandler.java**: it is the module that executes the commands received from the client. It defines a method for each command, or group of commands, and executes them using this methods. The function to be executed is established through the use of a hash table (in this case HashMap) containing the <Name of the instruction, pointer to the appropriate function> pairs.
- **CreateMessage.java**: this module is used to create error and log messages, and responses for the client depending on the context. Starting from the general strings, create specific ones. It is used to share the methods needed to do this between the ErrorMessage.java, LogMessage.java, and Response.java methods.
- **ErrorMessage.java**: this module is used to create ftp server error messages. It makes use of the CreateMessage.java module.
- **LogMessage.java**: this module is used to create ftp server log messages. It makes use of the CreateMessage.java module.
- **Response.java**: this module is used to create ftp server response messages. It makes use of the CreateMessage.java module.
## Server configuration
The server configuration is carried out via the following files:
- **The server configuration file**: which contains all the configuration information about the server, such as IP, port, root directory etc.
- **The user list file**: which contains user configuration information, such as name, password, permissions etc.
### Configuration example
Here follows an example of server configuration:
``` 
# IP address of the JFTP server.
serverip = 192.168.1.118
# JFTP server root directory.
rootdirectory = /
# User list file.
userlistfile = ../userlist.txt
# Port on which the JFTP server will listen. Must be between 0 and 65535.
listenport = 21
# Control connection timed out.
controltimeout = 300000
# Timeout for establishing data connection.
dataaccepttimeout = 300000
# Timeout for receiving data from the data connection.
datarcvtimeout = 300000
```
Here the server is configured so that its IP address is 192.168.1.118, the port on which it will listen is 21, the root directory it will use "/" (i.e. the root directory), the file list "../ userlist.txt " and the various timeouts set to 5 minutes.
Here is an example of user configuration:
```
admin admin l-r-w-re-h-d /
anonymous anonymous l-r /
```
Two users are created: the admin with the password "admin", all permissions, and the root directory equal to the root directory of the server.
And anonymous, with the password "anonymous", only list and read permissions, and the root directory equal to the server root directory.
In general the format for defining user is as follows:
```
<username> <password> <permission> <root directory>
```
The permissions are:
```
l:  for permission to list files.
r:  for read permission from the ftp server.
w:  for write permission on the ftp server.
re: for file renaming permission.
h:  for permission to see hidden files with file listing.
d:  for permission to delete files and directories.
```
Permissions must be divided by a "-".
## Security
The server implements the ftp protocol, not the ftps or sftp protocol. Therefore it is not secure: files and passwords are transmitted in clear text and no mechanism is implemented to verify the integrity of messages and authenticate the server.
## Execution
To compile the program, run the make file. Type this command from the terminal:
``` 
make
```
To run the server, move to the ByteCodes directory and execute the following command:
```
sudo java JFtpServer.java <configuration file pathname>
```
Where ```<configuration file pathname>``` is the name of the configuration file that will be used by the server. If the port set in the configuration file is greater than 1023, you can omit the ```sudo``` command.

