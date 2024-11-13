package eu.xaru.mysticrpg.storage.database;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.auctionhouse.Auction;
import eu.xaru.mysticrpg.storage.Callback;

import java.util.List;
import java.util.UUID;

public interface IDatabaseRepository {

    // Player Data Operations
    void savePlayerData(PlayerData playerData, Callback<Void> callback);
    void loadPlayerData(UUID uuid, Callback<PlayerData> callback);
    void deletePlayerData(UUID uuid, Callback<Void> callback);

    // Auction Operations
    void saveAuction(Auction auction, Callback<Void> callback);
    void loadAuctions(Callback<List<Auction>> callback);
    void deleteAuction(UUID auctionId, Callback<Void> callback);

    // Additional methods can be added as needed
    void loadAllPlayers(Callback<List<PlayerData>> callback);
}