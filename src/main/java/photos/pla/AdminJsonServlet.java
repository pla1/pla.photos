package photos.pla;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "AdminJsonServlet", urlPatterns = "/AdminJsonServlet")

public class AdminJsonServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (Utils.isBlank(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing POST parameter: action");
            return;
        }
        if (Utils.Labels.addAlbum.name().equals(action)) addAlbum(request, response);
        else if (Utils.Labels.deleteAlbum.name().equals(action)) deleteAlbum(request, response);
        else if (Utils.Labels.updateAlbum.name().equals(action)) updateAlbum(request, response);
        else
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format("Unknown action in POST: \"%s\"", action));
    }

    private void addAlbum(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        File file = new File(Utils.FILE_NAME_ALBUMS);
        boolean createdAsNew = file.createNewFile();
        System.out.format("File %s. Created as new: %s\n", file.getAbsolutePath(), createdAsNew);
        JsonElement jsonElementNew = JsonParser.parseReader(request.getReader());
        JsonObject jsonObject = Utils.read(file);
        if (!jsonObject.has(Utils.Labels.albums.name())) {
            jsonObject = new JsonObject();
            jsonObject.add(Utils.Labels.albums.name(), new JsonArray());
            System.out.format("Albums missing from file: %s. Added empty array.\n", file.getAbsolutePath());
        }
        JsonArray jsonArray = jsonObject.getAsJsonArray(Utils.Labels.albums.name());
        for (JsonElement element : jsonArray) {
            long dateMilliseconds = Utils.getLong(element, Utils.Labels.dateMilliseconds.name());
            if (dateMilliseconds == Utils.getLong(jsonElementNew, Utils.Labels.dateMilliseconds.name())) {
                String message = String.format("Date already exists. %s %d",
                        jsonElementNew.getAsJsonObject().get(Utils.Labels.dateDisplay.name()),
                        Utils.getLong(jsonElementNew, Utils.Labels.dateMilliseconds.name()));
                System.out.println(message);
                System.out.format("%s\n%s\n", jsonElementNew, element);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
                return;
            }
        }
        jsonArray.add(jsonElementNew.getAsJsonObject());
        Utils.write(file, jsonObject);
        write(response, jsonObject);
    }

    private void deleteAlbum(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        File file = new File(Utils.FILE_NAME_ALBUMS);
        boolean createdAsNew = file.createNewFile();
        System.out.format("File %s. Created as new: %s\n", file.getAbsolutePath(), createdAsNew);
        JsonElement jsonElementNew = JsonParser.parseReader(request.getReader());
        JsonObject jsonObject = Utils.read(file);
        JsonArray jsonArray = jsonObject.getAsJsonArray(Utils.Labels.albums.name());
        int indexRemove = 0;
        int i = 0;
        for (JsonElement element : jsonArray) {
            long dateMilliseconds = Utils.getLong(element, Utils.Labels.dateMilliseconds.name());
            if (dateMilliseconds == Utils.getLong(jsonElementNew, Utils.Labels.dateMilliseconds.name())) {
                indexRemove = i;
            }
            i++;
        }
        jsonArray.remove(indexRemove);
        Utils.write(file, jsonObject);
        write(response, jsonObject);
    }

    private void updateAlbum(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        File file = new File(Utils.FILE_NAME_ALBUMS);
        boolean createdAsNew = file.createNewFile();
        System.out.format("File %s. Created as new: %s\n", file.getAbsolutePath(), createdAsNew);
        JsonElement jsonElementNew = JsonParser.parseReader(request.getReader());
        JsonObject jsonObject = Utils.read(file);
        JsonArray jsonArray = jsonObject.getAsJsonArray(Utils.Labels.albums.name());
        for (JsonElement element : jsonArray) {
            long dateMilliseconds = Utils.getLong(element, Utils.Labels.dateMilliseconds.name());
            if (dateMilliseconds == Utils.getLong(jsonElementNew, Utils.Labels.dateMilliseconds.name())) {
                jsonArray.remove(element);
                jsonArray.add(jsonElementNew.getAsJsonObject());
            }
        }
        Utils.write(file, jsonObject);
        write(response, jsonObject);
    }

    private void write(HttpServletResponse response, JsonObject jsonObject) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter pw = response.getWriter();
        pw.write(jsonObject.toString());
        pw.flush();
        pw.close();
    }
}
