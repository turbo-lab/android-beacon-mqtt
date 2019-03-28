# Beacon MQTT
Simple android application for notifying MQTT server when iBeacon is in range or lost.

Beacon MQTT will be useful for Home Assistant users. It based on AltBeacon and Paho MQTT libraries. It has ugly UI but works perfectly with [MQTT Device Tracker module](https://www.home-assistant.io/components/device_tracker.mqtt/) of my HA.

![combo.png](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/combo.png)

[Main screen](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/1.png)
 | [New beacon](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/1.png)
 | [Delete beacon](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/1.png)
 | [Timing settings](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/1.png)
 | [MQTT settings](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/1.png)

# Download
You can find latest release of Beacon MQTT on  [github](https://github.com/turbo-lab/android-beacon-mqtt/releases/latest)

# Installation
Because I don't have Google Play developer account yet you should allow installation of apps from unknown sources.

# Before adding the first beacon
The detecting process is time critical. You have to disable any application activity restrictions in your android. Without this you will have lots of false beacon losses. It can take some percentage of a battery.

# Beacons database
Great! You are ready to add your first beacon. Press "ADD NEW BEACON" and enter Name and IDs of beacon. If you don't know your IDs please use additional android applications. I like [Beacon Scanner](https://play.google.com/store/apps/details?id=com.bridou_n.beaconscanner) from Nicolas Bridoux. For HA users: you should enter the same name as the name of corresponding zone. See [MQTT Device Tracker](https://www.home-assistant.io/components/device_tracker.mqtt/) for more details.

To delete the beacon just use long press on it as you running context menu.

# Discovery timings
I suggest next timings:
  * 1000 ... 2000ms for scan
  * 9000 ... 15000ms pause between scans
  * 180000 ... 300000ms loss timeout

This allows you to have a small discovery time and save battery power. Anyway you can play with timings.

# MQTT
You have to configure the parameters of your MQTT server. Toasts will help you with connection state. Beacon MQTT will reconnect to the MQTT server in case of network problems automatically. MQTT messages will be stored in buffer and delivered after reconnect will be done.

I've made templating subsystem for MQTT topic and payload. %mac% - is my WiFi mac. %beacon% - is the name of beacon in saved beacon’s database. These keywords will be changed by actual values before sending.

To be compatible with [MQTT Device Tracker](https://www.home-assistant.io/components/device_tracker.mqtt/) module use next:
* %beacon% - for enter payload
* not_home - for lost payload
* location/%mac% - for both topics

# Events journal
The log screen shows last 1k lines. It helps you with debugging of interaction. You can clear journal by pressing clear button.

# Limitations
* Only iBeacon is supported
* Just 4 beacons can be added
* No autofind beacons feature
* You shouldn't have overlapped beacons

# Known issues
Please visit my [github page](https://github.com/turbo-lab/android-beacon-mqtt/issues) to list known issues. Do not hesitate to open new ones. Thanks in advance for your feedback, guys!

# Donation
If you like Beacon MQTT you can support project development.

 [![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=9C6TJX2SCA3BC&source=url)
