package ar.utn.ba.dds.front_tp.dto.input;

import java.util.List;

public record PageInputDTO<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) { }