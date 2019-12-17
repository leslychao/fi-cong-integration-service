package ru.metlife.integration.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.metlife.integration.entity.OrderEntity;

@Repository
public interface OrderRepository extends CrudRepository<OrderEntity, String> {

  OrderEntity findByOrderId(String orderId);

  @Query("select o from OrderEntity o where o.deliveryStatus is not null and o.deliveryStatus <> 'COMPLETED'")
  List<OrderEntity> findByDeliveryStatusIsNotNullAndNotCompleted();
}
