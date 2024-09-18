README.md

# TftpServer

*TftpServer* is a console program that supports Trvial File Transfer.
This protocol is a **simplified** version of the TFTP Revision RFC-1350.
[TFTP RFC 1350](https://www.rfc-editor.org/rfc/rfc1350.txt)

## Usage

Firstly, the server must have a local directory containing a file to request.
This folder must be located in the same directory that the class files are in.

For example:
``	.
	├── TftpClient.class
	├── TftpServer.class
	├── TftpWorker.class
	└── server
	    ├── README.md
	    └── cat.txt
``
#### Server Side
In a terminal, enter:
`$ java TftpServer`

And the output should look something like:
`` 
Welcome to local TftpServer!
For the remote client, enter something like:
$ java TftpClient 127.0.0.1 cat.txt 
``

#### Client Side
On another machine's terminal, enter:
`java TftpClient <Server IP> <File Name>`

And the output should look something like:
`` 
Welcome, TftpClient!
	File Requested: cat.txt
	Download location: client/cat.txt
Waiting for download...
``

## Notes



