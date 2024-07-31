package org.example.mapper;

import org.example.model.Wind;

import java.util.List;

public interface DataMapper {
    void insertData(Wind wind);
    List<Wind> getAllDatas();
}
