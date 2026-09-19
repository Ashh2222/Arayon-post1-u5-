package com.ejemplo.servlet;

import com.ejemplo.model.Tarea;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet(name = "TareasServlet", urlPatterns = {"/tareas"})
public class TareasServlet extends HttpServlet {

    private final List<Tarea> tareas = new ArrayList<>();
    private int contadorId = 1;

    @Override
    public void init() throws ServletException {
        Date hoy = new Date();
        tareas.add(new Tarea(contadorId++, "Leer documentación de Servlets",
                "Estudio", "Alta", sumarDias(hoy, 2)));
        tareas.add(new Tarea(contadorId++, "Implementar ciclo GET/POST",
                "Estudio", "Alta", sumarDias(hoy, 3)));
        tareas.add(new Tarea(contadorId++, "Preparar sustentación del laboratorio",
                "Evaluación", "Media", sumarDias(hoy, 7)));
        tareas.add(new Tarea(contadorId++, "Revisar JSTL y Expression Language",
                "Estudio", "Baja", sumarDias(hoy, 10)));

        // Se expone la misma lista en el ServletContext (applicationScope)
        // para que DetalleTareaServlet la lea sin duplicar el estado
        getServletContext().setAttribute("tareas", tareas);
    }

    private Date sumarDias(Date base, int dias) {
        long unDiaMs = 24L * 60 * 60 * 1000;
        return new Date(base.getTime() + dias * unDiaMs);
    }

    /** GET /tareas — listar con filtro combinado (texto + categoría + prioridad) */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession();
        boolean hayFiltroEnUrl = req.getParameter("q") != null
                || req.getParameter("cat") != null
                || req.getParameter("prioridad") != null;

        String filtroTexto;
        String filtroCategoria;
        String filtroPrioridad;

        if (hayFiltroEnUrl) {
            // El usuario aplicó un filtro nuevo: se usa y se recuerda en sesión
            filtroTexto     = req.getParameter("q");
            filtroCategoria = req.getParameter("cat");
            filtroPrioridad = req.getParameter("prioridad");
            session.setAttribute("filtroTexto", filtroTexto);
            session.setAttribute("filtroCategoria", filtroCategoria);
            session.setAttribute("filtroPrioridad", filtroPrioridad);
        } else {
            // Sin parámetros en la URL (ej. clic directo en "Tareas"):
            // se restaura el último filtro guardado en la sesión, si existe
            filtroTexto     = (String) session.getAttribute("filtroTexto");
            filtroCategoria = (String) session.getAttribute("filtroCategoria");
            filtroPrioridad = (String) session.getAttribute("filtroPrioridad");
        }

        List<Tarea> resultado = tareas.stream()
                .filter(t -> filtroTexto == null || filtroTexto.isBlank()
                        || t.getTitulo().toLowerCase().contains(filtroTexto.toLowerCase()))
                .filter(t -> filtroCategoria == null || filtroCategoria.isBlank()
                        || t.getCategoria().equals(filtroCategoria))
                .filter(t -> filtroPrioridad == null || filtroPrioridad.isBlank()
                        || t.getPrioridad().equals(filtroPrioridad))
                .collect(Collectors.toList());

        List<String> categorias = tareas.stream()
                .map(Tarea::getCategoria).distinct().sorted()
                .collect(Collectors.toList());

        req.setAttribute("tareas", resultado);
        req.setAttribute("categorias", categorias);
        req.setAttribute("filtroTexto", filtroTexto);
        req.setAttribute("filtroCategoria", filtroCategoria);
        req.setAttribute("filtroPrioridad", filtroPrioridad);
        req.getRequestDispatcher("/WEB-INF/views/tareas.jsp")
                .forward(req, resp);
    }

    /** POST /tareas — agregar, eliminar, completar o identificar usuario */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String accion = req.getParameter("accion");

        if ("agregar".equals(accion)) {
            String titulo = req.getParameter("titulo");
            if (titulo == null || titulo.isBlank()) {
                req.setAttribute("error", "El título no puede estar vacío");
                req.setAttribute("tareas", tareas);
                req.getRequestDispatcher("/WEB-INF/views/tareas.jsp")
                        .forward(req, resp);
                return;
            }
            // Valores por defecto: el formulario rápido de la Parte 1 solo pide título
            tareas.add(new Tarea(contadorId++, titulo.trim(),
                    "General", "Media", sumarDias(new Date(), 5)));

        } else if ("eliminar".equals(accion)) {
            int id = Integer.parseInt(req.getParameter("id"));
            tareas.removeIf(t -> t.getId() == id);

        } else if ("completar".equals(accion)) {
            int id = Integer.parseInt(req.getParameter("id"));
            tareas.stream()
                    .filter(t -> t.getId() == id)
                    .findFirst()
                    .ifPresent(t -> t.setCompletada(true));

        } else if ("identificar".equals(accion)) {
            String nombre = req.getParameter("nombre");
            if (nombre != null && !nombre.isBlank()) {
                req.getSession().setAttribute("usuario", nombre.trim());
            }
        }

        // Decisión de diseño: el filtro activo y el nombre del usuario se
        // guardan en HttpSession, no en un atributo de request, porque
        // deben sobrevivir a MÁS de una petición (el usuario navega entre
        // /tareas y /tareas/detalle varias veces sin repetir su nombre ni
        // el filtro). Un atributo de request se pierde apenas termina esa
        // petición, por eso no sirve para este caso. En cambio, la lista
        // filtrada resultante (doGet, req.setAttribute("tareas", resultado))
        // sí es solo de request: es el resultado recién calculado para
        // ESTA respuesta, y no tiene sentido conservarlo aparte del filtro
        // que lo generó.

        // Patrón PRG: redirigir después de POST (todas las acciones)
        resp.sendRedirect(req.getContextPath() + "/tareas");
    }
}