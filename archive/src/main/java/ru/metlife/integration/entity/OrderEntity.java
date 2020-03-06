package ru.metlife.integration.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OrderEntity implements Serializable {

  private static final long serialVersionUID = -4458449717040974951L;

  @Id
  @Column(name = "order_id")
  String orderId;
  @Column(name = "recipient")
  String recipient;
  @Column(name = "channel")
  String channel;
  @Column(name = "sender")
  String sender;
  @Column(name = "subject")
  String subject;
  @Column(name = "body_url")
  String bodyUrl;
  @Column(name = "body_content")
  @Lob
  byte[] bodyContent;
  @Column(name = "dsg_doc_id")
  String dsgDocId;
  @Column(name = "cong_order_id")
  String congOrderId;
  @Column(name = "status")
  int status;
  @Column(name = "status_details")
  String statusDetails;
  @Column(name = "delivery_status")
  String deliveryStatus;
  @Column(name = "delivery_status_details")
  String deliveryStatusDetails;
  @Column(name = "provider_code")
  String providerCode;
  @Column(name = "copy_of_order_id")
  String copyOfOrderId;
  @Column(name = "created_at")
  Date createdAt;
  @Column(name = "created_by")
  String createdBy;
}
