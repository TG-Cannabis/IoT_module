package utils;

import io.github.cdimascio.dotenv.Dotenv;
import com.google.gson.Gson;
import model.SensorData;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MQTTSubscriber {
    private static final InfluxDBUtil influxDBUtil = new InfluxDBUtil();
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        try {
            // Cargar configuración desde .env
            Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load();
            String broker = dotenv.get("MQTT_BROKER");
            String clientId = dotenv.get("MQTT_SUBSCRIBER_ID");
            String topic = dotenv.get("MQTT_TOPIC");

            // Configurar cliente MQTT
            MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            // Conectar al broker
            client.connect(options);
            System.out.println("✅ Conectado a MQTT Broker: " + broker);

            // Suscribirse al tópico y procesar mensajes JSON
            client.subscribe(topic, (topicName, message) -> {
                String payload = new String(message.getPayload());
                System.out.println("📩 Mensaje recibido: " + payload);

                try {
                    // Convertir JSON a objeto SensorData
                    SensorData data = gson.fromJson(payload, SensorData.class);
                    influxDBUtil.writeSensorData(data);
                    System.out.println("💾 Datos guardados en InfluxDB: " + data);
                } catch (Exception e) {
                    System.err.println("❌ Error al procesar mensaje MQTT: " + e.getMessage());
                }
            });

            // Mantener el programa corriendo
            System.out.println("🔄 Esperando mensajes...");
            while (true) {
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
