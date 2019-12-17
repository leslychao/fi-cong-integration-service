package ru.metlife.integration.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.metlife.integration.entity.DeliveryDataEntity;

@Repository
public interface DeliveryDataRepository extends CrudRepository<DeliveryDataEntity, String> {

  @Query("select d from DeliveryDataEntity d where d.deliveryStatus is null or d.deliveryStatus <> 'COMPLETED'")
  List<DeliveryDataEntity> findByDeliveryStatus();

  boolean existsDeliveryDataByPpNum(String ppNum);

  @Modifying
  @Query("update DeliveryDataEntity d SET d.deliveryStatus = :deliveryStatus WHERE d.orderId = :orderId")
  int updateStatus(@Param("deliveryStatus") String deliveryStatus,
      @Param("orderId") String orderId);
}
