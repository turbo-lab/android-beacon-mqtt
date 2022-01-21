# Beacon MQTT

[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=9C6TJX2SCA3BC&source=url)
[![Donate](https://img.shields.io/badge/donate-Yandex-green.svg)](https://money.yandex.ru/to/41001690673042)

Beacon MQTT is the simple android application for notifying MQTT server when iBeacon is in range or lost.

Beacon MQTT will be useful for Home Assistant users and interacts with HA through the [MQTT Device Tracker module](https://www.home-assistant.io/components/device_tracker.mqtt/).

![combo.png](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/combo.png)

[Main screen](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/1.png)
 | [New beacon](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/2.png)
 | [Timing settings](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/4.png)
 | [MQTT settings](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/5.png)

# Installation

Since v0.2 Beacon MQTT is available on Google play.

<a href="https://play.google.com/store/apps/details?id=org.turbo.beaconmqtt">
  <img alt="Get it on Google Play"
       src="https://developer.android.com/images/brand/en_generic_rgb_wo_60.png" />
</a>

# Before adding the first beacon
The detecting process is time critical. You have to disable any application activity restrictions in your android. Without this action you will have lots of false beacon losses.

# Beacons database
Great! You are ready to add your first beacon. Press "ADD NEW BEACON" button and wait some time. You will see all beacons in the range. You can differ beacons by the distance. Tap on beacon which you want to add. In dialog write the name of beacon and click save.

Note for HA users: you should enter the same name as the name of corresponding zone. See [MQTT Device Tracker](https://www.home-assistant.io/components/device_tracker.mqtt/) for more details.

Also you can add beacons that are not in range. Click on plus button of action bar and fill fields manually.

![beacons.png](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/beacons.png)

[Scan in process](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/6.png)
 | [New beacon](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/2.png)
 | [Context menu](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/8.png)
 | [Delete beacon](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/3.png)
 | [Edit beacon](https://raw.githubusercontent.com/turbo-lab/beacon_mqtt/gh-pages/screenshots/9.png)

To modify or delete the beacon just use long press on it to show context menu.

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
* Only iBeacon is supported now
* Just 4 beacons can be added
* You shouldn't have overlapped beacons

# Known issues
Please visit my [github page](https://github.com/turbo-lab/android-beacon-mqtt/issues) to list known issues. Do not hesitate to open new ones. Thanks in advance for your feedback, guys!

# Privacy Policy

* The app collects information about BLE beacons around your phone. Let's call it location data.
* The app doesn’t store location data
* The app doesn’t forward location data to third parties
* The location data (like name, mac address, UUIDs, RSSI of beacons) forwards to your MQTT server only
* MQTT server settings are stored in app's secure area and used to establish link to your server only
