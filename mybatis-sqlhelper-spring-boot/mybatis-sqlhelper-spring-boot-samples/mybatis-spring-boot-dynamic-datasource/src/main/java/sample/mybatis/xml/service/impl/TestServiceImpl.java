package sample.mybatis.xml.service.impl;

import io.github.heykb.sqlhelper.dynamicdatasource.SqlHelperDsContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sample.mybatis.xml.domain.City;
import sample.mybatis.xml.mapper.CityMapper;
import sample.mybatis.xml.service.AnotherTestService;
import sample.mybatis.xml.service.TestService;

@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private CityMapper cityMapper;

    @Autowired
    private AnotherTestService anotherTestService;

    @Override
    @Transactional
    public City findByState(String state) {
        City city = cityMapper.findByState(state);
        SqlHelperDsContextHolder.switchTo("mysql");
        city = anotherTestService.findByState(state);
        SqlHelperDsContextHolder.clear();
        return city;
    }
}
