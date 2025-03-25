package model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SensorData {
    private SensorInformation sensorName;
    private double value;
    private long timestamp;
}