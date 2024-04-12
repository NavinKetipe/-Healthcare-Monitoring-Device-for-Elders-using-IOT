#include <ArduinoJson.h>
#include <Adafruit_MPU6050.h>
#include <Wire.h>
#include <SPI.h>
#include <Adafruit_BMP280.h>
#include "MAX30105.h"
#include "heartRate.h"
#include <WiFi.h>
#include <FirebaseESP32.h>
#include <Arduino.h>
#include <ESPAsyncWebServer.h>
//Provide the token generation process info.
#include "addons/TokenHelper.h"
//Provide the RTDB payload printing info and other helper functions.
#include "addons/RTDBHelper.h"

// Insert your network credentials
#define WIFI_SSID "Starlink-2.4G"
#define WIFI_PASSWORD "Rksecu_4"

#define API_KEY "AIzaSyDSCCwmVPMFZCwig5jI2liL_Wq0cXOdkYw"
#define DATABASE_URL "https://iotnsbm-9c86b-default-rtdb.asia-southeast1.firebasedatabase.app/"

//Define Firebase Data object
FirebaseData fbdo;

FirebaseAuth auth;
FirebaseConfig config;

unsigned long sendDataPrevMillis = 0;
int count = 0;
bool signupOK = false;

const byte RATE_SIZE = 4; //Increase this for more averaging. 4 is good.
byte rates[RATE_SIZE]; //Array of heart rates
byte rateSpot = 0;
long lastBeat = 0; //Time at which the last beat occurred

float beatsPerMinute;
int beatAvg;

AsyncWebServer server(80);

#define BMP_SCK  (13)
#define BMP_MISO (12)
#define BMP_MOSI (11)
#define BMP_CS   (10)

Adafruit_BMP280 bmp; // I2C
Adafruit_MPU6050 mpu;
MAX30105 particleSensor;

int range = 4;

void setupBMP(){
  while ( !Serial ) delay(100);   // wait for native usb
  Serial.println(F("BMP280 Init"));
  unsigned status;
  //status = bmp.begin(BMP280_ADDRESS_ALT, BMP280_CHIPID);
  status = bmp.begin();
  if (!status) {
    Serial.println(F("Could not find a valid BMP280 sensor, check wiring or "
                      "try a different address!"));
    Serial.print("SensorID was: 0x"); Serial.println(bmp.sensorID(),16);
    Serial.print("        ID of 0xFF probably means a bad address, a BMP 180 or BMP 085\n");
    Serial.print("   ID of 0x56-0x58 represents a BMP 280,\n");
    Serial.print("        ID of 0x60 represents a BME 280.\n");
    Serial.print("        ID of 0x61 represents a BME 680.\n");
    while (1) delay(10);
  }

  /* Default settings from datasheet. */
  bmp.setSampling(Adafruit_BMP280::MODE_NORMAL,     /* Operating Mode. */
                  Adafruit_BMP280::SAMPLING_X2,     /* Temp. oversampling */
                  Adafruit_BMP280::SAMPLING_X16,    /* Pressure oversampling */
                  Adafruit_BMP280::FILTER_X16,      /* Filtering. */
                  Adafruit_BMP280::STANDBY_MS_500); /* Standby time. */
}

void outBMP(){

    FirebaseJson bmpsensor_json;

    bmpsensor_json.add("Temperature", bmp.readTemperature());
    bmpsensor_json.add("Pressure", bmp.readPressure());
    bmpsensor_json.add("Approxaltitude", bmp.readAltitude(1013.25));

    if (Firebase.ready() && signupOK && (millis() - sendDataPrevMillis > 150 || sendDataPrevMillis == 0)){
    sendDataPrevMillis = millis();

    if (Firebase.setJSON(fbdo,"/bpmsensor", bmpsensor_json)) {
      Serial.println("PASSED");
      Serial.println("PATH: bpmsensor/JSON");
      Serial.println("TYPE: application/json");
    } else {
      Serial.println("FAILED");
      Serial.println("REASON: " + fbdo.errorReason());
    }
    }

    Serial.print(F("Temperature = "));
    Serial.print(bmp.readTemperature());
    Serial.println(" *C");

    Serial.print(F("Pressure = "));
    Serial.print(bmp.readPressure());
    Serial.println(" Pa");

    Serial.print(F(" = "));
    Serial.print(bmp.readAltitude(1013.25)); /* Adjusted to local forecast! */
    Serial.println(" m");

    Serial.println();
    delay(100);

}

void setupHR(){
    // Initialize sensor
  if (!particleSensor.begin(Wire, I2C_SPEED_FAST)) //Use default I2C port, 400kHz speed
  {
    Serial.println("MAX30105 was not found. Please check wiring/power. ");
    while (1);
  }
  Serial.println("Place your index finger on the sensor with steady pressure.");

  particleSensor.setup(); //Configure sensor with default settings
  particleSensor.setPulseAmplitudeRed(0x0A); //Turn Red LED to low to indicate sensor is running
  particleSensor.setPulseAmplitudeGreen(0); //Turn off Green LED
}

void getHeartRate(){
  long irValue = particleSensor.getIR();

  if (checkForBeat(irValue) == true)
  {
    //We sensed a beat!
    long delta = millis() - lastBeat;
    lastBeat = millis();

    beatsPerMinute = 60 / (delta / 1000.0);

    if (beatsPerMinute < 255 && beatsPerMinute > 20)
    {
      rates[rateSpot++] = (byte)beatsPerMinute; //Store this reading in the array
      rateSpot %= RATE_SIZE; //Wrap variable

      //Take average of readings
      beatAvg = 0;
      for (byte x = 0 ; x < RATE_SIZE ; x++)
        beatAvg += rates[x];
      beatAvg /= RATE_SIZE;
    }
  }

  FirebaseJson oximeter_json;

  oximeter_json.add("IR", irValue);
  oximeter_json.add("BPM", beatsPerMinute);
  oximeter_json.add("AvgBPM", beatAvg);

   if (Firebase.ready() && signupOK && (millis() - sendDataPrevMillis > 150 || sendDataPrevMillis == 0)){
    sendDataPrevMillis = millis();

    if (Firebase.setJSON(fbdo,"/oximeter", oximeter_json)) {
      Serial.println("PASSED");
      Serial.println("PATH: oximeter/JSON");
      Serial.println("TYPE: application/json");
    } else {
      Serial.println("FAILED");
      Serial.println("REASON: " + fbdo.errorReason());
    }
    }

  Serial.print("IR=");
  Serial.print(irValue);
  Serial.print(", BPM=");
  Serial.print(beatsPerMinute);
  Serial.print(", Avg BPM=");
  Serial.print(beatAvg);

  if (irValue < 50000)
    Serial.print(" No finger?");

  Serial.println();
}

void setupAXO(){
  while (!Serial)
    delay(10); // will pause Zero, Leonardo, etc until serial console opens

  Serial.println("Adafruit MPU6050 test!");

  // Try to initialize!
  if (!mpu.begin()) {
    Serial.println("Failed to find MPU6050 chip");
    while (1) {
      delay(10);
    }
  }
  Serial.println("MPU6050 Found!");

  //setupt motion detection
  mpu.setHighPassFilter(MPU6050_HIGHPASS_0_63_HZ);
  mpu.setMotionDetectionThreshold(1);
  mpu.setMotionDetectionDuration(20);
  mpu.setInterruptPinLatch(true);	// Keep it latched.  Will turn off when reinitialized.
  mpu.setInterruptPinPolarity(true);
  mpu.setMotionInterrupt(true);

  Serial.println("");
  delay(100);
}

void useAXIO() {
  if (mpu.getMotionInterruptStatus()) {
    /* Get new sensor events with the readings */
    sensors_event_t a, g, temp;
    mpu.getEvent(&a, &g, &temp);

    // Create a JSON object
    FirebaseJson accelerometer_json;

    // Populate JSON object with sensor data
    accelerometer_json.add("AccelX", a.acceleration.x);
    accelerometer_json.add("AccelY", a.acceleration.y);
    accelerometer_json.add("AccelZ", a.acceleration.z);
    accelerometer_json.add("GyroX", g.gyro.x);
    accelerometer_json.add("GyroY", g.gyro.y);
    accelerometer_json.add("GyroZ", g.gyro.z);

    // Serialize JSON object to string
    String jsonString;
     if (Firebase.ready() && signupOK && (millis() - sendDataPrevMillis > 150 || sendDataPrevMillis == 0)){
    sendDataPrevMillis = millis();

    if (Firebase.setJSON(fbdo,"/accelerometer", accelerometer_json)) {
      Serial.println("PASSED");
      Serial.println("PATH: accelerometer/JSON");
      Serial.println("TYPE: application/json");
    } else {
      Serial.println("FAILED");
      Serial.println("REASON: " + fbdo.errorReason());
    }
    }
  }

  delay(100);
}


void setupFirebase(){
  /* Assign the api key (required) */
  config.api_key = API_KEY;

  /* Assign the RTDB URL (required) */
  config.database_url = DATABASE_URL;

  /* Sign up */
  if (Firebase.signUp(&config, &auth, "", "")){
    Serial.println("ok");
    signupOK = true;
  }
  else{
    Serial.printf("%s\n", config.signer.signupError.message.c_str());
  }

  /* Assign the callback function for the long running token generation task */
  config.token_status_callback = tokenStatusCallback; //see addons/TokenHelper.h
  
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
}


void setupWIFI(){
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    Serial.println("\nConnecting");

    while(WiFi.status() != WL_CONNECTED){
        Serial.print(".");
        delay(100);
    }

    Serial.println("\nConnected to the WiFi network");
    Serial.print("Local ESP32 IP: ");
    Serial.println(WiFi.localIP());
    setupFirebase();
    FirebaseJson ip_json;

    ip_json.add("IP", WiFi.localIP().toString());
    if (Firebase.ready() && signupOK && (millis() - sendDataPrevMillis > 150 || sendDataPrevMillis == 0)){
    sendDataPrevMillis = millis();

    if (Firebase.setJSON(fbdo,"/IP", ip_json)) {
      Serial.println("PASSED");
      Serial.println("PATH: IP/JSON");
      Serial.println("TYPE: application/json");
    } else {
      Serial.println("FAILED");
      Serial.println("REASON: " + fbdo.errorReason());
    }
}}

void setup() {
  Serial.begin(115200);
  delay(1000);
  setupWIFI();
  setupBMP();
  setupHR();
  setupAXO();

  // Print the ESP32's IP address
  Serial.print("ESP32 Web Server's IP address: ");
  Serial.println(WiFi.localIP());

  // Define a route to serve the HTML page
  server.on("/", HTTP_GET, [](AsyncWebServerRequest* request) {
    Serial.println("ESP32 Web Server: New request received:");  // for debugging
    Serial.println("GET /");        // for debugging
    request->send(200, "text/html", "<html><body><h1>Hello, ESP32!</h1></body></html>");
  });

  // Define routes
server.on("/bpm", HTTP_GET, [](AsyncWebServerRequest* request) {
  Serial.println("ESP32 Web Server: New request received for /bpm");  // for debugging
  range = 1;
  request->send(200, "text/plain", "BPM Started"); // Send response
});

server.on("/hr", HTTP_GET, [](AsyncWebServerRequest* request) {
  Serial.println("ESP32 Web Server: New request received for /hr");  // for debugging
  range = 0;
  request->send(200, "text/plain", "HR started"); // Send response
});

server.on("/free", HTTP_GET, [](AsyncWebServerRequest* request) {
  Serial.println("ESP32 Web Server: New request received for /hr");  // for debugging
  range = 4;
  request->send(200, "text/plain", "Module in free state"); // Send response
});

server.on("/axo", HTTP_GET, [](AsyncWebServerRequest* request) {
  Serial.println("ESP32 Web Server: New request received for /axo");  // for debugging
  range = 2;
  request->send(200, "text/plain", "AXO Started"); // Send response
});

  // Start the server
  server.begin();

  setupFirebase();
  setupBMP();
  setupHR();
  setupAXO();

}

void loop() {

    switch (range) {
      case 0:  // your hand is on the sensor
        Serial.println("Heart Rate Reading..");
        getHeartRate();
        break;
      case 1:  // your hand is close to the sensor
        Serial.println("BMP Reading..");
        outBMP();
        break;
      case 2:  // your hand is a few inches from the sensor
        Serial.println("Axio reading..");
        useAXIO();
        break;
      case 4:  // your hand is a few inches from the sensor
        Serial.println("No request yet");
        break;
      default: // handle invalid input
        Serial.println("Invalid input. Please enter a value between 0 and 2.");
        break;
    }
  
  delay(20);  // delay in between reads for stability



}
