package app;

import java.sql.Connection;
import java.sql.DriverManager;

public class ApplicationBean {

    private String address;
    private String login;
    private String pass;

    private static ApplicationBean instance = new ApplicationBean();
    private Connection connection;

    public static ApplicationBean getInstance() {
        return instance;
    }

    private ApplicationBean() {
    }

    public Connection getConnection(){
        return connection;
    }

    public void initConnection() throws Exception{
        Class.forName("org.postgresql.Driver");
        connection = DriverManager
                .getConnection("jdbc:postgresql://" + address,
                        login, pass);
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }
}
