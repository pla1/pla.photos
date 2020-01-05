package photos.pla;


import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "JsonServlet", urlPatterns = "/JsonServlet")
public class JsonServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (Utils.isBlank(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: action");
            return;
        }
        if ("photoAlbum".equals(action))
            photoAlbum(request, response);
        else
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format("Unknown action: \"%s\"", action));
    }

    private void photoAlbum(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String urlString = request.getParameter("urlString");
        if (Utils.isBlank(urlString)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: urlString");
            return;
        }
        write(response, Utils.transfer(urlString));
    }

    private void write(HttpServletResponse response, JsonObject jsonObject) throws IOException {
        PrintWriter pw = response.getWriter();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        pw.write(jsonObject.toString());
        pw.flush();
        pw.close();
    }
}
