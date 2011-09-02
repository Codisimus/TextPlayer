
package TextPlayer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedList;

/**
 *
 * @author Codisimus
 */
public class SaveSystem {
    private static LinkedList<User> users = new LinkedList<User>();
    protected static void loadUsers() {
        BufferedReader bReader = null;
        try {
            new File("plugins/TextPlayer").mkdir();
            new File("plugins/TextPlayer/emails.save").createNewFile();
            bReader = new BufferedReader(new FileReader("plugins/TextPlayer/emails.save"));
            String line = "";
            while ((line = bReader.readLine()) != null) {
                String[] split = line.split(";");
                String name = split[0];
                String email = split[1];
                boolean disableWhenLogged = Boolean.parseBoolean(split[2]);
                int textLimit = Integer.parseInt(split[3]);
                int textsSent = Integer.parseInt(split[4]);
                int lastText = Integer.parseInt(split[5]);
                String watchingUsers = split[6];
                String watchingItems = split[7];
                String watchingWords;
                try {
                    watchingWords = split[8];
                }
                catch (Exception e) {
                    watchingWords = ",";
                }
                User user = new User(name, email, disableWhenLogged, textLimit,
                        textsSent, lastText, watchingUsers, watchingItems, watchingWords);
                users.add(user);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void saveUsers() {
        BufferedWriter bWriter = null;
        try {
            bWriter = new BufferedWriter(new FileWriter("plugins/TextPlayer/emails.save"));
            for(User user : users) {
                bWriter.write(user.name.concat(";"));
                bWriter.write(user.getEmail().concat(";"));
                bWriter.write(user.disableWhenLogged+";");
                bWriter.write(user.textLimit+";");
                bWriter.write(user.textsSent+";");
                bWriter.write(user.lastText+";");
                bWriter.write(user.getWatchingUsers().concat(";"));
                bWriter.write(user.getWatchingItems().concat(";"));
                bWriter.write(user.getWatchingWords().concat(";"));
                bWriter.newLine();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                bWriter.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected static User findUser(String name) {
        for(User user : users)
            if (user.name.equalsIgnoreCase(name))
                return user;
        return null;
    }

    protected static LinkedList<User> getUsers() {
        return users;
    }

    protected static void addUser(User user) {
        try {
            users.add(user);
            saveUsers();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void removeUser(User user){
        try {
            users.remove(user);
            saveUsers();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
