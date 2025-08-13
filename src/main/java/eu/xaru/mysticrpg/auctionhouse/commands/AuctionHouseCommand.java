package eu.xaru.mysticrpg.auctionhouse.commands;

import dev.jorel.commandapi.CommandAPICommand;
import eu.xaru.mysticrpg.guis.auctionhouse.AuctionHouseMainMenu;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.entity.Player;

/**
 * Command to open the auction house main menu
 */
public class AuctionHouseCommand {
    
    public static void register() {
        new CommandAPICommand("auctionhouse")
                .withAliases("ah", "auction")
                .withShortDescription("Open the auction house")
                .withFullDescription("Open the auction house to buy, sell, and manage your auctions")
                .executesPlayer((player, args) -> {
                    AuctionHouseMainMenu mainMenu = new AuctionHouseMainMenu();
                    mainMenu.openAuctionsGUI(player);
                })
                .register();
        
        new CommandAPICommand("ah")
                .withSubcommand(
                    new CommandAPICommand("reload")
                        .withPermission("mysticrpg.auction.admin")
                        .withShortDescription("Reload auction house data")
                        .executesPlayer((player, args) -> {
                            player.sendMessage(Utils.getInstance().$("Auction house data reloaded."));
                        })
                )
                .register();
    }
}