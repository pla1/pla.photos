package photos.pla;


import com.google.gson.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "JsonServlet", urlPatterns = "/JsonServlet")

public class JsonServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (Utils.isBlank(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing GET parameter: action");
            return;
        }
        if (Utils.Labels.albums.name().equals(action)) photoAlbums(request, response);
        else if (Utils.Labels.embedUrl.name().equals(action)) googlePhotoEmbedUrl(request, response);
        else if (Utils.Labels.photos.name().equals(action)) photos(request, response);
        else
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format("Unknown action in GET: \"%s\"", action));
    }

    private void photoAlbums(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        File file = new File(Utils.FILE_NAME_ALBUMS);
        if (!file.exists()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format("File missing: %s", file.getAbsolutePath()));
            return;
        }
        JsonObject jsonObject = Utils.read(file);
        write(response, jsonObject);
    }

    private void photos(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        File file = new File(Utils.FILE_NAME_ALBUMS);
        if (!file.exists()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format("File missing: %s", file.getAbsolutePath()));
            return;
        }
        String dateString = request.getParameter("q");
        if (Utils.isBlank(dateString)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter missing: q");
            return;
        }
        JsonObject jsonObjectAlbums = Utils.read(file);
        String path = String.format("%s%s.json", Utils.DIRECTORY, dateString);
        System.out.format("%s\n", path);
        File filePhotos = new File(path);
        JsonObject jsonObjectPhotos = null;
        if (filePhotos.exists()) {
            jsonObjectPhotos = Utils.read(filePhotos);
            write(response, jsonObjectPhotos);
            return;
        }
        String albumUrl = null;
        JsonArray jsonArrayAlbums = jsonObjectAlbums.getAsJsonArray(Utils.Labels.albums.name());
        System.out.format("%d albums.\n", jsonArrayAlbums.size());
        for (JsonElement element : jsonArrayAlbums) {
            JsonObject jo = element.getAsJsonObject();
            System.out.format("%s\n", jo.get(Utils.Labels.dateDisplay.name()).getAsString());
            if (jo.get(Utils.Labels.dateDisplay.name()).getAsString().equals(dateString)) {
                albumUrl = jo.get(Utils.Labels.url.name()).getAsString();
            }
        }
        if (Utils.isBlank(albumUrl)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format("Photo album not found with date: %s", dateString));
            return;
        }
        jsonObjectPhotos = Utils.transfer(albumUrl);
        Utils.write(filePhotos, jsonObjectPhotos);
        write(response, jsonObjectPhotos);
    }

    private void googlePhotoEmbedUrl(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String urlString = request.getParameter("urlString");
        if (Utils.isBlank(urlString)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: urlString");
            return;
        }
        String embedUrl = Utils.getEmbedUrl(urlString);
        String embedTitle = Utils.getEmbedTitle(urlString);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(Utils.Labels.urlString.name(), urlString);
        jsonObject.addProperty(Utils.Labels.embedUrl.name(), embedUrl);
        jsonObject.addProperty(Utils.Labels.embedTitle.name(), embedTitle);
        write(response, jsonObject);
    }

    private void write(HttpServletResponse response, JsonObject jsonObject) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter pw = response.getWriter();
        pw.write(new Gson().toJson(jsonObject));
        pw.flush();
        pw.close();
    }
}
