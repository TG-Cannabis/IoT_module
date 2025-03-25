package utils;

import model.SensorData;
import model.SensorInformation;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.google.gson.Gson;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import io.github.cdimascio.dotenv.Dotenv;


public class MQTTSensorPublisher {
    static Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load();
    private static final String BROKER = dotenv.get("MQTT_BROKER");
    private static final String CLIENT_ID = dotenv.get("MQTT_PUBLISHER_ID");
    private static final String TOPIC = dotenv.get("MQTT_TOPIC");
    private static final Gson gson = new Gson();
    private static final Random random = new Random();

    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(BROKER, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            System.out.println("Conectando al broker MQTT...");
            client.connect(options);
            System.out.println("Conectado. Enviando datos...");

            while (true) {
                SensorData data = generateSensorData();
                String payload = gson.toJson(data);

                MqttMessage message = new MqttMessage(payload.getBytes());
                message.setQos(1);

                client.publish(TOPIC, message);
                System.out.println("📡 Datos enviados: " + payload);

                TimeUnit.SECONDS.sleep(5); // Envía datos cada 5 segundos
            }
        } catch (MqttException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static SensorData generateSensorData() {
        Random random = new Random();

        // Crear información del sensor
        SensorInformation sensorInfo = new SensorInformation();
        sensorInfo.setSensorType(random.nextBoolean() ? "temperature" : "humidity"); // Tipo de sensor aleatorio
        sensorInfo.setLocation(random.nextBoolean() ? "Office" : "Warehouse"); // Ubicación aleatoria
        sensorInfo.setId("sensor_" + (random.nextInt(3) + 1)); // sensor_1, sensor_2, sensor_3

        // Generar valor del sensor
        double value = sensorInfo.getSensorType().equals("temperature")
                ? 20 + (random.nextDouble() * 10)  // Temperatura entre 20 y 30
                : 40 + (random.nextDouble() * 30); // Humedad entre 40 y 70

        // Crear objeto SensorData
        SensorData data = new SensorData();
        data.setSensorName(sensorInfo);
        data.setValue(value);
        data.setTimestamp(System.currentTimeMillis());

        return data;
    }

}
