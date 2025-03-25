package utils;

import com.influxdb.client.domain.WritePrecision;
import io.github.cdimascio.dotenv.Dotenv;
import com.influxdb.client.*;
import com.influxdb.client.write.Point;
import model.SensorData;

public class InfluxDBUtil {
    private InfluxDBClient influxDBClient;
    private String bucket;
    private String org;

    public InfluxDBUtil() {
        Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load();
        String url = dotenv.get("INFLUXDB_URL");
        String token = dotenv.get("INFLUXDB_TOKEN");
        this.bucket = dotenv.get("INFLUXDB_BUCKET");
        this.org = dotenv.get("INFLUXDB_ORG");

        // Conectar a InfluxDB con Token
        this.influxDBClient = InfluxDBClientFactory.create(url, token.toCharArray());
    }

    public void writeSensorData(SensorData data) {
        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

        Point point = Point.measurement("sensor_data")
                .addTag("sensor", data.getSensorName())
                .addField("value", data.getValue())
                .time(data.getTimestamp(), WritePrecision.MS);

        writeApi.writePoint(bucket, org, point);
        System.out.println("✅ Datos guardados en InfluxDB: " + data);
    }
}
