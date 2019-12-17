package ru.metlife.integration.dto;

import java.io.Serializable;
import java.util.Date;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class DeliveryDataDto implements Serializable {

  String id;
  String orderId;
  String deliveryStatus;
  Date createdAt;
  String ppNum;
}
