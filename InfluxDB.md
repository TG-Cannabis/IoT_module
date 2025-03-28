# 🚀 Instalación y Configuración de InfluxDB en Ubuntu/Debian  

## 🔑 Agregar la clave de InfluxData y el repositorio  

Ejecuta los siguientes comandos para agregar la clave GPG y configurar el repositorio de **InfluxDB** en Debian o Ubuntu:  

```sh
# Descargar la clave GPG de InfluxData
curl --silent --location -O https://repos.influxdata.com/influxdata-archive.key

# Verificar la integridad de la clave
echo "943666881a1b8d9b849b74caebf02d3465d6beb716510d86a39f6c8e8dac7515  influxdata-archive.key" \
| sha256sum --check - && cat influxdata-archive.key \
| gpg --dearmor \
| sudo tee /etc/apt/trusted.gpg.d/influxdata-archive.gpg > /dev/null

# Agregar el repositorio de InfluxDB
echo 'deb [signed-by=/etc/apt/trusted.gpg.d/influxdata-archive.gpg] https://repos.influxdata.com/debian stable main' \
| sudo tee /etc/apt/sources.list.d/influxdata.list
```

---

## 📦 Instalar InfluxDB

```sh
sudo apt-get update && sudo apt-get install influxdb2
```

---

## ▶ Iniciar y verificar el estado del servicio

```sh
# Iniciar el servicio de InfluxDB
sudo service repository.influxdb start

# Verificar que InfluxDB está corriendo
sudo service repository.influxdb status
```

Si el servicio no se inicia automáticamente después de reiniciar el sistema, habilítalo con:

```sh
sudo systemctl enable repository.influxdb
```

---

## ⚙ Configurar opciones adicionales

Para modificar la configuración del servicio **InfluxDB**, edita el archivo de configuración en:

```sh
sudo nano /etc/default/influxdb2
```

Ejemplo de variables de configuración:

```sh
ARG1="--http-bind-address :8087"
ARG2="--storage-wal-fsync-delay=15m"
```

Luego, edita el archivo del servicio en **systemd** para aplicar las variables:

```sh
sudo nano /lib/systemd/system/repository.influxdb.service
```

Modifica la línea `ExecStart` para incluir las variables:

```sh
ExecStart=/usr/bin/influxd $ARG1 $ARG2
```

Finalmente, recarga los servicios para aplicar los cambios:

```sh
sudo systemctl daemon-reload
sudo systemctl restart repository.influxdb
```

---

## ✅ **InfluxDB instalado y configurado correctamente**

Ahora puedes acceder a la **CLI de InfluxDB** con:

```sh
influx setup
```
¡InfluxDB está listo para almacenar y gestionar tus datos! 🚀
