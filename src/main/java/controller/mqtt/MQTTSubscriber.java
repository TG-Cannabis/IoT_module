package controller.mqtt;

import io.github.cdimascio.dotenv.Dotenv;
import com.google.gson.Gson;
import model.SensorData;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import repository.influxdb.InfluxDBUtil;

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
            kafkaProducer = setupKafkaProducer();

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
    private static KafkaProducer<String, String> setupKafkaProducer() {
        Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load();

        String kafkaBroker = dotenv.get("KAFKA_BROKER");
        String kafkaTopic = dotenv.get("KAFKA_TOPIC");

        // Verifica que las variables no sean null
        if (kafkaBroker == null || kafkaTopic == null) {
            throw new IllegalArgumentException("❌ Error: KAFKA_BROKER o KAFKA_TOPIC no están definidos en el .env");
        }

        // Configurar propiedades de Kafka
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBroker);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        System.out.println("✅ Kafka Producer configurado con broker: " + kafkaBroker);
        return new KafkaProducer<>(props);
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
                    System.out.println(exception.getMessage());
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

            // Enviar mensaje y manejar respuesta de forma asíncrona
            kafkaProducer.send(testRecord, (metadata, exception) -> {
                if (exception == null) {
                    kafkaAvailable = true;
                    System.out.println("✅ Kafka está disponible.");
                } else {
                    kafkaAvailable = false;
                    System.err.println("❌ Kafka no disponible. Usando InfluxDB.");
                    exception.printStackTrace();  // Imprime el error exacto
                }
            });
        } catch (Exception e) {
            kafkaAvailable = false;
            System.err.println("❌ Error inesperado al verificar Kafka.");
            e.printStackTrace();
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
        }, 5, 5, TimeUnit.SECONDS); // Intenta cada 30 segundos
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
