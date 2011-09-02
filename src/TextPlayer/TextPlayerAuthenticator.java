
package TextPlayer;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 *
 * @author Cody
 */
class TextPlayerAuthenticator extends Authenticator {
    String user;
    String pw;

    public TextPlayerAuthenticator (String username, String password) {
        super();
        this.user = username;
        this.pw = password;
    }

    @Override
    public PasswordAuthentication getPasswordAuthentication() {
       return new PasswordAuthentication(user, pw);
    }
}
