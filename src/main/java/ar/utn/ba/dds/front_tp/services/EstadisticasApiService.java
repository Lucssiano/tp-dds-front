package ar.utn.ba.dds.front_tp.services;

import ar.utn.ba.dds.front_tp.dto.admin.CategoriaEstadisticaDTO;
import ar.utn.ba.dds.front_tp.dto.admin.ColeccionEstadisticaDTO;
import ar.utn.ba.dds.front_tp.dto.input.CategoriaInputDTO;
import ar.utn.ba.dds.front_tp.services.internal.WebApiCallerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<ColeccionEstadisticaDTO> obtenerColecciones(List<String> colecciones) {

        String url = estadisticasServiceUrl + "/colecciones";

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);

        if (colecciones != null)
            colecciones.forEach(c -> builder.queryParam("colecciones", c));


        String finalUrl = builder.toUriString(); // ← ESTA es la URL con los query params

        return webClient.get()
                .uri(finalUrl)
                .retrieve()
                .bodyToFlux(ColeccionEstadisticaDTO.class)
                .collectList()
                .block();
    }

}