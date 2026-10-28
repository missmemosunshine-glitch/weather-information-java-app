# Weather Information App

## Overview
A Java Swing weather application developed for the UoPeople Programming Unit 8 assignment.

## API
The app uses the Open-Meteo Geocoding API to convert a city/location into coordinates and the Open-Meteo Weather Forecast API to retrieve current weather and a five-day forecast. Open-Meteo supports non-commercial use without an API key.

## Requirements
- Java 17 or later
- Maven 3.8 or later
- Internet connection

## Run
From the project root:

    mvn clean compile exec:java

## Features
- Search by city/location
- Current temperature, humidity, wind speed and conditions
- Five-day forecast
- Celsius/Fahrenheit conversion
- km/h and mph conversion
- Weather icons
- Dynamic background based on time of day
- Recent search history with timestamps
- Error handling for invalid locations and API/network failures
- SwingWorker keeps the GUI responsive during API calls

## Screenshots
Run the application and capture screenshots showing the main weather display, unit conversions, forecast/history, and an invalid-location error.

## Academic references
Eck, D. J. (2022). *Introduction to programming using Java version 9, JavaFX edition*. https://math.hws.edu/javanotes/

Morelli, R., & Wade, R. (n.d.). *Java, Java, Java: Object-oriented programming*. https://eng.libretexts.org/Bookshelves/Computer_Science/Programming_Languages/Java_Java_Java_-_Object-Oriented_Programming_(Morelli_and_Walde)

Open-Meteo. (n.d.-a). *Geocoding API*. https://open-meteo.com/en/docs/geocoding-api

Open-Meteo. (n.d.-b). *Weather forecast API*. https://open-meteo.com/en/docs
