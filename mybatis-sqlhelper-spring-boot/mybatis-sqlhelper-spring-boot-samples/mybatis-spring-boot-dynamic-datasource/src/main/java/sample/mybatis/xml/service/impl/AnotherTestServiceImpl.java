package sample.mybatis.xml.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sample.mybatis.xml.domain.City;
import sample.mybatis.xml.mapper.CityMapper;
import sample.mybatis.xml.service.AnotherTestService;

@Service
public class AnotherTestServiceImpl implements AnotherTestService {

    @Autowired
    private CityMapper cityMapper;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public City findByState(String state) {
        City city = cityMapper.findByState2(state);
        return city;
    }
}
