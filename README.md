# EV-Retina
EV-Retina is a real-time distributed mobile social networking system which matches nearby users' electronic (E) data with their visual (V) data to determine their identities and 
facilitate communications. We partition the electronic data into two parts: static data (e.g., facial features and gender) and dynamic data (e.g., apparel, movement). Users' 
static data is stored in our cloud storage while their dynamic data is maintained and updated by themselves. Our system transforms users' dynamic data into electronic data and 
broadcasts the electronic data using Bluetooth Low Energy (BLE) beacons. Since BLE beacons have limited transmission capacity, we aim to transmit as little dynamic data as 
possible. Users can identify other users by matching the visual data from their camera view with the static data from the cloud and dynamic data from the BLE beacons. However, it 
is highly likely that identifications through static data may not work due to face coverings or lack of static data, and identification through dynamic data may not work either 
if multiple users have similar appearances. Our system manages to solve this issue by proposing multi-tier appearance-based identification.
## Environment
This application is currently designed to run on Android Devices. The BLE portion is written using Android BLE libraries. The Facial recognition portion relies on facial recognition 
technology provided by the [face_recognition API](https://github.com/ageitgey/face_recognition). The dynamic data matching is provided by TensorFlow.
## Installation/Running
This application is meant to run on Android Devices. Thus, in order to install the application, you need to install Android Studio. Once Android Studio is installed, you can install 
the application on your device. You will need to give the application the following permissions to ensure that the application runs as intended.
* BLUETOOTH
* BLUETOOTH_ADMIN
* ACCESS_FINE_LOCATION
* INTERNET
* WRITE_EXTERNAL_STORAGE
* CAMERA
* ACTIVITY_RECOGNITION
## Parameters
For this application, the adjustable parameters are all regarding the BLE Beacon Broadcasting. They are described below:
* Advertising Interval: The interval between packet advertisiments. This is currently set to high, but can be modified.
* Tx Power Level: Measures the strength of the advertisement signal. This is currently set to medium, but can be modified.
* Minimum advertising time: Minimum amount of time that an entire broadcast will be advertised for. Currently this is set to 20 seconds.
* Broadcast Repeat: Minimum numnber of times that a broadcast will be re-advertised. Currently this is set to 5.
* Maximum Packet Size: Largest possible size for a single advertisement. For BLE 5, this was increased to 245. Since this implementation is meant to be for BLE 5, it is currently set to 245. If the implementer would like to decrease this value, he/she may do so. However, this value cannot be increased.
* Packet Advertisement Duration: Time that each packet is advertised for. Currently set to 420 ms. This value can be modified, but it may affect performance depending on how many devices are advertising and how large the broadcasts are.
* Scan Mode: Latency of the scan. Currently set to low latency.
## Important Functions/Methods
* Fragmenter.advertise: This function is where the actual beacon broadcasting is performed. In this function, the entire broadcast is broken into advertisements with a size equal to the 
Maximum Packet size. This function takes parameters that include Advertise Settings, Advertising callbacks, and Maximum Packet Size.
* Assembler.gather: his function is called everytime a packet is received. In this function, packet data is stored for each device and updated as new packets arrive. The function takes a 
byte array of Max Packet Size as a parameter.
* Capture.connectServer: This function connects the client application to the cloud server for sending photos and name data.
* Capture.postRequest: This function attempts to send the photo to the cloud server.
* Capture.sendNames: This function sends the names that the device has received from nearby users to the cloud server.
## Issues
Currently, the application has 2 portions that are not fully operation: The Activity Detection subsystem and the Augmented Reality subsection. As they are implemented now, they should not be considered 
a functional part of EV-Retina.
## Future Work
The main aspects of this project that should be worked on in the future are the Activity Detection subsystem and the Augmented Reality subsystem.
### Activity Detection
The Activity Detection Subsystem currently uses the Google ActivityRecognition API for activity detection. This API has proved not very responsive during testing. To improve this subsystem, future implementers 
could look into more responsive APIs.
### Augmented Reality Subsystem
The Augmented Reality subsystem is currently implemented using Google ARCore. Currently, the only functionality is showing an AR image in the camera view. In the future, implementers should focus on displaying images 
that can be sent over BLE.
