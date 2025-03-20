package model;

import lombok.Data;

@Data
public class SensorData {
    private String sensorName;
    private double value;
    private long timestamp;

    public SensorData(String sensorName, double value) {
        this.sensorName = sensorName;
        this.value = value;
        this.timestamp = System.currentTimeMillis();
    }
}