package com.cygnus.ipoten.resource;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AuthResourceRealisticTest2 {

    static final int USER_COUNT = 1000;       // 현실적 사용자 수
    static final int REFRESH_COUNT = 10;     // 사용자당 새로고침 횟수
    static final int THREAD_POOL = 50;       // 동시 요청 스레드 수

    public static void main(String[] args) throws InterruptedException, IOException {
        SessionAuthService sessionService = new SessionAuthService();
        CookieAuthService cookieService = new CookieAuthService();

        Result sessionResult = runSimulation(() -> sessionService.verifySession("userToken123"), "Session(Redis)");
        Result cookieResult = runSimulation(() -> cookieService.verifyJwtWithTTL("mockJwtToken"), "HttpOnly Cookie");

        printResult(sessionResult);
        printResult(cookieResult);
        saveToCsv(sessionResult, cookieResult);
    }

    private static Result runSimulation(Runnable authAction, String label) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL);
        AtomicLong totalTime = new AtomicLong();
        AtomicInteger requestCount = new AtomicInteger();

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
        executor.awaitTermination(10, TimeUnit.MINUTES);

        long end = System.currentTimeMillis();
        long endMem = getUsedMemory();

        return new Result(
                label,
                requestCount.get(),
                (end - start),
                totalTime.get() / requestCount.get(),
                (endMem - startMem)
        );
    }

    private static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static void printResult(Result r) {
        System.out.println("===== " + r.type + " =====");
        System.out.println("총 요청 수           : " + r.requests);
        System.out.println("총 수행 시간(ms)      : " + r.totalTimeMs);
        System.out.println("평균 요청당 처리(ns)  : " + r.avgTimeNs);
        System.out.println("메모리 사용(bytes)    : " + r.memBytes);
        System.out.println();
    }

    private static void saveToCsv(Result r1, Result r2) throws IOException {
        try (FileWriter writer = new FileWriter("auth_resource_realistic.csv")) {
            writer.write("Type,Requests,TotalTime(ms),AvgTime(ns),Memory(bytes)\n");
            writer.write(r1.toCsv() + "\n");
            writer.write(r2.toCsv() + "\n");
        }
        System.out.println("결과 저장 완료 → auth_resource_realistic.csv");
    }

    record Result(String type, int requests, long totalTimeMs, long avgTimeNs, long memBytes) {
        public String toCsv() {
            return type + "," + requests + "," + totalTimeMs + "," + avgTimeNs + "," + memBytes;
        }
    }

    // Session 방식 (Redis 조회 시뮬레이션)
    static class SessionAuthService {
        public void verifySession(String token) {
            try {
                Thread.sleep(3); // Redis I/O 딜레이 시뮬레이션
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // HttpOnly 쿠키 방식 (JWT + TTL 시뮬레이션)
    static class CookieAuthService {
        private final ConcurrentHashMap<String, Long> jwtCache = new ConcurrentHashMap<>();
        private final long TTL_MS = 5_000; // TTL 5초

        public void verifyJwtWithTTL(String jwt) {
            long now = System.currentTimeMillis();
            jwtCache.compute(jwt, (key, lastVerified) -> {
                if (lastVerified == null || now - lastVerified > TTL_MS) {
                    Math.pow(jwt.hashCode(), 2); // CPU 연산 시뮬레이션
                    return now;
                }
                return lastVerified; // TTL 내 재사용 시 검증 생략
            });
        }
    }
}
