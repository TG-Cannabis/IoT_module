package model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SensorInformation {
    String sensorType;
    String location;
    String id;
}
