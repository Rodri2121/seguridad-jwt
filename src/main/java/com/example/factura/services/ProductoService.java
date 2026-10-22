package com.example.factura.services;

import com.example.factura.exception.RecursoNoEncontradoException;
import com.example.factura.model.Producto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/** Almacén en memoria: los datos se reinician al detener la aplicación. */
@Service
public class ProductoService {
    private final Map<Long, Producto> datos = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong();
    private final CategoriaService categorias;

    public ProductoService(CategoriaService categorias) {
        this.categorias = categorias;
        crear(new Producto(null, "Arroz 1 lb", new BigDecimal("18.50"), 120, 1L));
        crear(new Producto(null, "Frijoles rojos 1 lb", new BigDecimal("22.00"), 90, 1L));
        crear(new Producto(null, "Agua purificada 600 ml", new BigDecimal("15.00"), 200, 2L));
        crear(new Producto(null, "Detergente 500 g", new BigDecimal("45.75"), 60, 3L));
    }

    public List<Producto> listar() {
        return datos.values().stream()
                .sorted((a, b) -> a.id().compareTo(b.id()))
                .toList();
    }

    public Producto obtener(Long id) {
        Producto producto = datos.get(id);
        if (producto == null) {
            throw new RecursoNoEncontradoException("Producto", id);
        }
        return producto;
    }

    public Producto crear(Producto producto) {
        verificarCategoria(producto.categoriaId());
        Producto nuevo = producto.conId(secuencia.incrementAndGet());
        datos.put(nuevo.id(), nuevo);
        return nuevo;
    }

    public Producto actualizar(Long id, Producto producto) {
        obtener(id);
        verificarCategoria(producto.categoriaId());
        Producto actualizado = producto.conId(id);
        datos.put(id, actualizado);
        return actualizado;
    }

    public void eliminar(Long id) {
        if (datos.remove(id) == null) {
            throw new RecursoNoEncontradoException("Producto", id);
        }
    }

    private void verificarCategoria(Long categoriaId) {
        if (!categorias.existe(categoriaId)) {
            throw new RecursoNoEncontradoException("Categoría", categoriaId);
        }
    }
}
