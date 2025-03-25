package utils;

import com.influxdb.client.*;
import com.influxdb.client.domain.DeletePredicateRequest;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;
import com.influxdb.query.FluxRecord;
import model.SensorData;
import model.SensorInformation;
import io.github.cdimascio.dotenv.Dotenv;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InfluxDBUtil {
    private final InfluxDBClient influxDBClient;
    private final WriteApiBlocking writeApi;
    private final String bucket;
    private final String org;

    public InfluxDBUtil() {
        // Cargar configuración desde .env
        Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load();
        String url = dotenv.get("INFLUXDB_URL");
        String token = dotenv.get("INFLUXDB_TOKEN");
        this.bucket = dotenv.get("INFLUXDB_BUCKET");
        this.org = dotenv.get("INFLUXDB_ORG");

        // Crear cliente de InfluxDB
        this.influxDBClient = InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
        this.writeApi = influxDBClient.getWriteApiBlocking();
    }

    /**
     * 📌 Guarda los datos del sensor en InfluxDB si Kafka no está disponible.
     */
    public void writeSensorData(SensorData data) {
        try {
            Point point = Point.measurement("sensor_data")
                    .addTag("sensor_id", data.getSensorName().getId())
                    .addTag("sensor_type", data.getSensorName().getSensorType())
                    .addTag("location", data.getSensorName().getLocation())
                    .addField("value", data.getValue())
                    .time(data.getTimestamp(), WritePrecision.MS);

            writeApi.writePoint(point);
            System.out.println("💾 Datos guardados en InfluxDB: " + data);
        } catch (Exception e) {
            System.err.println("❌ Error al escribir en InfluxDB: " + e.getMessage());
        }
    }

    /**
     * 📌 Obtiene datos pendientes de InfluxDB para reenvío a Kafka.
     */
    public List<SensorData> getPendingSensorData() {
        List<SensorData> pendingData = new ArrayList<>();
        String fluxQuery = "from(bucket: \"" + bucket + "\") |> range(start: -1h) |> filter(fn: (r) => r._measurement == \"sensor_data\")";

        try {
            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(fluxQuery);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    SensorData data = new SensorData(
                            new SensorInformation(
                                    Objects.requireNonNull(record.getValueByKey("sensor_type")).toString(),
                                    Objects.requireNonNull(record.getValueByKey("location")).toString(),
                                    Objects.requireNonNull(record.getValueByKey("sensor_id")).toString()
                            ),
                            ((Number) Objects.requireNonNull(record.getValue())).doubleValue(),
                            Objects.requireNonNull(record.getTime()).toEpochMilli()
                    );
                    pendingData.add(data);
                }
            }
            System.out.println("🔄 Datos pendientes obtenidos de InfluxDB: " + pendingData.size());
        } catch (Exception e) {
            System.err.println("❌ Error al obtener datos pendientes de InfluxDB: " + e.getMessage());
        }
        return pendingData;
    }

    /**
     * 📌 Elimina datos de InfluxDB después de enviarlos a Kafka.
     */
    public void deleteSensorData(SensorData data) {
        try {
            DeleteApi deleteApi = influxDBClient.getDeleteApi();
            DeletePredicateRequest request = new DeletePredicateRequest();
            request.setStart(OffsetDateTime.from(Instant.ofEpochMilli(data.getTimestamp()).minusSeconds(60))); // Rango 1 min antes
            request.setStop(OffsetDateTime.from(Instant.ofEpochMilli(data.getTimestamp()).plusSeconds(60))); // Rango 1 min después
            request.setPredicate("sensor_id=\"" + data.getSensorName().getId() + "\"");

            deleteApi.delete(request, bucket, org);
            System.out.println("🗑️ Datos eliminados de InfluxDB: " + data);
        } catch (Exception e) {
            System.err.println("❌ Error al eliminar datos de InfluxDB: " + e.getMessage());
        }
    }
}
