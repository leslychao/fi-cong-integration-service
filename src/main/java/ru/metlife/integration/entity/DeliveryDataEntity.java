package ru.metlife.integration.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "delivery_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DeliveryDataEntity implements Serializable {

  @Id
  @Column(name = "id")
  String id;
  @Column(name = "order_id")
  String orderId;
  @Column(name = "delivery_status")
  String deliveryStatus;
  @Column(name = "created_at")
  Date createdAt;
  @Column(name = "pp_num")
  String ppNum;
}
