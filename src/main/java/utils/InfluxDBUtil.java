package utils;

import io.github.cdimascio.dotenv.Dotenv;
import model.SensorData;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import java.util.concurrent.TimeUnit;

public class InfluxDBUtil {
    private InfluxDB influxDB;
    private String database;

    public InfluxDBUtil() {
        connect();
    }

    private void connect() {
        Dotenv dotenv = Dotenv.configure()
                .directory("src/main/resources")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        String url = dotenv.get("INFLUXDB_URL");
        String username = dotenv.get("INFLUXDB_USERNAME");
        String password = dotenv.get("INFLUXDB_PASSWORD");
        database = dotenv.get("INFLUXDB_DATABASE");

        influxDB = InfluxDBFactory.connect(url, username, password);
        influxDB.setDatabase(database);
        influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS);
    }

    public void writeSensorData(SensorData data) {
        Point point = Point.measurement("sensores")
                .time(data.getTimestamp(), TimeUnit.MILLISECONDS)
                .tag("sensor", data.getSensorName())
                .addField("valor", data.getValue())
                .build();

        influxDB.write(point);
    }

    public QueryResult queryData(String query) {
        return influxDB.query(new Query(query, database));
    }

    public void close() {
        influxDB.close();
    }
}
