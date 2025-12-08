package ar.utn.ba.dds.front_tp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Mapeamos la URL lógica a la Ruta Física

    // "file:./uploads/" significa: Buscá en la carpeta uploads que está
    // en la raíz de donde se ejecuta el proyecto.
    registry.addResourceHandler("/hechos/uploads/**")
        .addResourceLocations("file:./uploads/");
  }
}
