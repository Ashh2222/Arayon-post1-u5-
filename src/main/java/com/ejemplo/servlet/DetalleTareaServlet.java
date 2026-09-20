package com.ejemplo.servlet;

import com.ejemplo.model.Tarea;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "DetalleTareaServlet", urlPatterns = {"/tareas/detalle"})
public class DetalleTareaServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int id;
        try {
            id = Integer.parseInt(req.getParameter("id"));
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/tareas?error=id-invalido");
            return;
        }

        @SuppressWarnings("unchecked")
        List<Tarea> tareas = (List<Tarea>) getServletContext().getAttribute("tareas");

        Tarea tarea = tareas.stream()
                .filter(t -> t.getId() == id)
                .findFirst()
                .orElse(null);

        if (tarea == null) {
            resp.sendRedirect(req.getContextPath() + "/tareas?error=no-encontrada");
            return;
        }

        // Decisión de diseño: se usa forward y NO sendRedirect. Si usara
        // sendRedirect, el objeto Tarea recién encontrado se perdería y el
        // navegador tendría que hacer una segunda petición sin el dato ya
        // calculado; además la URL cambiaría de forma innecesaria. El
        // patrón PRG (sendRedirect) sigue siendo correcto para las acciones
        // de POST en TareasServlet, porque ahí sí conviene una nueva
        // petición GET limpia que evite el reenvío del formulario al
        // recargar; aquí, en cambio, es una simple lectura (GET) que solo
        // necesita entregar un objeto ya en memoria a una vista.
        req.setAttribute("tarea", tarea);
        req.getRequestDispatcher("/WEB-INF/views/detalle.jsp")
                .forward(req, resp);
    }
}