package com.hippo.ehviewer.dao;

import java.util.List;

public interface BasicDao<T> {
    List<T> fakeList();
    void fakeInsert(T t);
}
