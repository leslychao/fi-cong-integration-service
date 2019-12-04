package ru.metlife.integration.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class RecipientDto implements Serializable {

  private static final long serialVersionUID = 13151688118475791L;

  private final String email;
  private final String emailCC;
  private boolean isMain;
}
