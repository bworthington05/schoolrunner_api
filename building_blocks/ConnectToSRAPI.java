//Class that contains methods for connecting to a given SchoolRunner API endpoint
//returns a String representation of all the data received

import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.net.Proxy;

public class ConnectToSRAPI {
    
    private OkHttpClient client = new OkHttpClient();
    
    //instance variables used to connect to API, passed in through constructor
    private String endpoint;
    private String username;
    private String password;
    
    //Consructor that takes the API endpoint URL, username, and password
    public ConnectToSRAPI(String endpoint, String username, String password) {
        
        this.endpoint = endpoint;
        this.username = username;
        this.password = password;
    }
    
    //Constructor that requires no arguments
    public ConnectToSRAPI() {
        
    }
    
    //set endpoint URL
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    //set username for authentication
    public void setUsername(String username) {
        this.username = username;
    }
    
    //set password for authentication
    public void setPassword(String password) {
        this.password = password;
    }
    
    //method that connects to endpoint
    public String run() throws Exception {
    
        Authenticator authenticator = new Authenticator() {
            
            @Override 
            public Request authenticate(Proxy proxy, Response response) {
                
                System.out.println("Authenticating for response: " + response);
                System.out.println("Challenges: " + response.challenges());
                String credential = Credentials.basic(username, password);
                return response.request().newBuilder().header("Authorization", credential).build();
            }
        
            @Override 
            public Request authenticateProxy(Proxy proxy, Response response) {
                return null; // Null indicates no attempt to authenticate.
            }
        };
        
        client.setAuthenticator(authenticator);
    
        Request request = new Request.Builder().url(endpoint).build();
    
        Response response = client.newCall(request).execute();
        
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }
        
        //return the response as a String
        return response.body().string();
    
    } //end run method

} //end class ConnectToSRAPI