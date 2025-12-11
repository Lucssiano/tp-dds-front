package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.Utils.PageResponse;
import ar.utn.ba.dds.front_tp.dto.admin.CategoriaEstadisticaDTO;
import ar.utn.ba.dds.front_tp.dto.admin.ColeccionEstadisticaDTO;
import ar.utn.ba.dds.front_tp.dto.input.CategoriaInputDTO;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EstadisticasApiService {
    private final WebClient webClient;
    private final WebApiCallerService webApiCallerService;
    private final String estadisticasServiceUrl = "http://localhost:8084/metamapa/estadisticas";

    @Autowired
    public EstadisticasApiService(WebApiCallerService webApiCallerService) {
        this.webClient =  WebClient.builder().baseUrl(estadisticasServiceUrl).build();
        this.webApiCallerService = webApiCallerService;
    }

    public Page<CategoriaEstadisticaDTO> obtenerCategoriasPaginadas(
            List<String> categorias,
            Boolean top,
            int page,        // Recibe page
            int size) {      // Recibe size

        String url = estadisticasServiceUrl + "/categoria";

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);

        if (categorias != null)
            categorias.forEach(c -> builder.queryParam("categorias", c));

        if (top != null)
            builder.queryParam("top", top);

        // 1. Agregar los parámetros de paginación a la URL
        builder.queryParam("page", page);
        builder.queryParam("size", size);

        String finalUrl = builder.toUriString();

        // 2. Usar ParameterizedTypeReference con tu clase PageResponse
        ParameterizedTypeReference<PageResponse<CategoriaEstadisticaDTO>> responseType =
                new ParameterizedTypeReference<PageResponse<CategoriaEstadisticaDTO>>() {};

        return webClient.get()
                .uri(finalUrl)
                .retrieve()
                // 3. Deserializar la respuesta JSON del Backend a un Page<DTO>
                .bodyToMono(responseType)
                .block();
    }
    public List<CategoriaEstadisticaDTO> obtenerCategorias(List<String> categorias, Boolean top) {

        String url = estadisticasServiceUrl + "/categoria";

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);

        if (categorias != null)
            categorias.forEach(c -> builder.queryParam("categorias", c));

        if (top != null)
            builder.queryParam("top", top);

        String finalUrl = builder.toUriString(); // ← ESTA es la URL con los query params

        return webClient.get()
                .uri(finalUrl)
                .retrieve()
                .bodyToFlux(CategoriaEstadisticaDTO.class)
                .collectList()
                .block();
    }
    // NUEVO MÉTODO PARA COLECCIONES PAGINADAS
    public Page<ColeccionEstadisticaDTO> obtenerColeccionesPaginadas(
            List<String> colecciones,
            int page,
            int size) {

        String url = estadisticasServiceUrl + "/colecciones"; // Asumo que el endpoint del backend es /colecciones

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);

        if (colecciones != null)
            colecciones.forEach(c -> builder.queryParam("colecciones", c));

        // 1. Agregar los parámetros de paginación a la URL
        builder.queryParam("page", page);
        builder.queryParam("size", size);

        String finalUrl = builder.toUriString();

        // 2. Usar ParameterizedTypeReference con tu clase PageResponse
        ParameterizedTypeReference<PageResponse<ColeccionEstadisticaDTO>> responseType =
                new ParameterizedTypeReference<PageResponse<ColeccionEstadisticaDTO>>() {};

        return webClient.get()
                .uri(finalUrl)
                .retrieve()
                // 3. Deserializar la respuesta JSON del Backend a un Page<DTO>
                .bodyToMono(responseType)
                .block();
    }
}

