package ar.utn.ba.dds.front_tp.controller;

import ar.utn.ba.dds.front_tp.services.GestionUsuariosApiService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class RegisterController {

  private static final Logger log = LoggerFactory.getLogger(RegisterController.class);
  private final GestionUsuariosApiService gestionUsuariosApiService;

  //TODO
}
