package com.cygnus.ipoten.resource;

import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AuthResourceComparisonTest {

    private final SessionAuthService sessionAuthService = new SessionAuthService();
    private final CookieAuthService cookieAuthService = new CookieAuthService();

    private static final int USER_COUNT = 500;
    private static final int REFRESH_COUNT = 200; // 총 100,000 요청

    @Test
    void compareAuthResourceUsage() throws InterruptedException, IOException {
        // Redis 세션 방식
        var redisResult = runSimulation(() -> sessionAuthService.verifySession("userToken123"), "Localstorage");

        // HttpOnly 쿠키 방식
        var cookieResult = runSimulation(() -> cookieAuthService.verifyJwtOnce("mockJwtToken"), "HttpOnly");

        // CSV 저장
        saveToCsv(redisResult, cookieResult);

        // 콘솔에 바로 출력
        printResult(redisResult);
        printResult(cookieResult);
    }

    private Result runSimulation(Runnable authAction, String label) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(50);
        AtomicLong totalTime = new AtomicLong(0);
        AtomicInteger requestCount = new AtomicInteger(0);

        long startCpu = getCpuTime();
        long startMem = getUsedMemory();
        long start = System.currentTimeMillis();

        for (int i = 0; i < USER_COUNT; i++) {
            executor.submit(() -> {
                for (int j = 0; j < REFRESH_COUNT; j++) {
                    long s = System.nanoTime();
                    authAction.run();
                    totalTime.addAndGet(System.nanoTime() - s);
                    requestCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);

        long end = System.currentTimeMillis();
        long endCpu = getCpuTime();
        long endMem = getUsedMemory();

        return new Result(
                label,
                requestCount.get(),
                (end - start),
                totalTime.get() / requestCount.get(),
                (endCpu - startCpu),
                (endMem - startMem)
        );
    }

    private static long getCpuTime() {
        return System.nanoTime(); // 단순 CPU 시뮬레이션
    }

    private static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private void saveToCsv(Result redis, Result cookie) throws IOException {
        try (FileWriter writer = new FileWriter("auth_resource_comparison.csv")) {
            writer.write("Type,Requests,TotalTime(ms),AvgTime(ns),CPU(ns),Memory(bytes)\n");
            writer.write(redis.toCsv() + "\n");
            writer.write(cookie.toCsv() + "\n");
        }
        System.out.println("\n결과 저장 완료 → auth_resource_comparison.csv\n");
    }

    private void printResult(Result r) {
        System.out.println("===== " + r.type + " =====");
        System.out.println("총 요청 수       : " + r.requests);
        System.out.println("총 수행 시간(ms) : " + r.totalTimeMs);
        System.out.println("평균 요청당 처리(ns) : " + r.avgTimeNs);
        System.out.println("CPU(ns)          : " + r.cpuNs);
        System.out.println("Memory(bytes)    : " + r.memBytes);
        System.out.println();
    }

    record Result(String type, int requests, long totalTimeMs, long avgTimeNs, long cpuNs, long memBytes) {
        public String toCsv() {
            return type + "," + requests + "," + totalTimeMs + "," + avgTimeNs + "," + cpuNs + "," + memBytes;
        }
    }
}
