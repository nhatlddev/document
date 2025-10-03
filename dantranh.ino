// Dan Tranh Auto Tuner - ADXL345 (any orientation) + OLED + EEPROM + A4988
// Buttons pins: BUTTON_UP=14, BUTTON_DOWN=12, BUTTON_AUTO=13 (menu), BUTTON_SELECT=2 (auto/manual)

#include <Arduino.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_ADXL345_U.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <EEPROM.h>
#include <arduinoFFT.h>

// ----------------- HARDWARE PINS -----------------
#define I2C_SDA 21
#define I2C_SCL 22

#define STEP_PIN 16
#define DIR_PIN 5
#define ENABLE_PIN 4

#define BUTTON_UP 14
#define BUTTON_DOWN 12
#define BUTTON_AUTO 13
#define BUTTON_SELECT 2

#define LED_RED_PIN 25
#define LED_GREEN_PIN 26
#define LED_BLUE_PIN 27
#define BUZZER_PIN 32

// ----------------- OLED -----------------
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET -1
#define OLED_ADDR 0x3C
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

// ----------------- ADXL345 -----------------
Adafruit_ADXL345_Unified accel = Adafruit_ADXL345_Unified(12345);

// ----------------- FFT -----------------
const uint16_t SAMPLES = 256;
const double SAMPLING_FREQUENCY = 2000.0;
double vReal[SAMPLES];
double vImag[SAMPLES];
ArduinoFFT<double> FFT(vReal, vImag, SAMPLES, SAMPLING_FREQUENCY);

// ----------------- Strings (30) -----------------
const int NUM_STRINGS = 30;
double standardFreq[NUM_STRINGS] = {
  196, 207, 220, 233, 247, 262, 277, 294,
  311, 330, 349, 370, 392, 415, 440, 466,
  494, 523, 554, 587, 622, 659, 698, 740,
  784, 830, 880, 932, 988, 1047
};
const char* stringNames[NUM_STRINGS] = {
  "D3", "Eb3", "E3", "F3", "F#3", "G3", "G#3", "A3",
  "A#3", "B3", "C4", "C#4", "D4", "D#4", "E4", "F4",
  "F#4", "G4", "G#4", "A4", "A#4", "B4", "C5", "C#5",
  "D5", "D#5", "E5", "F5", "F#5", "G5"
};

// ----------------- EEPROM (motor positions) -----------------
int motorPosition[NUM_STRINGS];
const int EEPROM_SIZE = NUM_STRINGS * sizeof(int);

const int MOTOR_MIN = -5000;
const int MOTOR_MAX = 5000;

// ----------------- State -----------------
int currentString = 14;  // default C4-ish
bool autoMode = true;
bool menuMode = false;

int detectedString = -1;
int autoStage = 0;
unsigned long lastPluckTime = 0;
const unsigned long AUTO_TIMEOUT = 5000UL;

// stability filter
const int FREQ_TOLERANCE = 5;
const unsigned long STABLE_TIME_MS = 500;
double lastFreqSample = 0;
unsigned long stableStart = 0;
double stableFreq = 0;

const unsigned long BTN_DEBOUNCE = 50;

const unsigned long STEP_DELAY_US = 1200;

struct ButtonState {
  unsigned long lastDebounceTime = 0;
  int lastButtonState = HIGH;
};

ButtonState btnSelectState;
ButtonState btnUpState;
ButtonState btnDownState;
ButtonState btnAutoState;

// ----------------- Utility -----------------
void stepMotorSteps(int steps, bool dir, unsigned long stepDelayUs = STEP_DELAY_US) {
  digitalWrite(DIR_PIN, dir);
  for (int i = 0; i < steps; ++i) {
    digitalWrite(STEP_PIN, HIGH);
    delayMicroseconds(stepDelayUs);
    digitalWrite(STEP_PIN, LOW);
    delayMicroseconds(stepDelayUs);

    if (dir) {
      if (motorPosition[currentString] < MOTOR_MAX) {
        motorPosition[currentString]++;
      }
    } else {
      if (motorPosition[currentString] > MOTOR_MIN) {
        motorPosition[currentString]--;
      }
    }
  }
  int addr = currentString * sizeof(int);
  EEPROM.put(addr, motorPosition[currentString]);
  EEPROM.commit();
}

void captureSamplesRemoveDC() {
  sensors_event_t event;
  double sum = 0.0;
  for (uint16_t i = 0; i < SAMPLES; ++i) {
    accel.getEvent(&event);
    double x = event.acceleration.x;
    double y = event.acceleration.y;
    double z = event.acceleration.z;
    double mag = sqrt(x * x + y * y + z * z);
    vReal[i] = mag;
    vImag[i] = 0.0;
    sum += mag;
    delayMicroseconds((uint32_t)(1000000.0 / SAMPLING_FREQUENCY));
  }
  double mean = sum / SAMPLES;
  for (uint16_t i = 0; i < SAMPLES; ++i) vReal[i] -= mean;
}

double detectFundamentalHz() {
  FFT.windowing(FFTWindow::Hamming, FFTDirection::Forward);
  FFT.compute(FFTDirection::Forward);
  FFT.complexToMagnitude();
  double peak = 0.0;
  int peakIndex = 0;
  for (uint16_t i = 2; i < SAMPLES / 2; ++i) {
    if (vReal[i] > peak) {
      peak = vReal[i];
      peakIndex = i;
    }
  }
  double freq = (peakIndex * SAMPLING_FREQUENCY) / SAMPLES;
  return freq;
}

int getMedianFrequency(int reads = 7) {
  const int MAXR = 15;
  if (reads > MAXR) reads = MAXR;
  int tmp[MAXR];
  int cnt = 0;
  for (int i = 0; i < reads; ++i) {
    captureSamplesRemoveDC();
    double f = detectFundamentalHz();
    if (f >= 30 && f <= 2000) {
      tmp[cnt++] = (int)round(f);
    }
    delay(15);
  }
  if (cnt == 0) return 0;
  for (int a = 0; a < cnt - 1; ++a) {
    for (int b = a + 1; b < cnt; ++b) {
      if (tmp[a] > tmp[b]) {
        int t = tmp[a];
        tmp[a] = tmp[b];
        tmp[b] = t;
      }
    }
  }
  return tmp[cnt / 2];
}

int detectStringFromFreq(int freq, double maxDiffHz = 15.0) {
  int best = -1;
  double mind = 1e9;
  for (int i = 0; i < NUM_STRINGS; ++i) {
    double d = fabs((double)freq - standardFreq[i]);
    if (d < mind) {
      mind = d;
      best = i;
    }
  }
  if (mind <= maxDiffHz) return best;
  return -1;
}

void feedLedBuzzer(int diffHz) {
  if (abs(diffHz) <= 2) {
    digitalWrite(LED_RED_PIN, LOW);
    digitalWrite(LED_BLUE_PIN, LOW);
    digitalWrite(LED_GREEN_PIN, HIGH);
    tone(BUZZER_PIN, 1000, 80);
  } else if (diffHz < 0) {
    digitalWrite(LED_RED_PIN, HIGH);
    digitalWrite(LED_GREEN_PIN, LOW);
    digitalWrite(LED_BLUE_PIN, LOW);
  } else {
    digitalWrite(LED_RED_PIN, LOW);
    digitalWrite(LED_GREEN_PIN, LOW);
    digitalWrite(LED_BLUE_PIN, HIGH);
  }
}

void drawOLED(double freq, double target, const char* name, int diff) {
  display.clearDisplay();
  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE);

  display.setCursor(0, 0);
  display.print("Str: ");
  display.print(name);

  display.setCursor(80, 0);
  display.print(autoMode ? "A+M" : "M");

  display.setCursor(0, 12);
  display.print("Target: ");
  display.print(target, 1);
  display.print(" Hz");

  display.setCursor(0, 24);
  display.print("Freq: ");
  if (freq > 0.5) display.print(freq, 1);
  else display.print("---");
  display.print(" Hz");

  int center = SCREEN_WIDTH / 2;
  int barWidth = 4;
  int pos = map(diff, -30, 30, -50, 50);
  pos = constrain(pos, -60, 60);
  display.drawLine(center, 55, center, 63, SSD1306_WHITE);
  display.fillRect(center + pos - barWidth / 2, 55, barWidth, 8, SSD1306_WHITE);

  display.setCursor(96, 12);
  if (abs(diff) <= 2) display.print("OK");
  else if (diff < 0) display.print("^");
  else display.print("v");

  if (autoMode) {
    display.setCursor(0, 36);
    if (autoStage == 1) display.print("Auto: PLUCK 1");
    else if (autoStage == 2) display.print("Auto: PLUCK 2 -> adjust");
    else display.print("Auto: idle");
  }

  display.display();
}

void showMenu() {
  display.clearDisplay();
  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE);
  display.setCursor(0, 0);
  display.print("Manual Select String");
  int start = currentString - 2;
  if (start < 0) start = 0;
  if (start > NUM_STRINGS - 5) start = NUM_STRINGS - 5;
  for (int i = 0; i < 5; ++i) {
    int idx = start + i;
    int y = 12 + i * 10;
    if (idx == currentString) {
      display.fillRect(0, y, 128, 10, SSD1306_WHITE);
      display.setTextColor(SSD1306_BLACK);
      display.setCursor(2, y);
      display.print(stringNames[idx]);
      display.setTextColor(SSD1306_WHITE);
    } else {
      display.setCursor(2, y);
      display.print(stringNames[idx]);
    }
  }
  display.display();
}

bool pressedOnce(uint8_t pin, ButtonState& state) {
  int reading = digitalRead(pin);

  if (reading != state.lastButtonState) {
    state.lastDebounceTime = millis();
    Serial.print("Button "); Serial.print(pin); Serial.print(" changed to: "); Serial.println(reading ? "HIGH" : "LOW");
  }

  if ((millis() - state.lastDebounceTime) > BTN_DEBOUNCE) {
    if (reading == LOW && state.lastButtonState == HIGH) {
      state.lastButtonState = reading;
      Serial.println("DEBounced PRESS detected!");
      return true;
    }
  }

  state.lastButtonState = reading;
  return false;
}

void setup() {
  Serial.begin(115200);

  EEPROM.begin(EEPROM_SIZE);
  for (int i = 0; i < NUM_STRINGS; ++i) {
    int addr = i * sizeof(int);
    EEPROM.get(addr, motorPosition[i]);
    if (motorPosition[i] < -1000000 || motorPosition[i] > 1000000) motorPosition[i] = 0;
  }

  Wire.begin(I2C_SDA, I2C_SCL);
  if (!display.begin(SSD1306_SWITCHCAPVCC, OLED_ADDR)) {
    Serial.println("OLED init failed");
    while (1) delay(500);
  }
  display.clearDisplay();
  display.display();

  if (!accel.begin()) {
    Serial.println("ADXL345 not found");
    while (1) delay(500);
  }
  accel.setRange(ADXL345_RANGE_16_G);

  pinMode(STEP_PIN, OUTPUT);
  pinMode(DIR_PIN, OUTPUT);
  pinMode(ENABLE_PIN, OUTPUT);
  digitalWrite(ENABLE_PIN, LOW);

  pinMode(LED_RED_PIN, OUTPUT);
  pinMode(LED_GREEN_PIN, OUTPUT);
  pinMode(LED_BLUE_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);

  pinMode(BUTTON_UP, INPUT_PULLUP);
  pinMode(BUTTON_DOWN, INPUT_PULLUP);
  pinMode(BUTTON_AUTO, INPUT_PULLUP);
  pinMode(BUTTON_SELECT, INPUT_PULLUP);

  drawOLED(0, standardFreq[currentString], stringNames[currentString], 0);
  Serial.println("Dan Tranh Tuner ready");
}

void loop() {
  static unsigned long lastDebug = 0;
  if (millis() - lastDebug > 100) {
    int selectReading = digitalRead(BUTTON_SELECT);
    Serial.print("BUTTON_SELECT raw: ");
    Serial.println(selectReading ? "HIGH" : "LOW");
    lastDebug = millis();
  }

  if (pressedOnce(BUTTON_SELECT, btnSelectState)) {
    Serial.println("BUTTON_SELECT PRESSED! Toggling mode..."); 
    autoMode = !autoMode;
    Serial.print("Auto Mode: ");
    Serial.println(autoMode ? "ON" : "OFF");
    menuMode = false;
    autoStage = 0;
    detectedString = -1;
    lastPluckTime = 0;
    drawOLED(0, standardFreq[currentString], stringNames[currentString], 0);
  }

  if (!autoMode && pressedOnce(BUTTON_AUTO, btnAutoState)) {
    menuMode = !menuMode;
    if (menuMode) {
      showMenu();
    } else {
      drawOLED(stableFreq, standardFreq[currentString], stringNames[currentString],
               (int)(stableFreq - standardFreq[currentString]));
    }
  }

  if (menuMode) {
    if (pressedOnce(BUTTON_UP, btnUpState)) {
      currentString = (currentString - 1 + NUM_STRINGS) % NUM_STRINGS;
      showMenu();
    }
    if (pressedOnce(BUTTON_DOWN, btnDownState)) {
      currentString = (currentString + 1) % NUM_STRINGS;
      showMenu();
    }
    return;
  }

  if (!autoMode) {
    if (pressedOnce(BUTTON_UP, btnUpState)) {
      currentString = (currentString + 1) % NUM_STRINGS;
      drawOLED(0, standardFreq[currentString], stringNames[currentString], 0);
    }
    if (pressedOnce(BUTTON_DOWN, btnDownState)) {
      currentString = (currentString - 1 + NUM_STRINGS) % NUM_STRINGS;
      drawOLED(0, standardFreq[currentString], stringNames[currentString], 0);
    }
  }

  int medianFreq = getMedianFrequency(7);
  if (medianFreq == 0) {
    drawOLED(0, standardFreq[currentString], stringNames[currentString], 0);
    delay(60);
    return;
  }

  Serial.print("Freq: ");
  Serial.print(medianFreq);
  Serial.print(" Hz | String: ");
  int detected = detectStringFromFreq(medianFreq);
  if (detected >= 0) {
    Serial.print(stringNames[detected]);
    Serial.print(" (");
    Serial.print(standardFreq[detected], 1);
    Serial.println(" Hz)");
  } else {
    Serial.println("unknown");
  }

  if (fabs((double)medianFreq - lastFreqSample) <= FREQ_TOLERANCE) {
    if (stableStart == 0) stableStart = millis();
    if ((millis() - stableStart) >= STABLE_TIME_MS) {
      stableFreq = medianFreq;
    }
  } else {
    stableStart = 0;
    stableFreq = 0;
  }
  lastFreqSample = medianFreq;

  if (stableFreq > 0) {
    int best = detectStringFromFreq((int)round(stableFreq), 15.0);
    if (best >= 0) {
      if (autoMode) {
        if (detectedString != best) {
          detectedString = best;
          autoStage = 1;
          lastPluckTime = millis();
        } else {
          if (autoStage == 1) autoStage = 2;

          if (autoStage == 2) {
            double target = standardFreq[best];
            int diffHz = (int)round(stableFreq - target);

            if (abs(diffHz) <= 2) {
              feedLedBuzzer(diffHz);
              drawOLED(stableFreq, target, stringNames[best], diffHz);
              Serial.println("Tuning complete.");
              autoStage = 0;
              detectedString = -1;
            } else {
              if (abs(diffHz) > 20) stepMotorSteps(80, diffHz < 0);
              else if (abs(diffHz) > 5) stepMotorSteps(20, diffHz < 0);
              else stepMotorSteps(6, diffHz < 0);

              drawOLED(stableFreq, target, stringNames[best], diffHz);
              Serial.print("Adjusted by ");
              Serial.print(abs(diffHz) > 20 ? 80 : (abs(diffHz) > 5 ? 20 : 6));
              Serial.println(" steps");
              autoStage = 0;
              detectedString = -1;
            }
          }
        }
        if (millis() - lastPluckTime > AUTO_TIMEOUT) {
          autoStage = 0;
          detectedString = -1;
        }
      } else {
        double target = standardFreq[currentString];
        int diffHz = (int)round(stableFreq - target);
        feedLedBuzzer(diffHz);
        drawOLED(stableFreq, target, stringNames[currentString], diffHz);
      }
    } else {
      drawOLED(stableFreq, standardFreq[currentString], stringNames[currentString],
               (int)round(stableFreq - standardFreq[currentString]));
    }
  } else {
    drawOLED(medianFreq, standardFreq[currentString], stringNames[currentString],
             (int)round(medianFreq - standardFreq[currentString]));
  }

  delay(30);
}
