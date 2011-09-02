
package TextPlayer;

import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijikokun.register.payment.Methods;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.Plugin;

/**
 * Checks for plugins whenever one is enabled
 *
 */
public class PluginListener extends ServerListener {
    public PluginListener() { }
    Methods methods = new Methods();

    @Override
    public void onPluginEnable(PluginEnableEvent event) {
        if (!TextPlayer.perm.equals("op")) {
            if (TextPlayer.permissions == null) {
                Plugin permissions = TextPlayer.pm.getPlugin("Permissions");
                if (permissions != null) {
                    TextPlayer.permissions = ((Permissions)permissions).getHandler();
                    System.out.println("[TextPlayer] Successfully linked with Permissions!");
                }
            }
        }
        if (Register.economy == null)
            System.err.println("[TextPlayer] Config file outdated, Please regenerate");
        else if (!Register.economy.equalsIgnoreCase("none") && !methods.hasMethod()) {
            try {
                methods.setMethod(TextPlayer.pm.getPlugin(Register.economy));
                if (methods.hasMethod()) {
                    Register.econ = methods.getMethod();
                    System.out.println("[TextPlayer] Successfully linked with "+
                            Register.econ.getName()+" "+Register.econ.getVersion()+"!");
                }
            }
            catch (Exception e) {
            }
        }
    }
}