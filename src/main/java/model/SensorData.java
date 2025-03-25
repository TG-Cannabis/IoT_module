package model;

import lombok.Data;

@Data
public class SensorData {
    private SensorInformation sensorName;
    private double value;
    private long timestamp;
}