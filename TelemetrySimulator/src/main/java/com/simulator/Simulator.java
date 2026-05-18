package com.simulator;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.*;

public class Simulator {

    static String BASE_URL = "http://localhost:9090/api";

    static Map<String, Long> lastSpikeTime = new HashMap<>();
    static final long COOLDOWN_MS = 20 * 60 * 1000;

    static boolean canSpike(String deviceId, String metric) {
        String key = deviceId + "_" + metric;
        long now = System.currentTimeMillis();
        Long last = lastSpikeTime.get(key);
        if (last == null || (now - last) > COOLDOWN_MS) {
            lastSpikeTime.put(key, now);
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {

        RestTemplate restTemplate = new RestTemplate();
        Random random = new Random();

        while (true) {

            List<Map<String, Object>> devices =
                    restTemplate.getForObject(BASE_URL + "/devices", List.class);

            if (devices == null || devices.isEmpty()) {
                System.out.println("No devices found...");
                Thread.sleep(5000);
                continue;
            }

            for (Map<String, Object> device : devices) {

                String deviceId = (String) device.get("deviceId");
                String type     = (String) device.get("type");
                String status   = (String) device.get("status");

                if (!"ACTIVE".equals(status)) {
                    System.out.println("Skipping INACTIVE device: " + deviceId);
                    continue;
                }

                if ("temperature_sensor".equals(type)) {
                    boolean spike = random.nextInt(20) == 0 && canSpike(deviceId, "temperature");
                    double temp = spike
                            ? 36 + random.nextDouble() * 3
                            : 22 + random.nextDouble() * 6;
                    send(restTemplate, deviceId, "temperature", temp);
                }

                else if ("humidity_sensor".equals(type)) {
                    send(restTemplate, deviceId, "humidity",
                            35 + random.nextDouble() * 30);
                }

                else if ("co2_sensor".equals(type)) {
                    boolean spike = random.nextInt(20) == 0 && canSpike(deviceId, "co2_level");
                    double co2 = spike
                            ? 1100 + random.nextDouble() * 300
                            : 400  + random.nextDouble() * 500;
                    send(restTemplate, deviceId, "co2_level", co2);
                }

                else if ("light_sensor".equals(type)) {
                    boolean spike = random.nextInt(20) == 0 && canSpike(deviceId, "light_intensity");
                    double light = spike
                            ? 100 + random.nextDouble() * 150
                            : 300 + random.nextDouble() * 300;
                    send(restTemplate, deviceId, "light_intensity", light);
                }

                else if ("multi_sensor".equals(type)) {
                    send(restTemplate, deviceId, "temperature",
                            22 + random.nextDouble() * 6);
                    send(restTemplate, deviceId, "humidity",
                            35 + random.nextDouble() * 30);
                    send(restTemplate, deviceId, "co2_level",
                            400 + random.nextDouble() * 500);
                    send(restTemplate, deviceId, "light_intensity",
                            300 + random.nextDouble() * 300);
                }
            }

            System.out.println("Data Sent Successfully...");
            Thread.sleep(5000 + random.nextInt(1000));
        }
    }

    static void send(RestTemplate restTemplate, String deviceId, String metric, double value) {

        Map<String, Object> body = new HashMap<>();
        body.put("deviceId",  deviceId);
        body.put("metric",    metric);
        body.put("value",     value);
        body.put("timestamp", LocalDateTime.now().toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForObject(BASE_URL + "/telemetry/saveData", request, String.class);
        } catch (Exception e) {
            System.out.println("Error sending data for " + deviceId + ": " + e.getMessage());
        }
    }
}