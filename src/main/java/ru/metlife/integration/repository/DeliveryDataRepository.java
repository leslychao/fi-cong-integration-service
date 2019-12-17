package ru.metlife.integration.repository;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.metlife.integration.entity.DeliveryDataEntity;

@Repository
public interface DeliveryDataRepository extends CrudRepository<DeliveryDataEntity, String> {

  List<DeliveryDataEntity> findByOrderId(List<String> orderId);

  DeliveryDataEntity findByPpNum(String ppNum);

  boolean existsDeliveryDataByOrderIdAndPpNum(String orderId, String ppNum);
}
