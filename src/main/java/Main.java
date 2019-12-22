import com.google.gson.Gson;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static Document document;
    private static HashMap<String, ArrayList<String>> stationsMap = new HashMap<>();
    private static ArrayList<ArrayList<Station>> connectionsList = new ArrayList<>();
    private static TreeSet<Line> lineSet = new TreeSet<>((l1, l2) ->
            Integer.compare(l1.getNumber().compareTo(l2.getNumber()), 0));

    private static String path = "src\\file.json";

    public static void main(String[] args) {

        createDocument();
        parseDocument();

        JSONObject metro = new JSONObject();
        metro.put("stations", stationsMap);
        metro.put("lines", lineSet);
        metro.put("connections", connectionsList);

        writeJSONFile(metro, path);

        parseJsonFile();
    }

    private static void createDocument() {
        try {
            document = Jsoup
                    .connect("https://ru.wikipedia.org/wiki/Список_станций_Московского_метрополитена")
                    .maxBodySize(0)
                    .timeout(0)
                    .get();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void parseDocument() {
        Elements rows = document.select("div#mw-content-text div.mw-parser-output " +
                "table.standard tbody tr");
        for (Element row : rows) {
            Elements cols = row.select("td");
            if (cols.size() != 0) {
                String lineName = cols.get(0).select("a").first().attr("title");
                String stationNumber = cols.get(0).select("span").first().text();
                String stationName = cols.get(1).select("a").first().text();

                fillStationMap(stationNumber, stationName);

                lineSet.add(new Line(stationNumber, lineName));

                ArrayList<Station> connections = parseConnections(cols.get(3));
                if (connections != null) {
                    connections.add(new Station(stationNumber, stationName));
                    connectionsList.add(connections);
                }
            }
        }
    }

    private static void fillStationMap(String number, String name) {
        stationsMap.putIfAbsent(number, new ArrayList<>());
        stationsMap.computeIfPresent(number, (k, v) -> {
            v.add(name);
            return v;
        });
    }

    private static ArrayList<Station> parseConnections(Element column) {
        if (column.attr("data-sort-value").matches("^\\d+[.]?\\d*$")) {
            Elements numCon = column.select("span");
            ArrayList<Station> connectionsList = new ArrayList<>();
            for (int i = 0; i < numCon.size(); i += 2) {
                String num = numCon.get(i).text();
                String text = numCon.get(i + 1).attr("title");
                connectionsList.add(new Station(num, nameFromSubstring(text)));
            }
            return connectionsList;
        }
        return null;
    }

    private static String nameFromSubstring(String text) {
        String connectionName = "";
        Pattern pattern = Pattern.compile("\\s[А-Я][А-я]+[\\s?[А-яё]\\-?[А-я]]*\\s[А-Я]");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            connectionName = text.substring(matcher.start() + 1, matcher.end() - 1);
        }
        return connectionName.trim();
    }

    private static void writeJSONFile(JSONObject object, String path) {
        try {
            FileWriter file = new FileWriter(path);
            file.write(object.toString());
            file.flush();
            file.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void parseJsonFile() {
        try {
            Map generalObject = new Gson().fromJson(new FileReader(path), Map.class);
            Map object = (Map) generalObject.get("stations");
            Set set = object.keySet();
            for (Object s : set) {
                String line = s.toString();
                ArrayList array = (ArrayList) object.get(line);
                System.out.println("Номер линиии: " + line + "\tКоличество станций: " + array.size());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
