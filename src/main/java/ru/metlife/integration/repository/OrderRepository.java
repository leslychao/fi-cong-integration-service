package ru.metlife.integration.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.metlife.integration.entity.OrderEntity;

@Repository
public interface OrderRepository extends CrudRepository<OrderEntity, String> {

  OrderEntity findByOrderId(String orderId);
}
