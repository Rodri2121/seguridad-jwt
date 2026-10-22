package com.example.factura.services;

import com.example.factura.exception.RecursoNoEncontradoException;
import com.example.factura.model.Categoria;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/** Almacén en memoria: los datos se reinician al detener la aplicación. */
@Service
public class CategoriaService {
    private final Map<Long, Categoria> datos = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong();

    public CategoriaService() {
        crear(new Categoria(null, "Abarrotes", "Productos de consumo básico"));
        crear(new Categoria(null, "Bebidas", "Refrescos, jugos y agua"));
        crear(new Categoria(null, "Limpieza", "Artículos de limpieza del hogar"));
    }

    public List<Categoria> listar() {
        return datos.values().stream()
                .sorted((a, b) -> a.id().compareTo(b.id()))
                .toList();
    }

    public Categoria obtener(Long id) {
        Categoria categoria = datos.get(id);
        if (categoria == null) {
            throw new RecursoNoEncontradoException("Categoría", id);
        }
        return categoria;
    }

    public boolean existe(Long id) {
        return datos.containsKey(id);
    }

    public Categoria crear(Categoria categoria) {
        Categoria nueva = categoria.conId(secuencia.incrementAndGet());
        datos.put(nueva.id(), nueva);
        return nueva;
    }

    public Categoria actualizar(Long id, Categoria categoria) {
        obtener(id);
        Categoria actualizada = categoria.conId(id);
        datos.put(id, actualizada);
        return actualizada;
    }

    public void eliminar(Long id) {
        if (datos.remove(id) == null) {
            throw new RecursoNoEncontradoException("Categoría", id);
        }
    }
}
