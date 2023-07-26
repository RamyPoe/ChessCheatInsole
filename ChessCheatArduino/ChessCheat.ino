
#include <Wire.h>
#include <SoftwareSerial.h>

// Bluetooth UART
SoftwareSerial bluetooth(8, 4); //Rx,Tx

// Pins
#define buttonPin 2
#define buzzerPin 6

// Game logic
boolean startAsWhite = false;
String move;

// Settings
const int longWaitTime = 2000;
const int buzzerStrength = 200;

/* SETUP */
void setup() {

  // Turn on led
  pinMode(LED_BUILTIN, OUTPUT);
  digitalWrite(LED_BUILTIN, HIGH);

  // Setup buzzer
  pinMode(buzzerPin, OUTPUT);

  // Setup button
  pinMode(buttonPin, INPUT_PULLUP);
  delay(1000);

  // Read for starting position
  startAsWhite = digitalRead(buttonPin) == HIGH ? true : false;

  // Start UART
  Serial.begin(9600);
  bluetooth.begin(9600);

  // Let user know they can let go of button
  buzzShort();

  // Let bluetooth connection form
  Serial.println("READY!");
  delay(5000);
  buzzNumber(2);
  while (digitalRead(buttonPin) == LOW) {}
  while (digitalRead(buttonPin) == HIGH) {}

  // Notify the start
  buzzLong();
  delay(600);
  buzzNumber(startAsWhite ? 1 : 2);

  // Starting config
  if (startAsWhite) {
    Serial.println("STARTING AS WHITE");
    sendPacket("wss");
    move = receivePacket();
    buzzMove(move);

  } else {
    Serial.println("STARTING AS BLACK");
    sendPacket("bss");
    
  }

}

/* LOOP */
void loop() {

  // As long as the moves are valid
  boolean validMove = false;
  do {

    // Try to send move
    move = getMoveButton();
    sendPacket(move);

    // Get response
    String resp = receivePacket();

    if (resp == "ok") {
      validMove = true;
    } else {
      buzzBadMove();
    }

  } while (!validMove);


  // We get best move buzzed
  move = receivePacket();
  buzzMove(move);
  
}

/* Get the four presses for a move */
String getMoveButton() {

  // Get 4 data points
  int m1 = getButtonPressNum();
  int m2 = getButtonPressNum();
  int m3 = getButtonPressNum();
  int m4 = getButtonPressNum();

  // String to be returned
  String out = "";

  // Convert move to string
  out += char(m1 + 96);
  out += String(m2);
  out += char(m3 + 96);
  out += String(m4);

  // Return
  return out;

}

/* Blocks until consecutive clicks */
int getButtonPressNum() {

  // Wait for first presss
  while (digitalRead(buttonPin) == HIGH) {}

  // Vars
  int v = 0;
  long clickTime = 0;
  boolean pressed = false;
  boolean pressedBefore = false;

  // Main loop
  while (true) {

    // Check button state
    pressed = digitalRead(buttonPin) == HIGH ? false : true;

    // Check for click
    if (pressed && !pressedBefore) {
      clickTime = millis();
      v++;
    }

    // Check for phase swap/end
    if (millis() - clickTime > longWaitTime) {
      buzzShort();
      break;
    }

    // For click check
    pressedBefore = pressed;
    delay(100);

  }

  Serial.println(v);
  return v;

}


/* Buzz the given move */
void buzzMove(String move) {

  // Convert to numbers
  int m1 = move.charAt(0) - 96;
  int m2 = int(move.charAt(1)) - 48;
  int m3 = move.charAt(2) - 96;
  int m4 = int(move.charAt(3)) - 48;

  buzzNumber(m1);
  delay(2000);
  buzzNumber(m2);
  delay(2000);
  buzzNumber(m3);
  delay(2000);
  buzzNumber(m4);

}


/* Buzz one number */
void buzzNumber(int n) {

  for (int i = 0; i < n; i++) {

    // Buzz
    for (int j = 0; j < 30; j++) {
      analogWrite(buzzerPin, (j/30.0)*buzzerStrength);
      delay(10);
    }
    analogWrite(buzzerPin, 0);

    // Wait
    delay(400);

  }

}

/* Long buzz */
void buzzLong() {

  // Buzz
  for (int i = 0; i < 50; i++) {
    analogWrite(buzzerPin, (i/50.0)*buzzerStrength);
    delay(10);
  }
  analogWrite(buzzerPin, buzzerStrength);
  delay(500);
  analogWrite(buzzerPin, 0);

}

/* Short buzz */
void buzzShort() {

  // Buzz
  for (int i = 0; i < 20; i++) {
    analogWrite(buzzerPin, (i/20.0)*buzzerStrength);
    delay(10);
  }
  analogWrite(buzzerPin, 0);

}

/* Signify a mistake */
void buzzBadMove() {

  // Buzz
  buzzShort();
  delay(110);
  buzzShort();
  delay(110);
  buzzShort();
  delay(110);
  buzzShort();
  delay(110);
  buzzShort();
  delay(110);
  buzzShort();

}


/* Send bluetooth packet to device */
void sendPacket(String data) {

  String out = "...";
  out = out + data;
  out = out + "!";

  bluetooth.print(out);

  // Debug
  Serial.print("SENDING: "); Serial.println(out);

}

/* Receive a packet */
String receivePacket() {  

  // Empty buffer
  while (bluetooth.available() > 0) { bluetooth.read(); delay(2); }
  Serial.println(bluetooth.available());

  // Wait for data
  while (bluetooth.available() == 0) {}
  delay(400);

  // Vars
  boolean read_done = false;
  String out = "";

  // Main loop
  while (bluetooth.available() > 0) {

    char c = bluetooth.read();

    Serial.print(c);

    if (c == '!') {
      read_done = true;
      break;
    } else {
      out = out + c;
    }

  }

  // Empty buffer
  while (bluetooth.available() > 0) { bluetooth.read(); delay(2); }

  // Packet data lost
  if (!read_done) { return ""; }
  
  Serial.print("GOT: "); Serial.println(out);

  // Return good packet
  out.replace(".", "");
  return out;
}


