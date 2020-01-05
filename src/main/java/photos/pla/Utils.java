package photos.pla;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private final static Pattern PATTERN_ALBUM_PHOTO_URLS = Pattern.compile("\\[\"(https:\\/\\/lh3\\.googleusercontent\\.com\\/[a-z0-9-\\/_]+)\",[\\d]+,[\\d]+", Pattern.CASE_INSENSITIVE);
    public static void main(String[] args) throws Exception {
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
            return urlString.substring(0,pos);
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
        for (Element element:elements) {
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

        JsonArray jsonArray =new JsonArray();
        jsonObject.add("photoUrls", jsonArray);
        while (matcher.find()) {
            System.out.format("%s\n", matcher.group(1));
            jsonArray.add(matcher.group(1));
        }
        System.out.format("%s\n",jsonObject.toString());
        System.out.format("Finished in %d milliseconds.\n", (System.currentTimeMillis() - start));
        return jsonObject;
    }
}
