import java.util.Scanner;
import java.io.Console;

public class Login {
    
    private String username;
    private String password;
    
    private Scanner input = new Scanner(System.in);
    private Console console = System.console();
    
    public void setUsername() {
        System.out.print("Enter username or email: ");
        this.username = input.nextLine();
    }
    
    public void setPassword() {
        //masks input in the console so that the password is not visible
        //will look like nothing is being typed, but input is being entered
        this.password = new String(console.readPassword("Enter password: "));
    }
    
    public String getUsername() {
        return this.username;
    }
    
    public String getPassword() {
        return this.password;
    }
    
} //end class Login