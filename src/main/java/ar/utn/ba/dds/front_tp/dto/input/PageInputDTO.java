package ar.utn.ba.dds.front_tp.dto.input;

import lombok.Data;

import java.util.List;

@Data
public class PageInputDTO<T> {
  private List<T> content;
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;
}
