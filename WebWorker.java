/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.*;

import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.util.TimeZone;



public class WebWorker implements Runnable
{

private Socket socket;
private String serverName;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
   serverName = "Danya's Server";
}
/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      String url = readHTTPRequest(is);
      File f = new File(url);
      if(f.exists()){
         //write appropriate MIME types to the header and serve the content
         if(url.endsWith(".html")){ 
            writeHTTPHeader(os,"text/html","200");
            writeHtmlContent(os, f);
         }
         else if(url.endsWith(".jpg") || url.endsWith(".jpeg")){
            writeHTTPHeader(os,"image/jpeg","200");
            //copy image bytes to output stream
            java.nio.file.Files.copy(f.toPath(), os);
         }
         else if(url.endsWith(".png")){
            writeHTTPHeader(os,"image/png","200");
            java.nio.file.Files.copy(f.toPath(), os);
         }
         else if(url.endsWith(".gif")){
            writeHTTPHeader(os,"image/gif","200");
            java.nio.file.Files.copy(f.toPath(), os);
         }
         else if(url.endsWith(".ico")){
            writeHTTPHeader(os,"image/x-icon","200");
            java.nio.file.Files.copy(f.toPath(), os);
         }

      }

      
      else{
         writeHTTPHeader(os,"text/html","404");
         os.write("404 Not Found".getBytes());
      }

      
      
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
//changed method to return requested filepath
private String readHTTPRequest(InputStream is)
{
   String line;
   String requestedPath = null;
   
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   while (true) {
      try {
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
         //string processing to extract file name and path from the request
         String[] requestLines = line.split(" ");
         if(requestedPath == null && requestLines[0].contains("GET")){
            //set filepath to the value after GET and remove '/' at the start
            requestedPath = requestLines[1].substring(1);
         }
         System.err.println("Request line: ("+line+")");
         if (line.length()==0) break;
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
   }
   return requestedPath;
}


/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
//changed method to take response code as parameter based on whether file exists or not
private void writeHTTPHeader(OutputStream os, String contentType, String responseCode) throws Exception
{
   
   os.write(("HTTP/1.1 " + responseCode + "\n").getBytes());
   os.write("Date: ".getBytes());
   os.write(getDateString().getBytes());
   os.write("\n".getBytes());
   os.write("Server: Jon's very own server\n".getBytes());
   //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   //os.write("Content-Length: 438\n".getBytes()); 
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}
//moved to own method for easier access to date from multiple methods
private String getDateString(){
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   return df.format(d);
}
/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
//changed method to take in the file to serve as a parameter
private void writeHtmlContent(OutputStream os, File fs) throws Exception
{
  //read html file content to a string and do the replacements
  
  String content = new Scanner(fs).useDelimiter("\\Z").next();
  content = content.replace("<cs371date>",getDateString());
  content = content.replace("<cs371server>",serverName);
  os.write(content.getBytes());
   
}

} // end class
