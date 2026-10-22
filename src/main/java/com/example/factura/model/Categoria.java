package com.example.factura.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Categoria(
        Long id,
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,
        @Size(max = 200, message = "La descripción no puede superar 200 caracteres")
        String descripcion
) {
    public Categoria conId(Long nuevoId) {
        return new Categoria(nuevoId, nombre, descripcion);
    }
}
