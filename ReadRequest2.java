package lab11;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * A basic webserver that reads and send files to the browser
 * and shows error pages in case something went wrong.
 *
 * @Author Thorben Dhaenens
 * @Author lab 12 from Eck, https://math.hws.edu/eck/cs225/f16/lab11/index.html
 */

public class ReadRequest2 {

//        private final static String rootDirectory = "<ADD PATHNAME>";
    private final static String rootDirectory = "src/lab11/www/rootDirectory";

    private static class ConnectionThread extends Thread {
        Socket connection;

        ConnectionThread(Socket connection) {
            this.connection = connection;
        }

        /**
         * runs one thread per connection.
         * Multiple connection will have multiple threads
         */
        public void run() {
            try {
                handleConnection(connection);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private final static int LISTENING_PORT = 8080;

    public static void main(String[] args) {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(LISTENING_PORT);
        } catch (Exception e) {
            System.out.println("Failed to create listening socket.");
            return;
        }
        System.out.println("Listening on port " + LISTENING_PORT);
        try {
            while (true) {
                Socket connection = serverSocket.accept();
                System.out.println("\nConnection from "
                        + connection.getRemoteSocketAddress());
                ConnectionThread thread = new ConnectionThread(connection);
                thread.start();
            }
        } catch (Exception e) {
            System.out.println("Server socket shut down unexpectedly!");
            System.out.println("Error: " + e);
            System.out.println("Exiting.");
        }
    }


    /**
     * This method will handle the in and outstream of the connection
     * once the initial connection is established.
     *
     * @param connection
     * @throws IOException
     */
    private static void handleConnection(Socket connection) throws IOException {
        String pathToFile;
        PrintWriter outWriter;
        Scanner inScanner;
        String clientInfo;
        try {
            inScanner = new Scanner(connection.getInputStream());
            outWriter = new PrintWriter(connection.getOutputStream());
            clientInfo = inScanner.nextLine();
            if (clientInfo.startsWith("GET")) {
                if (!clientInfo.contains("HTTP/1.1") && !clientInfo.contains("HTTP/1.0")) {
                    sendErrorResponse(400, connection.getOutputStream());
                } else {
                    pathToFile = clientInfo.replace("GET ", "").replace(" HTTP/1.1", "");
                    String[] splitLine = clientInfo.replace(" HTTP/1.1", "").replace("GET /", "").split("/");
                    String filename = splitLine[splitLine.length - 1];
                    File file = new File(rootDirectory + pathToFile);
                    String mimeType = getMimeType(filename);
                    if (file.isDirectory()) {
                        sendErrorResponse(403, connection.getOutputStream());
                    } else if (file.exists() && file.canRead()) {
                        outWriter.print("HTTP/1.1 200 OK" + "\r\n");
                        outWriter.print("Connection: close\r\n");
                        outWriter.print("Content-Type: " + mimeType + "\r\n");
                        outWriter.print("Content-Length: " + file.length() + "\r\n");
                        outWriter.print("\r\n");
                        outWriter.flush();
                        sendFile(file, connection.getOutputStream());
                    } else {
                        if (!file.exists()) {
                            sendErrorResponse(404, connection.getOutputStream());
                        } else if (file.exists() && !file.canRead()) {
                            sendErrorResponse(403, connection.getOutputStream());
                        } else {
                            sendErrorResponse(500, connection.getOutputStream());
                        }
                    }


                }
            } else {
                sendErrorResponse(501, connection.getOutputStream());
            }

        } catch (
                Exception e) {
            System.out.println("Error while communicating with client: " + e);
            try {
                sendErrorResponse(500, connection.getOutputStream());
                System.out.println("internal server error" + e);
            } catch (Exception ex) {
                System.out.println("'internal' internal server error" + e);
            }
        } finally {
            connection.close();
        }

    }

    /**
     * This method will determine the file type information for the browser
     * by looking at the extension of the file.
     *
     * @param fileName
     * @return
     */
    public static String getMimeType(String fileName) {
        int pos = fileName.lastIndexOf('.');
        if (pos < 0)  // no file extension in name
            return "x-application/x-unknown";
        String ext = fileName.substring(pos + 1).toLowerCase();
        return switch (ext) {
            case "txt" -> "text/plain";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "text/javascript";
            case "java" -> "text/x-java";
            case "jpeg", "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "ico" -> "image/x-icon";
            case "class" -> "application/java-vm";
            case "jar" -> "application/java-archive";
            case "zip" -> "application/zip";
            case "xml" -> "application/xml";
            case "xhtml" -> "application/xhtml+xml";
            default -> "x-application/x-unknown";
        };
        // Note:  x-application/x-unknown  is something made up;
        // it will probably make the browser offer to save the file.
    }

    /**
     * This method sends an error response to the browser in case there's no match
     *
     * @param errorCode 400,403,404,501 are defined, the others will return an internal server error
     * @param socketOut
     */
    private static void sendErrorResponse(int errorCode, OutputStream socketOut) {
        String statusInfo = " ";

        switch (errorCode) {
            case 400 -> statusInfo += "400 Bad Request";
            case 403 -> statusInfo += "403 Forbidden";
            case 404 -> statusInfo += "404 Not Found";
            case 501 -> statusInfo += "501 Not Implemented";
            default -> statusInfo += "500 Internal Server Error";
        }
        String errorHtml = "<style>*{\n" +
                "    transition: all 0.6s;\n" +
                "}\n" +
                "\n" +
                "html {\n" +
                "    height: 100%;\n" +
                "}\n" +
                "\n" +
                "body{\n" +
                "    font-family: 'Lato', sans-serif;\n" +
                "    color: #888;\n" +
                "    margin: 0;\n" +
                "}\n" +
                "\n" +
                "#main{\n" +
                "    display: table;\n" +
                "    width: 100%;\n" +
                "    height: 100vh;\n" +
                "    text-align: center;\n" +
                "}\n" +
                "\n" +
                ".fof{\n" +
                "\t  display: table-cell;\n" +
                "\t  vertical-align: middle;\n" +
                "}\n" +
                "\n" +
                ".fof h1{\n" +
                "\t  font-size: 50px;\n" +
                "\t  display: inline-block;\n" +
                "\t  padding-right: 12px;\n" +
                "\t  animation: type .5s alternate infinite;\n" +
                "}\n" +
                "\n" +
                "@keyframes type{\n" +
                "\t  from{box-shadow: inset -3px 0px 0px #888;}\n" +
                "\t  to{box-shadow: inset -3px 0px 0px transparent;}\n" +
                "}</style><div id=\"main\">\n" +
                "    \t<div class=\"fof\">\n" +
                "        \t\t<h1>" + statusInfo + "</h1>\n" +
                "    \t</div>\n" +
                "</div>";

        try {
            PrintWriter outPW = new PrintWriter(socketOut);
            outPW.print("HTTP/1.1" + statusInfo + "\r\n");
            outPW.print("Connection: close\r\n");
            outPW.print("Content-Type: text/html\r\n");
            outPW.print("\r\n");
            outPW.print(errorHtml + "\r\n");
            outPW.flush();
            outPW.close();
        } catch (Exception e) {
            System.out.println("'internal' internal server error");
        }
    }

    /**
     * Sends a file to the browser by transforming the characters to bits
     *
     * @param file
     * @param socketOut
     * @throws IOException
     */
    private static void sendFile(File file, OutputStream socketOut) throws
            IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        OutputStream out = new BufferedOutputStream(socketOut);
        while (true) {
            int x = in.read();
            if (x < 0)
                break;
            out.write(x);
        }
        out.flush();
    }

}
