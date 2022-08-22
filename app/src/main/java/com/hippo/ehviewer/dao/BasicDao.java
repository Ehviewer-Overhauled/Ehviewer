package com.hippo.ehviewer.dao;

import java.util.List;

public interface BasicDao<T> {
    List<T> list();
    long insert(T t);
}
