package com.ssafy.groute.service;

import com.ssafy.groute.dto.PlanShareUser;
import com.ssafy.groute.mapper.PlanShareUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlanShareUserServiceImpl implements PlanShareUserService {
    @Autowired
    PlanShareUserMapper planShareUserMapper;

    @Override
    public void insertPlanShareUser(PlanShareUser planShareUser) throws Exception {
        if(planShareUserMapper.selectByUserIdPlanId(planShareUser)==0) {
            planShareUserMapper.insertPlanShareUser(planShareUser);
        }
    }

    @Override
    public PlanShareUser selectPlanShareUser(int id) throws Exception {
        return planShareUserMapper.selectPlanShareUser(id);
    }

    @Override
    public List<PlanShareUser> selectAllPlanShareUser() throws Exception {
        return planShareUserMapper.selectAllPlanShareUser();
    }

    @Override
    public void deletePlanShareUser(PlanShareUser planShareUser) throws Exception {
        planShareUserMapper.deletePlanShareUserByPlanIdUserId(planShareUser);
    }

    @Override
    public void updatePlanShareUser(PlanShareUser planShareUser) throws Exception {
        planShareUserMapper.updatePlanShareUser(planShareUser);
    }

    @Override
    public List<PlanShareUser> selectByUserId(String userId) throws Exception {
        return planShareUserMapper.selectByUserId(userId);
    }

    @Override
    public List<PlanShareUser> selectByPlanId(int planId) throws Exception {
        return planShareUserMapper.selectByPlanId(planId);
    }
}
