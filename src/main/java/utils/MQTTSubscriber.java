package utils;

import io.github.cdimascio.dotenv.Dotenv;
import com.google.gson.Gson;
import model.SensorData;
import org.apache.kafka.clients.producer.*;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MQTTSubscriber {
    private static final InfluxDBUtil influxDBUtil = new InfluxDBUtil();
    private static final Gson gson = new Gson();
    private static KafkaProducer<String, String> kafkaProducer;
    private static String kafkaTopic;
    private static boolean kafkaAvailable = false;

    public static void main(String[] args) {
        try {
            // Cargar configuración desde .env
            Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load();
            String mqttBroker = dotenv.get("MQTT_BROKER");
            String mqttClientId = dotenv.get("MQTT_SUBSCRIBER_ID");
            String mqttTopic = dotenv.get("MQTT_TOPIC");
            String kafkaBrokers = dotenv.get("KAFKA_BROKERS");
            kafkaTopic = dotenv.get("KAFKA_TOPIC");

            // Configurar Kafka Producer
            setupKafkaProducer(kafkaBrokers);

            // Configurar cliente MQTT
            MqttClient mqttClient = new MqttClient(mqttBroker, mqttClientId, new MemoryPersistence());
            MqttConnectOptions mqttOptions = new MqttConnectOptions();
            mqttOptions.setCleanSession(true);

            // Conectar a MQTT
            mqttClient.connect(mqttOptions);
            System.out.println("✅ Conectado a MQTT Broker: " + mqttBroker);

            // Iniciar el proceso de reintento en segundo plano
            startRetryMechanism();

            // Suscribirse al tópico y procesar mensajes
            mqttClient.subscribe(mqttTopic, (topic, message) -> {
                String payload = new String(message.getPayload());
                System.out.println("📩 Mensaje recibido: " + payload);

                try {
                    SensorData data = gson.fromJson(payload, SensorData.class);
                    sendToKafkaOrInfluxDB(data);
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

    /**
     * Configura el Kafka Producer.
     */
    private static void setupKafkaProducer(String kafkaBrokers) {
        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaBrokers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "all");
        props.put("retries", 3);

        kafkaProducer = new KafkaProducer<>(props);
        checkKafkaAvailability();
    }

    /**
     * Intenta enviar datos a Kafka. Si falla, los guarda en InfluxDB.
     */
    private static void sendToKafkaOrInfluxDB(SensorData data) {
        String jsonData = gson.toJson(data);

        if (kafkaAvailable) {
            ProducerRecord<String, String> record = new ProducerRecord<>(kafkaTopic, data.getSensorName().getId(), jsonData);

            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    System.out.println("🚀 Enviado a Kafka: " + jsonData);
                } else {
                    System.err.println("❌ Kafka no disponible. Guardando en InfluxDB...");
                    influxDBUtil.writeSensorData(data);
                    kafkaAvailable = false;
                }
            });
        } else {
            System.out.println("⚠️ Kafka no disponible. Guardando en InfluxDB...");
            influxDBUtil.writeSensorData(data);
        }
    }

    /**
     * Verifica si Kafka está disponible y cambia el estado `kafkaAvailable`.
     */
    private static void checkKafkaAvailability() {
        try {
            ProducerRecord<String, String> testRecord = new ProducerRecord<>(kafkaTopic, "test-key", "test-message");
            kafkaProducer.send(testRecord).get();
            kafkaAvailable = true;
            System.out.println("✅ Kafka está disponible.");
        } catch (Exception e) {
            kafkaAvailable = false;
            System.err.println("❌ Kafka no disponible. Usando InfluxDB.");
        }
    }

    /**
     * Inicia un proceso en segundo plano para reintentar enviar datos de InfluxDB a Kafka.
     */
    private static void startRetryMechanism() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            if (!kafkaAvailable) {
                checkKafkaAvailability();
            }
            if (kafkaAvailable) {
                resendDataFromInfluxDB();
            }
        }, 10, 30, TimeUnit.SECONDS); // Intenta cada 30 segundos
    }

    /**
     * Recupera datos almacenados en InfluxDB y los envía a Kafka.
     */
    private static void resendDataFromInfluxDB() {
        System.out.println("🔄 Intentando reenviar datos almacenados en InfluxDB...");

        List<SensorData> pendingData = influxDBUtil.getPendingSensorData();
        if (pendingData.isEmpty()) {
            System.out.println("✅ No hay datos pendientes en InfluxDB.");
            return;
        }

        for (SensorData data : pendingData) {
            String jsonData = gson.toJson(data);
            ProducerRecord<String, String> record = new ProducerRecord<>(kafkaTopic, data.getSensorName().getId(), jsonData);

            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    System.out.println("🚀 Reenviado a Kafka: " + jsonData);
                    influxDBUtil.deleteSensorData(data); // Eliminar de InfluxDB después de enviarlo
                } else {
                    System.err.println("❌ No se pudo reenviar a Kafka. Se mantiene en InfluxDB.");
                }
            });
        }
    }
}
