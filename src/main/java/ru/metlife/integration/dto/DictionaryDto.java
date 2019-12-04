package ru.metlife.integration.dto;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class DictionaryDto implements Serializable {

  private static final long serialVersionUID = -4083173478864137713L;

  private String number;
  private String partner;
  private String dealership;
  private String region;
  private String email;
  private String emailCC;

}
