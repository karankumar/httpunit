import com.meterware.httpunit.*;

/** This is a simple example of using HttpUnit to read and understand web pages. **/
public class Example {


    public static void main( String[] params ) {
        try {
            // create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Obtain the main page on the meterware web site
            WebRequest request = new GetMethodWebRequest( "http://www.meterware.com" );
            WebResponse response = wc.getResponse( request );

            // find the link which contains the string "HttpUnit" and click it
            WebLink httpunitLink = response.getFirstMatchingLink( WebLink.MATCH_CONTAINED_TEXT, "HttpUnit" );
            response = httpunitLink.click();

            // print out the number of links on the HttpUnit main page
            System.out.println( "The HttpUnit main page contains " + response.getLinks().length + " links" );

        } catch (Exception e) {
            System.err.println( "Exception: " + e );
        }
    }
}

