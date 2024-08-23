//package eu.xaru.mysticrpg.ui;
//
//import java.util.List;
//import java.util.Objects;
//
//public class CachedPlayerData {
//    private int level;
//    private double balance;
//    private String quest;
//    private List<String> partyMembers;
//
//    public CachedPlayerData(int level, double balance, String quest, List<String> partyMembers) {
//        this.level = level;
//        this.balance = balance;
//        this.quest = quest;
//        this.partyMembers = partyMembers;
//    }
//
//    // Getters and Setters
//    public int getLevel() {
//        return level;
//    }
//
//    public void setLevel(int level) {
//        this.level = level;
//    }
//
//    public double getBalance() {
//        return balance;
//    }
//
//    public void setBalance(double balance) {
//        this.balance = balance;
//    }
//
//    public String getQuest() {
//        return quest;
//    }
//
//    public void setQuest(String quest) {
//        this.quest = quest;
//    }
//
//    public List<String> getPartyMembers() {
//        return partyMembers;
//    }
//
//    public void setPartyMembers(List<String> partyMembers) {
//        this.partyMembers = partyMembers;
//    }
//
//    // Override equals and hashCode to compare objects easily
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        CachedPlayerData that = (CachedPlayerData) o;
//        return level == that.level &&
//                Double.compare(that.balance, balance) == 0 &&
//                Objects.equals(quest, that.quest) &&
//                Objects.equals(partyMembers, that.partyMembers);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(level, balance, quest, partyMembers);
//    }
//}
