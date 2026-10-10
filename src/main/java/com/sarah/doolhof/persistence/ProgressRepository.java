package com.sarah.doolhof.persistence;

import com.sarah.doolhof.domain.model.Progress;

public interface ProgressRepository {

    Progress load();

    void save(Progress progress);
}
