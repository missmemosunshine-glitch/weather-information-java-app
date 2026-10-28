package com.example.weatherapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WeatherApp extends JFrame {
    private final JTextField locationField = new JTextField("Johannesburg", 18);
    private final JComboBox<String> temperatureUnit = new JComboBox<>(new String[]{"Celsius", "Fahrenheit"});
    private final JComboBox<String> windUnit = new JComboBox<>(new String[]{"km/h", "mph"});
    private final JButton searchButton = new JButton("Search");
    private final JLabel locationLabel = new JLabel("Enter a location");
    private final JLabel conditionLabel = new JLabel("—");
    private final JLabel temperatureLabel = new JLabel("—");
    private final JLabel humidityLabel = new JLabel("Humidity: —");
    private final JLabel windLabel = new JLabel("Wind: —");
    private final JLabel updatedLabel = new JLabel("Updated: —");
    private final WeatherIcon weatherIcon = new WeatherIcon();
    private final JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
    private final JPanel currentPanel = new JPanel(new BorderLayout(12, 12));
    private final JPanel forecastPanel = new JPanel(new GridLayout(1, 5, 8, 8));
    private final DefaultListModel<String> historyModel = new DefaultListModel<>();
    private final JList<String> historyList = new JList<>(historyModel);

    private WeatherData lastWeather;

    public WeatherApp() {
        setTitle("Weather Information App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        buildUi();
        searchButton.addActionListener(e -> searchWeather());
        temperatureUnit.addActionListener(e -> refreshUnits());
        windUnit.addActionListener(e -> refreshUnits());
        historyList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String item = historyList.getSelectedValue();
                    if (item != null) {
                        int sep = item.indexOf(" — ");
                        if (sep > 0) {
                            locationField.setText(item.substring(0, sep));
                            searchWeather();
                        }
                    }
                }
            }
        });

        SwingUtilities.invokeLater(this::searchWeather);
    }

    private void buildUi() {
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(mainPanel);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        top.add(new JLabel("City / Location:"));
        top.add(locationField);
        top.add(searchButton);
        top.add(new JLabel("Temperature:"));
        top.add(temperatureUnit);
        top.add(new JLabel("Wind:"));
        top.add(windUnit);
        mainPanel.add(top, BorderLayout.NORTH);

        currentPanel.setBorder(BorderFactory.createTitledBorder("Current Weather"));
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        locationLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        conditionLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        temperatureLabel.setFont(new Font("SansSerif", Font.BOLD, 42));
        text.add(locationLabel);
        text.add(Box.createVerticalStrut(8));
        text.add(conditionLabel);
        text.add(Box.createVerticalStrut(8));
        text.add(temperatureLabel);
        text.add(Box.createVerticalStrut(8));
        text.add(humidityLabel);
        text.add(windLabel);
        text.add(updatedLabel);
        currentPanel.add(text, BorderLayout.CENTER);
        currentPanel.add(weatherIcon, BorderLayout.EAST);
        mainPanel.add(currentPanel, BorderLayout.CENTER);

        forecastPanel.setBorder(BorderFactory.createTitledBorder("5-Day Forecast"));
        for (int i = 0; i < 5; i++) {
            JLabel d = new JLabel("—", SwingConstants.CENTER);
            d.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            forecastPanel.add(d);
        }

        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.add(forecastPanel, BorderLayout.CENTER);
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("Recent Searches"));
        historyList.setVisibleRowCount(5);
        historyPanel.add(new JScrollPane(historyList), BorderLayout.CENTER);
        bottom.add(historyPanel, BorderLayout.EAST);
        mainPanel.add(bottom, BorderLayout.SOUTH);

        applyTimeBackground();
    }

    private void searchWeather() {
        String location = locationField.getText().trim();
        if (location.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a city or location.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        searchButton.setEnabled(false);
        searchButton.setText("Loading...");
        new SwingWorker<WeatherData, Void>() {
            protected WeatherData doInBackground() throws Exception {
                return WeatherService.fetch(location);
            }
            protected void done() {
                try {
                    lastWeather = get();
                    updateUi(lastWeather);
                    addHistory(location);
                } catch (Exception ex) {ex.printStackTrace();
                    JOptionPane.showMessageDialog(WeatherApp.this,
                            "Could not retrieve weather information.\nPlease check the location and your internet connection.",
                            "Weather Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    searchButton.setEnabled(true);
                    searchButton.setText("Search");
                }
            }
        }.execute();
    }

    private void updateUi(WeatherData w) {
        locationLabel.setText(w.location + (w.country.isBlank() ? "" : ", " + w.country));
        conditionLabel.setText(w.description);
        temperatureLabel.setText(formatTemp(w.temperatureC));
        humidityLabel.setText(String.format("Humidity: %.0f%%", w.humidity));
        windLabel.setText(String.format("Wind: %s", formatWind(w.windKmh)));
        updatedLabel.setText("Updated: " + w.updated);
        weatherIcon.setCode(w.weatherCode, w.isDay);
        weatherIcon.repaint();

        for (int i = 0; i < forecastPanel.getComponentCount(); i++) {
            JPanel cell = new JPanel();
            cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
            cell.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            if (i < w.forecast.size()) {
                DayForecast d = w.forecast.get(i);
                JLabel date = new JLabel(d.date, SwingConstants.CENTER);
                JLabel desc = new JLabel(d.description, SwingConstants.CENTER);
                JLabel temps = new JLabel(formatTemp(d.maxC) + " / " + formatTemp(d.minC), SwingConstants.CENTER);
                date.setAlignmentX(Component.CENTER_ALIGNMENT);
                desc.setAlignmentX(Component.CENTER_ALIGNMENT);
                temps.setAlignmentX(Component.CENTER_ALIGNMENT);
                cell.add(Box.createVerticalStrut(8));
                cell.add(date); cell.add(desc); cell.add(temps);
                cell.add(Box.createVerticalGlue());
            }
            forecastPanel.remove(i);
            forecastPanel.add(cell, i);
        }
        forecastPanel.revalidate();
        forecastPanel.repaint();
        applyTimeBackground();
    }

    private String formatTemp(double c) {
        if ("Fahrenheit".equals(temperatureUnit.getSelectedItem())) {
            return String.format("%.1f °F", c * 9 / 5 + 32);
        }
        return String.format("%.1f °C", c);
    }

    private String formatWind(double kmh) {
        if ("mph".equals(windUnit.getSelectedItem())) {
            return String.format("%.1f mph", kmh * 0.621371);
        }
        return String.format("%.1f km/h", kmh);
    }

    private void refreshUnits() {
        if (lastWeather != null) {
            temperatureLabel.setText(formatTemp(lastWeather.temperatureC));
            windLabel.setText(String.format("Wind: %s", formatWind(lastWeather.windKmh)));
            for (int i = 0; i < forecastPanel.getComponentCount() && i < lastWeather.forecast.size(); i++) {
                JPanel cell = (JPanel) forecastPanel.getComponent(i);
                if (cell.getComponentCount() >= 3) {
                    ((JLabel) cell.getComponent(2)).setText(
                            formatTemp(lastWeather.forecast.get(i).maxC) + " / " +
                            formatTemp(lastWeather.forecast.get(i).minC));
                }
            }
        }
    }

    private void addHistory(String location) {
        String entry = location + " — " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        historyModel.removeElement(entry);
        historyModel.add(0, entry);
        while (historyModel.size() > 10) historyModel.removeElementAt(10);
    }

    private void applyTimeBackground() {
        int hour = LocalDateTime.now().getHour();
        if (hour >= 6 && hour < 12) mainPanel.setBackground(new Color(235, 245, 255));
        else if (hour >= 12 && hour < 18) mainPanel.setBackground(new Color(255, 248, 225));
        else if (hour >= 18 && hour < 22) mainPanel.setBackground(new Color(225, 230, 250));
        else mainPanel.setBackground(new Color(205, 215, 235));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WeatherApp().setVisible(true));
    }

    static class WeatherService {
        private static final HttpClient CLIENT = HttpClient.newHttpClient();

        static WeatherData fetch(String location) throws Exception {
            String encoded = URLEncoder.encode(location, StandardCharsets.UTF_8);
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                    encoded + "&count=1&language=en&format=json";
            JsonObject geo = getJson(geoUrl);
            JsonArray results = geo.getAsJsonArray("results");
            if (results == null || results.isEmpty()) throw new IOException("Location not found.");
            JsonObject place = results.get(0).getAsJsonObject();
            double lat = place.get("latitude").getAsDouble();
            double lon = place.get("longitude").getAsDouble();
            String name = place.get("name").getAsString();
            String country = place.has("country") ? place.get("country").getAsString() : "";

            String forecastUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s" +
                    "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code,is_day" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min,wind_speed_10m_max,precipitation_sum" +
                    "&forecast_days=5&timezone=auto", lat, lon);

            JsonObject data = getJson(forecastUrl);
            JsonObject current = data.getAsJsonObject("current");
            double temp = current.get("temperature_2m").getAsDouble();
            double humidity = current.get("relative_humidity_2m").getAsDouble();
            double wind = current.get("wind_speed_10m").getAsDouble();
            int code = current.get("weather_code").getAsInt();
            boolean day = current.get("is_day").getAsInt() == 1;
            String updated = current.get("time").getAsString();

            JsonObject daily = data.getAsJsonObject("daily");
            JsonArray dates = daily.getAsJsonArray("time");
            JsonArray codes = daily.getAsJsonArray("weather_code");
            JsonArray max = daily.getAsJsonArray("temperature_2m_max");
            JsonArray min = daily.getAsJsonArray("temperature_2m_min");
            List<DayForecast> days = new ArrayList<>();
            for (int i = 0; i < dates.size(); i++) {
                days.add(new DayForecast(dates.get(i).getAsString(),
                        codes.get(i).getAsInt(), max.get(i).getAsDouble(), min.get(i).getAsDouble()));
            }
            return new WeatherData(name, country, temp, humidity, wind, code, day, updated, days);
        }

        private static JsonObject getJson(String url) throws Exception {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IOException("HTTP " + response.statusCode());
            return JsonParser.parseString(response.body()).getAsJsonObject();
        }
    }

    static class WeatherData {
        String location, country, description, updated;
        double temperatureC, humidity, windKmh;
        int weatherCode;
        boolean isDay;
        List<DayForecast> forecast;
        WeatherData(String l, String c, double t, double h, double w, int code, boolean day, String u, List<DayForecast> f) {
            location=l; country=c; temperatureC=t; humidity=h; windKmh=w; weatherCode=code;
            description=description(code); isDay=day; updated=u; forecast=f;
        }
        static String description(int code) {
            if (code == 0) return "Clear sky";
            if (code <= 3) return "Partly cloudy";
            if (code <= 48) return "Fog";
            if (code <= 57) return "Drizzle";
            if (code <= 67) return "Rain";
            if (code <= 77) return "Snow";
            if (code <= 82) return "Rain showers";
            if (code <= 86) return "Snow showers";
            return "Thunderstorm";
        }
    }

    static class DayForecast {
        String date, description; double maxC, minC;
        DayForecast(String d, int code, double max, double min) {
            date=d; description=WeatherData.description(code); maxC=max; minC=min;
        }
    }

    static class WeatherIcon extends JPanel {
        int code = 0; boolean day = true;
        WeatherIcon() { setPreferredSize(new Dimension(180, 150)); setOpaque(false); }
        void setCode(int c, boolean d) { code=c; day=d; }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx=getWidth()/2, cy=getHeight()/2;
            g2.setStroke(new BasicStroke(4));
            if (code == 0) {
                g2.drawOval(cx-35, cy-35, 70, 70);
                for(int i=0;i<8;i++){ double a=i*Math.PI/4; g2.drawLine(cx+(int)(50*Math.cos(a)),cy+(int)(50*Math.sin(a)),
                        cx+(int)(70*Math.cos(a)),cy+(int)(70*Math.sin(a))); }
            } else {
                g2.drawOval(cx-55, cy-15, 80, 45);
                g2.drawOval(cx-25, cy-35, 65, 55);
                if (code >= 51) {
                    for(int i=0;i<4;i++) g2.drawLine(cx-45+i*25,cy+35,cx-55+i*25,cy+60);
                }
            }
            g2.dispose();
        }
    }
}
