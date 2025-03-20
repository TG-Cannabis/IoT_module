package utils;

import io.github.cdimascio.dotenv.Dotenv;
import model.SensorData;
import org.eclipse.paho.client.mqttv3.*;

public class MQTTSubscriber {
    private static InfluxDBUtil influxDBUtil = new InfluxDBUtil();

    public static void main(String[] args) {
        try {
            // Cargar configuración desde .env
            Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load();
            String broker = dotenv.get("MQTT_BROKER");
            String clientId = dotenv.get("MQTT_CLIENT_ID");
            String topic = dotenv.get("MQTT_TOPIC");

            // Configurar cliente MQTT
            MqttClient client = new MqttClient(broker, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            // Conectar
            client.connect(options);
            System.out.println("Conectado a MQTT Broker: " + broker);

            // Suscribirse al tópico
            client.subscribe(topic, (topicName, message) -> {
                String payload = new String(message.getPayload());
                System.out.println("Mensaje recibido: " + payload);

                // Convertir mensaje a objeto SensorData
                String[] parts = payload.split(",");
                if (parts.length == 2) {
                    String sensorName = parts[0].trim();
                    double value = Double.parseDouble(parts[1].trim());

                    SensorData data = new SensorData(sensorName, value);
                    influxDBUtil.writeSensorData(data);
                    System.out.println("Datos guardados en InfluxDB");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
