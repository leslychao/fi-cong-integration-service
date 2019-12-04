package ru.metlife.integration.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.metlife.integration.entity.DataFiTimeFreezeEntity;

@Repository
public interface DataFiTimeFreezeRepository extends CrudRepository<DataFiTimeFreezeEntity, String> {

}
