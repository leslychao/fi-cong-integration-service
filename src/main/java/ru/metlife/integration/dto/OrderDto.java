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
public class OrderDto implements Serializable {

  private static final long serialVersionUID = 547368841245519989L;

  private String ppNum;
  private String orderId;
  private String deliveryStatus;
  private String letterId;
  private String recipient;
  private String channel;
  private String sender;
  private String subject;
  private int status;
  private Date createdAt;
  private String createdBy;
  private String clientFio;
  private String polNum;
  private String comment;
  private String docType;
  private String emailCC;
  private byte[] entryHash;
  private String partner;
  private String dealership;
  private String region;


}
