package ru.metlife.integration.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.metlife.integration.entity.DeliveryDataEntity;

@Repository
public interface DeliveryDataRepository extends CrudRepository<DeliveryDataEntity, String> {

  DeliveryDataEntity findByOrderId(String orderId);

  boolean existsDeliveryDataByPpNum(String ppNum);

  @Modifying
  @Query("UPDATE DeliveryDataEntity d SET d.deliveryStatus = :deliveryStatus WHERE c.orderId = :orderId")
  String updateStatus(@Param("deliveryStatus") String deliveryStatus, @Param("orderId") String orderId);
}
