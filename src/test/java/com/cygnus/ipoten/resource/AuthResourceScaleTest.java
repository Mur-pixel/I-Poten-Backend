package com.cygnus.ipoten.resource;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class AuthResourceScaleTest {

    static final int[] USER_COUNTS = {1000, 100_000};
    static final int REFRESH_COUNT = 10;
    static final int THREAD_POOL = 50;

    public static void main(String[] args) throws InterruptedException, IOException {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

        for (int userCount : USER_COUNTS) {
            System.out.println("===== 사용자 수: " + userCount + " =====");

            SessionAuthService sessionService = new SessionAuthService();
            CookieAuthService cookieService = new CookieAuthService();

            Result sessionResult = runSimulation(sessionService::verifySessionCpu, "Localstorage", userCount, osBean);
            Result cookieResult = runSimulation(cookieService::verifyJwtWithTTL, "HttpOnlyCookie", userCount, osBean);

            printResult(sessionResult);
            printResult(cookieResult);

            saveToCsv(sessionResult, cookieResult, userCount);
        }
    }

    private static Result runSimulation(Runnable authAction, String label, int userCount, OperatingSystemMXBean osBean) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL);
        AtomicLong totalTime = new AtomicLong();
        AtomicInteger requestCount = new AtomicInteger();

        long startWall = System.currentTimeMillis();
        long startCpu = osBean.getProcessCpuTime();

        for (int i = 0; i < userCount; i++) {
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
        executor.awaitTermination(30, TimeUnit.MINUTES);

        long endWall = System.currentTimeMillis();
        long endCpu = osBean.getProcessCpuTime();

        return new Result(
                label,
                requestCount.get(),
                (endWall - startWall),
                totalTime.get() / requestCount.get(),
                (endCpu - startCpu)
        );
    }

    private static void printResult(Result r) {
        System.out.println("===== " + r.type + " =====");
        System.out.println("총 요청 수             : " + r.requests);
        System.out.println("총 수행 시간(ms)        : " + r.totalTimeMs);
        System.out.println("평균 요청당 처리(ns)    : " + r.avgTimeNs);
        System.out.println("CPU 사용 시간(ns)       : " + r.cpuNs);
        System.out.println();
    }

    private static void saveToCsv(Result r1, Result r2, int userCount) throws IOException {
        String fileName = "auth_resource_cpu_" + userCount + ".csv";
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("Type,Requests,TotalTime(ms),AvgTime(ns),CPU(ns)\n");
            writer.write(r1.toCsv() + "\n");
            writer.write(r2.toCsv() + "\n");
        }
    }

    record Result(String type, int requests, long totalTimeMs, long avgTimeNs, long cpuNs) {
        public String toCsv() {
            return type + "," + requests + "," + totalTimeMs + "," + avgTimeNs + "," + cpuNs;
        }
    }

    // Session 방식 (Redis 조회 시 CPU 연산 시뮬레이션)
    static class SessionAuthService {
        public void verifySessionCpu() {
            double x = 0;
            for (int i = 0; i < 1000; i++) {
                x += Math.sqrt(i) * Math.random();
            }
        }
    }

    // HttpOnly 쿠키 방식 (JWT + TTL 시뮬레이션)
    static class CookieAuthService {
        private final ConcurrentHashMap<String, Long> jwtCache = new ConcurrentHashMap<>();
        private final long TTL_MS = 5_000;

        public void verifyJwtWithTTL() {
            String jwt = "mockJwtToken";
            long now = System.currentTimeMillis();
            jwtCache.compute(jwt, (key, lastVerified) -> {
                if (lastVerified == null || now - lastVerified > TTL_MS) {
                    double x = 0;
                    for (int i = 0; i < 10; i++) {
                        x += Math.sqrt(i) * Math.random();
                    }
                    return now;
                }
                return lastVerified;
            });
        }
    }
}
