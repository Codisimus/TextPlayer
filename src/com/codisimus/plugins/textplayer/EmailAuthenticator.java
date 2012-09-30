package com.codisimus.plugins.textplayer;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * Authenticates the Username and Password of an email account
 */
public class EmailAuthenticator extends Authenticator {
    private String user;
    private String pw;

    /**
     * Constructs a new Authenticator with the given Username and Password
     *
     * @param username The given Username
     * @param password The given Password
     */
    public EmailAuthenticator (String username, String password) {
        super();
        this.user = username;
        this.pw = password;
    }

    /**
     * Returns a new Password Authenticator
     *
     * @return A new Password Authenticator
     */
    @Override
    public PasswordAuthentication getPasswordAuthentication() {
       return new PasswordAuthentication(user, pw);
    }
}
