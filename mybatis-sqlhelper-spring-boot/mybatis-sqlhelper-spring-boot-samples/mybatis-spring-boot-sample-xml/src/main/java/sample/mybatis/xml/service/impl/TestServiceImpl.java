package sample.mybatis.xml.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sample.mybatis.xml.domain.City;
import sample.mybatis.xml.mapper.CityMapper;
import sample.mybatis.xml.service.TestService;

@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private CityMapper cityMapper;

    @Override
    @Transactional
    public City findByState(String state) {
        return cityMapper.findByState(state);
    }
}
