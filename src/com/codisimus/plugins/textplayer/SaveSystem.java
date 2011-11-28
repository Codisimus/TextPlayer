package com.codisimus.plugins.textplayer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Holds TextPlayer data and is used to load/save data
 *
 * @author Codisimus
 */
public class SaveSystem {
    public static LinkedList<User> users = new LinkedList<User>();
    
    /**
     * Loads Users from the save file
     * 
     */
    public static void load() {
        String line = "";
        
        try {
            new File("plugins/TextPlayer/emails.save").createNewFile();
            BufferedReader bReader = new BufferedReader(new FileReader("plugins/TextPlayer/emails.save"));
            
            while ((line = bReader.readLine()) != null) {
                String[] data = line.split(";");
                
                User user = new User(data[0], data[1]);

                user.disableWhenLogged = Boolean.parseBoolean(data[2]);
                user.textLimit = Integer.parseInt(data[3]);
                user.textsSent = Integer.parseInt(data[4]);
                user.lastText = Integer.parseInt(data[5]);

                //Check if an old save file
                if (data.length == 9) {
                    //Update outdated save file
                    if (data[6].contains("server,"))
                        data[6] = data[6].replaceAll("server,", ",");
                    if (data[6].contains(",,"))
                        data[6] = data[6].replaceAll(",,", ",");
                    if (data[6].equals(","))
                        data[6] = "none";

                    if (!data[6].equals("none")) 
                        user.players = new LinkedList(Arrays.asList(data[6].split(",")));

                    //Update outdated save file
                    if (data[7].equals(","))
                        data[7] = "none";

                    if (!data[7].equals("none"))
                        user.items = new LinkedList(Arrays.asList(data[7].split(",")));

                    //Update outdated save file
                    if (data[8].equals(","))
                        data[8] = "none";

                    if (!data[8].equals("none"))
                        user.words = new LinkedList(Arrays.asList(data[8].split(",")));

                    users.add(user);
                }
                else {
                    user.watchingServer = Boolean.parseBoolean(data[6]);
                    
                    user.players = new LinkedList(Arrays.asList(data[6].substring(1, data[6].length() - 1).split(", ")));

                    user.items = new LinkedList(Arrays.asList(data[7].substring(1, data[7].length() - 1).split(", ")));

                    user.words = new LinkedList(Arrays.asList(data[8].substring(1, data[8].length() - 1).split(", ")));

                    users.add(user);
                }
            }
            
            bReader.close();
        }
        catch (Exception loadFailed) {
            System.err.println("[TextPlayer] Load failed, saving turned off to prevent loss of data");
            System.err.println("[TextPlayer] Errored line: "+line);
            loadFailed.printStackTrace();
        }
    }

    /**
     * Writes Users to save file
     * Old file is overwritten
     */
    public static void save() {
        try {
            BufferedWriter bWriter = new BufferedWriter(new FileWriter("plugins/TextPlayer/emails.save"));
            
            for(User user: users) {
                //Writes User to file in the following format
                //name;email;disabledWhenLogged;textLimit;TextsSent;lastText;watchingServer;watchedPlayers;watchedItems;watchedWords;
                bWriter.write(user.name.concat(";"));
                bWriter.write(user.email.concat(";"));
                bWriter.write(user.disableWhenLogged+";");
                bWriter.write(user.textLimit+";");
                bWriter.write(user.textsSent+";");
                bWriter.write(user.lastText+";");
                bWriter.write(user.watchingServer+";");
                bWriter.write(user.players.toString().concat(";"));
                bWriter.write(user.items.toString().concat(";"));
                bWriter.write(user.words.toString().concat(";"));
                
                //Write each User on it's own line
                bWriter.newLine();
            }
            
            bWriter.close();
        }
        catch (Exception saveFailed) {
            System.err.println("[TextPlayer] Save Failed!");
            saveFailed.printStackTrace();
        }
    }

    /**
     * Returns the User with the given name
     * 
     * @param name The name of the User you wish to find
     * @return The User with the given name or null if not found
     */
    public static User findUser(String name) {
        //Iterate through all Users to find the one with the given Name
        for(User user : users)
            if (user.name.equalsIgnoreCase(name))
                return user;
        
        //Return null because the User does not exist
        return null;
    }
}