# File-Sender
This is a small Java project that I am currently working on. This program will allow you to access a data pool of files on a server through a client. The client should be able to authenticate using a password and send in requests to the server asking for data via the command line. The server should be able to validate those requests and submit the data back to the client.

Immidiately when the connection starts, the server and client will do a secure key exchange to make sure that they both have the correct AES key to speak to each other securely. Right after the key exchange, the server send a random string of length 10 to the client. If the client successfully responds to the challenge, which means that it sends back SHA256(SHA256(random_string_from_server) + SHA256(password)), the server will initiate a connection (and if the client does not respond with the correct answer, the server will ignore the client).

Image of the file-sender server running and receiving commands from the client:

![Image of server](https://preview.ibb.co/i4gjJe/server.png)


Image of a file-sender client running and sending commands to the server:

![Image of client](https://preview.ibb.co/cmNMWz/client.png)


Image of a server log file:

![Image of log file](https://preview.ibb.co/csr4jK/log.png)

## Usage
Since this program is written using the Eclipse IDE, both the source code and the compiled code are in two separate folders. Running this program is as easy as downloading or cloning this repository and running "java Start" in terminal while being inside the /bin/ directory. Modifying the source code is easy; you should be able to easily add this as a new project in eclipse, import it as another project in another Java IDE, or use your favorite text editor to edit the files under the /src/ directory.
