# IoT Module for Cannabis Monitoring

Welcome to the **IoT Module** designed for cannabis cultivation monitoring and management. This project integrates various IoT technologies to provide a seamless and real-time data collection experience.

## Features

- **Real-time Monitoring**: Keep track of environmental parameters like temperature, humidity, and soil moisture in your cannabis cultivation area.
- **MQTT Integration**: Efficient data transmission with MQTT, ensuring reliable and fast updates.
- **InfluxDB Support**: Data is stored in **InfluxDB**, making it easy to visualize and analyze your cultivation metrics over time.
- **Scalable**: Suitable for small to large-scale growing operations.

## Requirements

- Java 11+
- InfluxDB (for data storage)
- MQTT Broker (e.g., Eclipse Mosquitto)
- Kafka (For Pub/Sub)
  
## Setup

To run the MQTT broker locally:

```bash
docker run -it -p 1883:1883 -p 9001:9001 eclipse-mosquitto
```

## Installation

1. Clone this repository:
    ```bash
    git clone https://github.com/TG-Cannabis/IoT_module.git
    cd IoT_module
    ```

2. Install dependencies:
    ```bash
    mvn install
    ```

3. Configure your InfluxDB and MQTT Broker settings in the configuration files.

4. Run the application.

## Contributing

Feel free to fork this repository, create branches, and submit pull requests to improve the project!

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For questions or issues, open a GitHub issue or contact the maintainers.


