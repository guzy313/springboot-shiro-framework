package com.my.shiro.client;

import com.my.shiro.constant.CacheConstant;
import com.my.shiro.constant.SuperConstant;
import com.my.shiro.core.UserBridgeService;
import com.my.shiro.core.base.ShiroUser;
import com.my.shiro.core.base.SimpleMapCache;
import com.my.shiro.core.impl.SimpleCacheServiceImpl;
import com.my.shiro.face.UserAdapterFace;
import com.my.shiro.pojo.ShPermission;
import com.my.shiro.pojo.ShRoles;
import com.my.shiro.pojo.ShUsers;
import com.my.shiro.shiroUtils.SecurityUtils;
import com.my.shiro.utils.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gzy
 * @version 1.0
 * @Description
 */
public class UserBridgeServiceImpl implements UserBridgeService {

    @DubboReference
    private UserAdapterFace userAdapterFace;

    //缓存
    @Autowired
    private SimpleCacheServiceImpl simpleCacheService;


    @Override
    public ShUsers findUserByLoginName(String loginName){
        String cacheKey = CacheConstant.FIND_USER_BY_LOGINNAME + loginName;
        Cache<Object, Object> cache = simpleCacheService.
                getCache(cacheKey);

        ShUsers shUsers = null;

        if(!ObjectUtils.isNullOrEmpty(cache)){
            //存在缓存情况
            shUsers = (ShUsers)cache.get(cacheKey);
        }else{
            //不存在缓存情况
            List<ShUsers> shUsersList = userAdapterFace.findUserByLoginName(loginName);
            if(shUsersList.size() > 0){
                shUsers = shUsersList.get(0);
                //写入缓存
                Map<Object,Object> cacheMap = new HashMap<>();
                cacheMap.put(cacheKey,shUsers);
                SimpleMapCache simpleMapCache = new SimpleMapCache(cacheMap,cacheKey);
                simpleCacheService.createCache(cacheKey,simpleMapCache);
            }
        }
        return shUsers;
    }

    @Override
    public List<String> findRoleNamesByUserId(String userId) {
        List<ShRoles> roles = userAdapterFace.findRolesByUserId(userId);
        List<String> roleNameList = new ArrayList<>();
        for (ShRoles s:roles) {
            roleNameList.add(s.getRoleName());
        }
        return roleNameList;
    }

    @Override
    public List<String> findRoleLabelsByUserId(String key,String userId) {
        List<String> roleLabelList = null;
        Cache<Object, Object> cache = simpleCacheService.getCache(key);
        if(!ObjectUtils.isNullOrEmpty(cache)){
            //存在缓存的情况
            roleLabelList = (List<String>)cache.get(key);
        }else{
            //不存在缓存的情况
            List<ShRoles> roles = userAdapterFace.findRolesByUserId(userId);
            roleLabelList = new ArrayList<>();
            for (ShRoles s:roles) {
                roleLabelList.add(s.getLabel());
            }
            //写入缓存
            Map<Object,Object> cacheMap = new HashMap<>();
            cacheMap.put(key,roleLabelList);
            Cache<Object,Object> simpleCache = new SimpleMapCache(cacheMap,key);
            simpleCacheService.createCache(key,simpleCache);
        }
        return roleLabelList;
    }

    @Override
    public List<String> findPermissionIdsByUserId(String userId) {
        List<ShPermission> shPermissionList = userAdapterFace.findPermissionsByUserId(userId);
        List<String> permissionIds = new ArrayList<>();
        for (ShPermission p:shPermissionList) {
            permissionIds.add(p.getId());
        }
        return permissionIds;
    }

    @Override
    public List<String> findPermissionNamesByUserId(String userId) {
        List<ShPermission> shPermissionList = userAdapterFace.findPermissionsByUserId(userId);
        List<String> permissionNames = new ArrayList<>();
        for (ShPermission p:shPermissionList) {
            permissionNames.add(p.getPermissionName());
        }
        return permissionNames;
    }

    @Override
    public List<ShPermission> findAllPermissions() {
        List<ShPermission> shPermissionList = userAdapterFace.findAllPermissions();
        return shPermissionList;
    }

    @Override
    public List<ShPermission> findPermissionsByUserId(String userName,String userId) {
        List<ShPermission> shPermissionList = null;
        String sessionId = SecurityUtils.getShiroSessionId();
        //此处缓存是根据每次会话不同更新(资源/权限对象缓存)
        String cacheKey = CacheConstant.PERMISSION_KEY + sessionId;
        //获取缓存
        Cache<Object, Object> permissionsCache = simpleCacheService.getCache(cacheKey);
        //判断是否存在缓存
        if(!ObjectUtils.isNullOrEmpty(permissionsCache)){
            //存在缓存情况
            shPermissionList = (List<ShPermission>)permissionsCache.get(cacheKey);
        }else{
            if(SuperConstant.ADMINISTRATOR_NAME.equals(userName)){
                //获取所有权限-管理员
                shPermissionList = userAdapterFace.findAllPermissions();
            }else{
                //查询拥有的权限-普通用户
                shPermissionList = userAdapterFace.findPermissionsByUserId(userId);
            }
            //将权限列表写入缓存
            Map<Object,Object> cacheMap = new HashMap<>();
            cacheMap.put(cacheKey,shPermissionList);
            Cache<Object,Object> cache = new SimpleMapCache(cacheMap,cacheKey);
            simpleCacheService.createCache(cacheKey,cache);
        }
        return shPermissionList;
    }

    @Override
    public List<String> findPermissionLabelsByUserId(String key,String userId) {
        List<String> permissionLabels = null;
        Cache<Object, Object> cache = simpleCacheService.getCache(key);
        if(!ObjectUtils.isNullOrEmpty(cache)){
            //存在缓存情况-从缓存读取
            permissionLabels = (List<String>) cache.get(key);
        }else{
            //不存在缓存情况-从数据库查询-写入缓存
            List<ShPermission> shPermissionList = userAdapterFace.findPermissionsByUserId(userId);
            permissionLabels = new ArrayList<>();
            for (ShPermission p:shPermissionList) {
                permissionLabels.add(p.getLabel());
            }
            //写入缓存
            Map<Object,Object> cacheMap = new HashMap<>();
            cacheMap.put(key,permissionLabels);
            Cache<Object,Object> simpleCache = new SimpleMapCache(cacheMap,key);
            simpleCacheService.createCache(key,simpleCache);
        }
        return permissionLabels;
    }

    @Override
    public AuthorizationInfo getAuthorizationInfo(ShiroUser shiroUser) {
        //根据不同会话ID获取缓存
        String sessionId = SecurityUtils.getShiroSessionId();
        String roleLabelKey = CacheConstant.ROLE_LABEL_KEY + sessionId;
        String permissionLabelKey = CacheConstant.PERMISSION_LABEL_KEY + sessionId;
        //查询 用户 对应的 角色标识 (已做缓存)
        List<String> roleLabels = this.findRoleLabelsByUserId(roleLabelKey,shiroUser.getId());
        //查询 用户 对应的 资源标识 (已做缓存)
        List<String> permissionLabels = this.findPermissionLabelsByUserId(permissionLabelKey,shiroUser.getId());
        //构建 授权信息类
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        authorizationInfo.addRoles(roleLabels);//将角色标签存入 授权信息
        authorizationInfo.addStringPermissions(permissionLabels);//将权限标签 存入 授权信息

        return authorizationInfo;
    }

    @Override
    public void loadUserAuthorityToCache(ShiroUser shiroUser) {
        String sessionId = SecurityUtils.getShiroSessionId();
        String roleLabelKey = CacheConstant.ROLE_LABEL_KEY + sessionId;
        String permissionLabelKey = CacheConstant.PERMISSION_LABEL_KEY + sessionId;
        //通过用户查询对应的角色标识
        List<String> roleLabels =
                this.findRoleLabelsByUserId(roleLabelKey, shiroUser.getId());
        //通过用户查询对应的权限标识
        List<String> permissionLabels =
                this.findPermissionLabelsByUserId(permissionLabelKey, shiroUser.getId());
    }



}
