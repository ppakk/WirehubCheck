//package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;


public class WirehubCheck {

     
   //------------------------------------------------------------------------------------
   // This method sends an email as an alert.
   //
   public static void sendMail( String message, String mailServer ) throws MessagingException {

      boolean debug = false;

      //Set the host smtp address
      Properties props = new Properties();
      props.put("mail.smtp.host", "smtp.XYZ.com");

      // create some properties and get the default Session
      Session session = Session.getDefaultInstance(props, null);
      session.setDebug(debug);

      // create a message
      Message msg = new MimeMessage(session);

      // set the from and to address
      InternetAddress addressFrom = new InternetAddress("dev@XYZ.com");
      msg.setFrom(addressFrom);

      InternetAddress[] addressTo = new InternetAddress[1];
      addressTo[0] = new InternetAddress("XYZ@XYZ.com");

      msg.setRecipients(Message.RecipientType.TO, addressTo);

      // Setting the Subject and Content Type
      msg.setSubject("WP Wirehub count");
      msg.setContent(message, "text/plain");

      Transport.send(msg);
   }


   //------------------------------------------------------------------------------------
   // This client runs on a cron schedule. Sample cron setup to run every 60 minutes:
   // */60 * * * *   /home/lx/runWirehubCheck.sh
   //
   // When this runs a rest call to wp is made to see how many articles (posts) exist.
   // It compares this value to the number of articles from the previous run.
   // This value is stored in file:
   // "/tmp/lastWirehubTotal.txt"
   //
   public static void main(String args[]) throws IOException {
      // The wp rest url
      if (args == null || args.length != 1) {
         System.out.println("Usage: WirehubCheck http://XYZ.com/wp-json/wp/v2/posts");
         return;
      }
      String stringUrl = args[0];
      URL url = new URL(stringUrl);
      URLConnection uc = url.openConnection();

      uc.setRequestProperty("X-Requested-With", "Curl");

      // gets header page count
      String h1 = uc.getHeaderField("X-WP-Total");
      //System.out.println(h1);
      int newTotal = Integer.parseInt(h1);
        
      // read last count from previous run
      try {
         // this file stores the article count.
         File originalFile = new File("/tmp/lastWirehubTotal.txt");
         // if file does not exist then create it with 0 value.
         if (!originalFile.exists()) {
            PrintWriter pw1 = new PrintWriter(new FileWriter(originalFile));
            pw1.println("0");
            pw1.flush();
            pw1.close();
         }

         BufferedReader br = new BufferedReader(new FileReader(originalFile));

         // Construct the new temp file that will later be renamed to overwrite the original file.
         File tempFile = new File("/tmp/tempfileWirehub.txt");
         PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

         pw.println(h1);
         pw.flush();
         pw.close();

         String line = null;
         int oldTotal = 0;
         // Read from the original file and convert to integer
         while ((line = br.readLine()) != null) {
            oldTotal = Integer.parseInt(line);
         }
         br.close();

         // if the number of articles added is less than threshold, then send email as alert.
         if (newTotal <= oldTotal + 5) {
            WirehubCheck wc = new WirehubCheck();
            wc.sendMail("WP Wirehub count : " + h1, "WP Wirehub alert");
            System.out.println("Error: Wirehub count=" +h1);
            System.exit(2);
         }
        
         // Delete the original file
         if (!originalFile.delete()) {
            System.out.println("Could not delete file");
            System.exit(2);
         }

         // Rename the new file to the filename the original file had.
         if (!tempFile.renameTo(originalFile)) {
            System.out.println("Could not rename file");
            System.exit(2);
         }

      } catch (MessagingException me) {
         me.printStackTrace();
         System.exit(2);
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(2);
      }
      // return normal status
      System.exit(0);
        
   }

}
