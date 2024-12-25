package eu.xaru.mysticrpg.storage.benchmark;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import eu.xaru.mysticrpg.storage.Callback;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Registers /dbbench command that performs a DB stress test.
 */
public class DbBenchmarkCommand {

    private static final List<String> BENCHMARK_LOG = new ArrayList<>();

    public static void register() {
        new CommandAPICommand("dbbench")
                .withPermission("mysticrpg.debug")
                .withArguments(new IntegerArgument("count", 1, 100_000))
                .executes((sender, args) -> {
                    int count = (int) args.args()[0];
                    runBenchmark(sender, count);
                })
                .register();
    }

    private static void runBenchmark(CommandSender sender, int count) {
        sender.sendMessage(Utils.getInstance().$("Starting DB benchmark with " + count + " inserts..."));
        long startTime = System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(count);
        AtomicLong successes = new AtomicLong(0);
        AtomicLong failures = new AtomicLong(0);

        // Insert random PlayerData
        for (int i = 0; i < count; i++) {
            UUID uuid = UUID.randomUUID();
            PlayerData data = PlayerData.defaultData(uuid.toString());
            data.setXp(i);
            data.setBankGold(i * 10);

            PlayerDataCache.getInstance().cacheAndMarkDirty(uuid, data, new Callback<>() {
                @Override
                public void onSuccess(Void result) {
                    successes.incrementAndGet();
                    latch.countDown();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    DebugLogger.getInstance().error("Benchmark insert fail for " + uuid, throwable);
                    failures.incrementAndGet();
                    latch.countDown();
                }
            });
        }

        // Wait off main thread
        new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            String summary = "DB Benchmark done!"
                    + "\nCount: " + count
                    + "\nSuccess: " + successes.get()
                    + "\nFail: " + failures.get()
                    + "\nTime: " + duration + " ms";

            synchronized (BENCHMARK_LOG) {
                BENCHMARK_LOG.add(summary);
            }

            sender.sendMessage(Utils.getInstance().$(summary));
            DebugLogger.getInstance().log(Level.INFO, "[Benchmark] " + summary, 0);

        }, "DbBenchmarkThread").start();
    }

    public static List<String> getBenchmarkLogs() {
        synchronized (BENCHMARK_LOG) {
            return new ArrayList<>(BENCHMARK_LOG);
        }
    }
}
