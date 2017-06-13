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
import java.util.Calendar;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;


public class WirehubCheck {

     
   //------------------------------------------------------------------------------------
   // This method sends an email as an alert.
   //
   public static void sendMail( String message, String subjectMsg ) throws MessagingException {

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
      addressTo[0] = new InternetAddress("ppak@XYZ.com");

      msg.setRecipients(Message.RecipientType.TO, addressTo);

      // Setting the Subject and Content Type
      msg.setSubject(subjectMsg);
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
      if (args == null || args.length != 3) {
         System.out.println("Usage: WirehubCheck http://XYZ.com/wp-json/wp/v2/posts feedName 5137");
         return;
      }
      // http://XYZ.com/wp-json/wp/v2/posts/?after=2017-01-09T17:00:00Z&sources=5137
      // ?after DateTime use current value + 5 hours. Hubs use GMT +7 and -2 to check past 2 hours.
      String stringUrl = args[0];
      // feedName used for informational purpose which is sent in email i.e. AP or WAPO
      String feedName  = args[1];
      // For WAPO use &sources=5152 and AP use &sources=5137
      String sourcesId = "&sources=" + args[2];

      // get current date
      Calendar cal = Calendar.getInstance();
	  int currLocHour = cal.get( Calendar.HOUR_OF_DAY );
	  System.out.println( "currLocHour=" +currLocHour );
      // adjust for wirehub GMT(7) - 2 hours
	  if ("WAPO".equals( feedName )) {
        cal.add(Calendar.HOUR_OF_DAY, 3);
	  } else {
        cal.add(Calendar.HOUR_OF_DAY, 4);
      }

      // format date
      StringBuffer dateStr = new StringBuffer(50);
      dateStr.append( cal.get(Calendar.YEAR) + "-" );
      if ( cal.get(Calendar.MONTH) < 9) { dateStr.append("0"); }
      dateStr.append( (cal.get(Calendar.MONTH)+1) + "-" );
      if ( cal.get(Calendar.DAY_OF_MONTH) <= 9) { dateStr.append("0"); }
      dateStr.append( cal.get(Calendar.DAY_OF_MONTH) + "T" );
      if ( cal.get(Calendar.HOUR_OF_DAY) <= 9) { dateStr.append("0"); }
      dateStr.append( cal.get(Calendar.HOUR_OF_DAY) + ":" );
      if ( cal.get(Calendar.MINUTE) <= 9) { dateStr.append("0"); }
      dateStr.append( cal.get(Calendar.MINUTE) + ":00Z" );

      stringUrl = stringUrl + "?after=" + dateStr + sourcesId ;

      // The connection
      System.out.println(stringUrl);

	  WirehubCheck wc = new WirehubCheck();
      try {
	     URL url = new URL(stringUrl);
         URLConnection uc = url.openConnection();
         if (uc == null) {
            wc.sendMail(" Check WP Wirehub : " + feedName, "Error in openConnection" );
            System.exit(2);
         }
         uc.setRequestProperty("X-Requested-With", "Curl");

         // gets header page count
         String h1 = uc.getHeaderField("X-WP-Total");
         if (h1 == null || h1.length() < 1) {
            wc.sendMail(" Check WP Wirehub : " + feedName, "X-WP-Total header=["+h1+"]" );
            System.exit(2);
         }
         int newTotal = Integer.parseInt(h1);

         // check if no articles have been created in last 2 hours.
         // if the number of articles added is less than threshold, then send email as alert.
		 if ( ("WAPO".equals(feedName) && currLocHour > 7 && newTotal < 1) ||
              ("AP".equals(feedName) && newTotal < 1) ) {
            wc.sendMail("Check WP Wirehub : " + stringUrl, "WP Wirehub alert: " + feedName);
            System.exit(2);
         }

      } catch (MessagingException me) {
         me.printStackTrace();
         System.exit(2);
	  } catch (IOException ie) {
	     // Construct the new temp file that will later be renamed to overwrite the original file.
         File tempFile = new File("/tmp/tempfileWirehub.txt");
         PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
         pw.println(ie.toString() + " " + stringUrl);
         pw.flush();
         pw.close();
		 System.exit(2);
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(2);
      }
      // return normal status
      System.exit(0);
        
   }

}

