package photos.pla;

import com.google.gson.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private final static Pattern PATTERN_ALBUM_PHOTO_URLS = Pattern.compile("\\[\"(https:\\/\\/lh3\\.googleusercontent\\.com\\/[a-z0-9-\\/_]+)\",[\\d]+,[\\d]+", Pattern.CASE_INSENSITIVE);
    public final static String FILE_NAME_ALBUMS = "/opt/pla.photos/albums.json";
    public final static String DIRECTORY = "/opt/pla.photos/";

    public enum Labels {content, album, albums, addAlbum, deleteAlbum, updateAlbum,photos, date, dateMilliseconds, dateDisplay, cover, url, embedUrl, embedTitle, urlString}

    public static void main(String[] args) throws Exception {
        String photoAlbumUrl = "https://photos.app.goo.gl/XA1ZgVf61isQYj3B9";
        if (true) {
            File file = new File(Utils.FILE_NAME_ALBUMS);
            Utils.print(file);
            JsonObject jsonObject = Utils.read(file);
            JsonArray jsonArray = jsonObject.getAsJsonArray(Labels.albums.name());
            for (JsonElement element : jsonArray) {
                System.out.format("%s %s\n", element.getAsJsonObject().get("title"), element.toString());
            }
            System.exit(0);
        }
        if (false) {
            File file = new File(Utils.FILE_NAME_ALBUMS);
            JsonObject jsonObject = Utils.read(file);
            Utils.write(file, jsonObject);
            jsonObject = Utils.read(file);
            JsonArray jsonArray = jsonObject.getAsJsonArray(Labels.albums.name());
            for (JsonElement element : jsonArray) {
                System.out.format("%s %s\n", element.getAsJsonObject().get("title"), element.toString());
            }
            System.exit(0);
        }
        if (false) {
            Utils.getEmbedUrl(photoAlbumUrl);
            System.exit(0);
        }
        if (false) {
            JsonObject jsonObject = Utils.transfer(photoAlbumUrl);
            File file = File.createTempFile("jsonTest", ".json");
            System.out.format("Temporary file: %s\n", file.getAbsolutePath());
            Utils.write(file, jsonObject);
            jsonObject = Utils.read(file);
            Utils.prettyPrint(jsonObject);
            System.exit(0);
        }
        if (false) {
            File file = new File(Utils.FILE_NAME_ALBUMS);
            boolean newFileCreated = file.createNewFile();
            JsonObject jsonObject = Utils.read(file);
            Utils.write(file, jsonObject);
            System.out.format("Content: %s Has albums: %s New file created: %s\n", jsonObject, jsonObject.has("albums"), newFileCreated);
            System.exit(0);
        }
    }

    public static String getEmbedUrl(String urlString) throws IOException {
        Document doc = Jsoup.connect(urlString).userAgent("curl/7.58.0").followRedirects(true).get();
        String query = "meta[property='og:image']";
        Elements elements = doc.select(query);
        //    Elements elements = doc.getElementsByTag("meta");
        System.out.format("%d elements found with query: \"%s\" via URL %s\n", elements.size(), query, urlString);
        for (Element element : elements) {
            System.out.format("Element %s Element attribute: %s\n", element, element.attr(Labels.content.name()));
            return Utils.trimDimensionParameter(element.attr(Labels.content.name()));
        }
        if (elements.size() == 0) {
            elements = doc.getAllElements();
            for (Element element : elements) {
                if (element.toString().contains("og:")) {
                    System.out.format("%s\n\n", element.toString());
                }
            }
        }
        return null;
    }


    public static String getEmbedTitle(String urlString) throws IOException {
        Document doc = Jsoup.connect(urlString).userAgent("curl/7.58.0").followRedirects(true).get();
        String query = "meta[property='og:title']";
        Elements elements = doc.select(query);
        //    Elements elements = doc.getElementsByTag("meta");
        System.out.format("%d elements found with query: \"%s\" via URL %s\n", elements.size(), query, urlString);
        for (Element element : elements) {
            System.out.format("Element %s Element attribute: %s\n", element, element.attr(Labels.content.name()));
            return element.attr(Labels.content.name());
        }
        if (elements.size() == 0) {
            elements = doc.getAllElements();
            for (Element element : elements) {
                if (element.toString().contains("og:")) {
                    System.out.format("%s\n\n", element.toString());
                }
            }
        }
        return null;
    }


    public static boolean isBlank(String s) {
        return (s == null || s.trim().length() == 0);
    }

    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }


    private static String trimDimensionParameter(String urlString) {
        if (Utils.isBlank(urlString)) {
            return urlString;
        }
        int pos = urlString.indexOf("=");
        if (pos > 0 && pos < urlString.length()) {
            return urlString.substring(0, pos);
        }
        return urlString;
    }

    public static JsonObject transfer(String photoAlbumUrl) throws IOException {
        long start = System.currentTimeMillis();
        Document doc = Jsoup.connect(photoAlbumUrl).userAgent("curl/7.58.0").followRedirects(true).get();
        Elements elements = doc.getElementsByTag("meta");
        System.out.format("%d elements found.\n", elements.size());
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("photoAlbumUrl", photoAlbumUrl);
        for (Element element : elements) {
            if (element.toString().contains("og:")) {
                System.out.format("%s %s\n\n", element.attr("property"), element.attr("content"));
                if ("og:image".equals(element.attr("property"))) {
                    jsonObject.addProperty("albumCoverUrl", trimDimensionParameter(element.attr("content")));
                }
                if ("og:title".equals(element.attr("property"))) {
                    jsonObject.addProperty("title", element.attr("content"));
                }
            }
        }
        String output = doc.toString();
        Matcher matcher = PATTERN_ALBUM_PHOTO_URLS.matcher(output);
        JsonArray jsonArray = new JsonArray();
        jsonObject.add("photoUrls", jsonArray);
        ArrayList<String> arrayList = new ArrayList<String>();
        while (matcher.find()) {
            String urlString = matcher.group(1);
            System.out.format("%s\n", urlString);
            if (!arrayList.contains(urlString)) {
                jsonArray.add(urlString);
            }
        }
        System.out.format("%s\n", jsonObject.toString());
        System.out.format("Finished in %d milliseconds.\n", (System.currentTimeMillis() - start));
        return jsonObject;
    }

    public static void write(File file, JsonObject jsonObject) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")));
        writer.write(jsonObject.toString());
        writer.flush();
        writer.close();
        System.out.format("File %s written to disk.\n", file.getAbsolutePath());
    }

    public static JsonObject read(File file) {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();
        Object json = gson.fromJson(bufferedReader, Object.class);
        JsonElement jsonElement = gson.toJsonTree(json);
        if (!jsonElement.isJsonObject()) {
            System.out.format("Not a Json Object in file: %s. Returning empty JsonObject.\n", file.getAbsolutePath());
            return new JsonObject();
        }
        return gson.toJsonTree(json).getAsJsonObject();
    }

    public static void print(File file) throws IOException {
        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                out.println(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            bufferedReader.close();
        }
    }

    public static void prettyPrint(JsonElement jsonElement) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.format("%s\n", gson.toJson(jsonElement));
    }

    public static long getLong(JsonElement jsonElement, String property) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject.has(property)) {
            return jsonObject.get(property).getAsLong();
        }
        return 0;
    }
}
