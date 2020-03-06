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
@Table(name = "data_fi_time_freeze")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DataFiTimeFreezeEntity implements Serializable {

  private static final long serialVersionUID = -7425933237381145088L;

  @Id
  @Column(name = "id")
  private String id;
  @Column(name = "polnum")
  private String polNum;
  @Column(name = "doc_type")
  private String docType;
  @Column(name = "client_fio")
  private String clientFio;
  @Column(name = "comment")
  private String comment;
  @Column(name = "e_mail")
  private String email;
  @Column(name = "e_mail_copy")
  private String emailCopy;
  @Column(name = "entry_hash")
  private byte[] entryHash;
  @Column(name = "created_at")
  private Date createdAt;
  @Column(name = "order_id")
  private String orderId;
}
